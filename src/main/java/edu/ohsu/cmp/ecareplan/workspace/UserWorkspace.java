package edu.ohsu.cmp.ecareplan.workspace;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.impl.JWTParser;
import com.auth0.jwt.interfaces.Payload;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.entity.User;
import edu.ohsu.cmp.ecareplan.entity.UserEndpoint;
import edu.ohsu.cmp.ecareplan.exception.CaseNotHandledException;
import edu.ohsu.cmp.ecareplan.model.*;
import edu.ohsu.cmp.ecareplan.model.dataset.BaseDataSetModel;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSet;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentials;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.ecareplan.model.progress.EndpointReadProgressModel;
import edu.ohsu.cmp.ecareplan.model.progress.IProgress;
import edu.ohsu.cmp.ecareplan.service.*;
import edu.ohsu.cmp.ecareplan.transform.GenericResourceTransformer;
import edu.ohsu.cmp.ecareplan.transform.ResourceTransformer;
import edu.ohsu.cmp.ecareplan.util.CryptoUtil;
import edu.ohsu.cmp.ecareplan.util.ExecutorUtil;
import edu.ohsu.cmp.ecareplan.util.FhirUtil;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Calendar;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class UserWorkspace {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final int POOL_SIZE = 5;

    private final ApplicationContext ctx;
    private final String sessionId;
    private final Audience audience;
    private final Integer socketTimeout;
    private final FHIRCredentials launchCredentials;
    private final User user;
    private final Map<Long, UserEndpointCredentials> userEndpointCredentialsMap;
    private final Map<Long, String> endpointPatientIdMap;
    private final Cache<String, List<? extends BaseDataSetModel<?>>> cache;
    private final ExecutorService executorService;
    private final EndpointService endpointService;
    private final SDSService sdsService;
    private final AuditService auditService;
    private final Map<Long, EndpointReadProgressModel> endpointReadProgressMap;
    private final AtomicBoolean shutdown;

    private SecretKey secretKey;
    private Endpoint currentlyLaunchingEndpoint = null;

    private volatile SseEmitter emitter = null;

    protected UserWorkspace(ApplicationContext ctx, String sessionId, Audience audience,
                            FHIRCredentials launchCredentials, Integer socketTimeout) {
        this.ctx = ctx;
        this.sessionId = sessionId;
        this.audience = audience;
        this.launchCredentials = launchCredentials;
        this.socketTimeout = socketTimeout;

        endpointService = ctx.getBean(EndpointService.class);
        sdsService = ctx.getBean(SDSService.class);
        auditService = ctx.getBean(AuditService.class);

        UserService userService = ctx.getBean(UserService.class);
        user = userService.getUser(
                launchCredentials.getPatientId()
        );

        // generate a secret key that can be used to encrypt and decrypt sensitive database assets
        // presently, the user's FHIR Patient ID is used as the password that undergirds this key, which admittedly
        // isn't the best, but it isn't anywhere else in the database, and it's certainly not something anyone
        // will be able to easily guess.  we want something that's just a part of the access token so the user
        // doesn't need to set and manage a separate password.  I think this is probably secure enough?
        try {
            secretKey = CryptoUtil.generateSecretKey(
                    launchCredentials.getPatientId().toCharArray(),
                    Base64.getDecoder().decode(user.getSaltB64())
            );
        } catch (Exception e) {
            logger.error("caught {} generating secret key for session {} - {}", e.getClass().getSimpleName(), sessionId, e.getMessage());
        }

        userEndpointCredentialsMap = new LinkedHashMap<>();
        endpointPatientIdMap = new LinkedHashMap<>();

        Endpoint launcherEndpoint = getLauncherEndpoint();
        UserEndpoint launchUserEndpoint = getOrCreateUserEndpoint(launcherEndpoint, launchCredentials.getPatientId());
        configureUserEndpointCredentials(launchUserEndpoint, launchCredentials);

        endpointReadProgressMap = Collections.synchronizedMap(new LinkedHashMap<>());

        cache = Caffeine.newBuilder()
                .expireAfterWrite(6, TimeUnit.HOURS)
                .build();

        executorService = Executors.newFixedThreadPool(POOL_SIZE);

        shutdown = new AtomicBoolean(false);
        setupAutoShutdownJob();
    }

    public String getPatientIdForEndpoint(Endpoint endpoint) {
        if ( ! endpointPatientIdMap.containsKey(endpoint.getId()) ) {
            try {
                UserEndpoint userEndpoint = endpointService.getUserEndpoint(user, endpoint);
                endpointPatientIdMap.put(endpoint.getId(), CryptoUtil.decrypt(userEndpoint.getEncryptedPatientId(), secretKey));
            } catch (Exception e) {
                if (e instanceof RuntimeException re) {
                    throw re;
                } else {
                    throw new RuntimeException(e);
                }
            }
        }
        return endpointPatientIdMap.get(endpoint.getId());
    }

    public UserEndpoint getOrCreateUserEndpoint(Endpoint endpoint, String fhirPatientId) {
        UserEndpoint userEndpoint;
        try {
            userEndpoint = endpointService.getUserEndpoint(user, endpoint);
        } catch (NoSuchElementException e) {
            logger.warn("caught {} getting launch user endpoint for session {} - {}", e.getClass().getSimpleName(), sessionId, e.getMessage());
            try {
                userEndpoint = endpointService.createUserEndpoint(user, endpoint, fhirPatientId, null, secretKey);
            } catch (Exception e1) {
                logger.error("caught {} creating launch user endpoint for session {} - {}", e1.getClass().getSimpleName(), sessionId, e1.getMessage());
                if (e1 instanceof RuntimeException re) {
                    throw re;
                } else {
                    throw new RuntimeException(e1);
                }
            }
        }
        return userEndpoint;
    }

    private Endpoint getLauncherEndpoint() {
        if (Audience.PATIENT.equals(audience)) {
            return endpointService.getPatientLaunchEndpoint();
        } else if (Audience.CARE_TEAM.equals(audience)) {
            return endpointService.getCareTeamLaunchEndpoint();
        } else {
            throw new CaseNotHandledException("no case for audience: " + audience);
        }
    }

    public synchronized List<IProgress> getCurrentProgress() {
        List<IProgress> list = new ArrayList<>(endpointReadProgressMap.values());
        List<IProgress> sdsList = sdsService.getCurrentProgress(sessionId);
        if (sdsList != null) {
            list.addAll(sdsList);
        }
        return list;
    }

    public synchronized List<IProgress> getCurrentProgress(DataSet<?> dataSet) {
        List<IProgress> list = new ArrayList<>();
        for (EndpointReadProgressModel model : endpointReadProgressMap.values()) {
            list.add(model.getDataSetReadProgressModel(dataSet));
        }
        List<IProgress> sdsList = sdsService.getCurrentProgress(sessionId, dataSet);
        if (sdsList != null) {
            list.addAll(sdsList);
        }
        return list;
    }

    private synchronized void updateProgress(Endpoint endpoint, DataSet<?> dataSet, ProgressStatus status) {
        if (endpointReadProgressMap.containsKey(endpoint.getId())) {
            endpointReadProgressMap.get(endpoint.getId()).setStatus(dataSet, status);
        }
    }

    private synchronized void addProgressError(Endpoint endpoint, DataSet<?> dataSet, String error) {
        if (endpointReadProgressMap.containsKey(endpoint.getId())) {
            endpointReadProgressMap.get(endpoint.getId()).addError(dataSet, error);
        }
    }

    private synchronized void clearAllCompletedProgress() {
        endpointReadProgressMap.values().removeIf(pm -> pm.getFuture() == null || pm.getFuture().isDone());
        sdsService.clearAllCompletedProgress(sessionId);
    }

    private void waitUntilAllProgressComplete() {
        endpointReadProgressMap.values().forEach(pm -> {
            try {
                if (pm.getFuture() != null) {
                    pm.getFuture().get();
                }
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Error waiting for future to complete", e);
            }
        });
        sdsService.waitUntilAllProgressComplete(sessionId);
    }

    private synchronized void terminateRemainingProgress() {
        endpointReadProgressMap.values().forEach(pm -> {
            if (pm.getFuture() != null) {
                pm.getFuture().cancel(true);
            }
        });
        sdsService.terminateRemainingProgress(sessionId);
    }

    public synchronized SseEmitter createNewEmitter() {
        if (shutdown.get()) {
            SseEmitter closedEmitter = new SseEmitter(0L);
            closedEmitter.complete();
            return closedEmitter;
        }

        closeEmitterIfPresent();

        SseEmitter newEmitter = new SseEmitter(30 * 60 * 1000L); // 30 minutes

        newEmitter.onCompletion(() -> clearEmitter(newEmitter));
        newEmitter.onTimeout(() -> clearEmitter(newEmitter));
        newEmitter.onError((ex) -> clearEmitter(newEmitter));

        logger.debug("Created new emitter {} for session {}", newEmitter, sessionId);

        this.emitter = newEmitter;
        return this.emitter;
    }

    private synchronized void clearEmitter(SseEmitter emitterToRemove) {
        // this function needs to take emitterToRemove as a parameter to ensure that an old emitter's event
        // doesn't inadvertently affect the current emitter, if they're different
        if (this.emitter == emitterToRemove) {
            this.emitter = null;
        }
    }

    private synchronized void closeEmitterIfPresent() {
        SseEmitter currentEmitter = this.emitter;
        this.emitter = null;

        if (currentEmitter != null) {
            try {
                currentEmitter.complete();
            } catch (Exception e) {
                logger.debug("caught {} completing SSE emitter for session {} - {}", e.getClass().getSimpleName(), sessionId,
                        e.getMessage(), e);
            }
        }
    }

    private void sendUpdateNotification(String eventName, Map<String, String> payload) {
        SseEmitter currentEmitter = this.emitter;
        if (currentEmitter != null) {
            try {
                currentEmitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(payload, MediaType.APPLICATION_JSON)
                );

            } catch (Exception e) {
                logger.debug("caught {} attempting to send {} - {}", e.getClass().getSimpleName(), "dataset-update", e.getMessage(), e);
                clearEmitter(currentEmitter);
                try {
                    currentEmitter.completeWithError(e);
                } catch (Exception ignored) {
                    // emitter may already be completed/closed
                }
            }
        }
    }

    private void notifyDataSetUpdated(DataSet<?> dataSet, Endpoint endpoint) {
        sendUpdateNotification("dataset-update", Map.of(
                "dataSet", dataSet.toString(),
                "endpoint", endpoint.getName()
        ));
    }

    private void notifyEndpointPopulationStarted(Endpoint endpoint) {
        sendUpdateNotification("endpoint-population-started", Map.of(
                "endpoint", endpoint.getName()
        ));
    }

    private void notifyEndpointPopulationComplete(Endpoint endpoint) {
        sendUpdateNotification("endpoint-population-complete", Map.of(
                "endpoint", endpoint.getName()
        ));
    }

    private void notifyIfAllComplete() {
        List<IProgress> progress = getCurrentProgress();
        if (progress.stream().allMatch(p -> p.getStatus().equals(ProgressStatus.COMPLETED))) {
            sendUpdateNotification("all-complete", Map.of());
        }
    }

    public String getSessionId() {
        return sessionId;
    }

    public Audience getAudience() {
        return audience;
    }

    public FHIRCredentialsWithClient getCredentialsWithClientForEndpoint(Endpoint endpoint) {
        UserEndpointCredentials uec = getUserEndpointCredentials(endpoint);
        return uec != null ?
                uec.getCredentialsWithClient() :
                null;
    }

    public void configureUserEndpointCredentials(UserEndpoint userEndpoint, FHIRCredentials credentials) {
        IGenericClient client = FhirUtil.buildClient(
                credentials.getServerURL(),
                credentials.getBearerToken(),
                socketTimeout
        );
        FHIRCredentialsWithClient fcc = new FHIRCredentialsWithClient(credentials, client);

        Date expiresAt;
        try {
            expiresAt = parseExpiresAt(credentials.getBearerToken());

        } catch (Exception e) {
            logger.warn("couldn't parse token for session={} - will auto-expire token in 1 hour", sessionId);
            logger.debug("caught {} parsing bearer token for session={} - {}", e.getClass().getName(), sessionId, e.getMessage(), e);

            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            cal.add(Calendar.HOUR_OF_DAY, 1);
            expiresAt = cal.getTime();
        }

        userEndpointCredentialsMap.put(userEndpoint.getEndpoint().getId(), new UserEndpointCredentials(userEndpoint, fcc, expiresAt));
    }

    public Long getUserId() {
        return user.getId();
    }

    public User getUser() {
        return user;
    }

    public void populate() {
        clearAllCompletedProgress();
        for (UserEndpoint ue : endpointService.getAllUserEndpoints(user)) {
            populateEndpoint(ue.getEndpoint());
        }
    }

    public void populateEndpoint(Endpoint endpoint) {
        // todo : eventually, a refresh token should be stored on the UserEndpoint object, and
        //        this function should use that to automatically obtain a fresh authentication token
        //        if a valid one isn't present, prior to populating data sets

        // preliminary sanity check
        UserEndpoint ue = endpointService.getUserEndpoint(user, endpoint);
        UserEndpointCredentials uec = getUserEndpointCredentials(endpoint);
        if (uec == null && ue.getLastSyncCompleted() == null) {
            logger.warn("Endpoint {} is not configured for OAuth, and has no record of data synced to the SDS.  How did we get here?", endpoint.getName());
            return;
        }
        boolean loadFromEndpoint = uec != null;

        clearAllCompletedProgress();

        Callable<Void> callable = new Callable<>() {
            @Override
            public Void call() {
                long start = System.currentTimeMillis();
                logger.info("BEGIN populating for endpoint={} for session={}", endpoint.getName(), sessionId);
                notifyEndpointPopulationStarted(endpoint);
                List<Future<Void>> futures = new ArrayList<>();
                try {
                    for (DataSet<?> dataSet : DataSet.ALL_DATASETS_BY_PRIORITY) {
                        try {
                            updateProgress(endpoint, dataSet, ProgressStatus.RUNNING);
                            cache.invalidate(buildDataSetEndpointKey(dataSet, endpoint));

                            if (loadFromEndpoint) {
                                getDataSetModelsForEndpoint(dataSet, endpoint, endpointService);
                                Future<Void> future = sdsService.shareToSDS(sessionId, dataSet, endpoint);
                                if (future != null) {
                                    futures.add(future);
                                }
                            } else {
                                getDataSetModelsForEndpoint(dataSet, endpoint, sdsService);
                            }

                        } catch (Exception e) {
                            final String endpointNameForLogging = ! loadFromEndpoint ?
                                    "SDS for " + endpoint.getName() :
                                    endpoint.getName();

                            logger.error("caught {} populating {} from {} for session={} - {}", e.getClass().getSimpleName(), dataSet.getName(),
                                    endpointNameForLogging, sessionId, e.getMessage(), e);
                            auditService.doAudit(user, AuditSeverity.ERROR, "endpoint population",
                                    "caught " + e.getClass().getSimpleName() + " populating " + dataSet.getName() + " from " +
                                    endpointNameForLogging + " - " + e.getMessage());
                            addProgressError(endpoint, dataSet, e.getMessage());

                            if (e instanceof ForbiddenOperationException && ! loadFromEndpoint) {
                                // user can't access their SDS records that the app seems to think they have
                                // maybe the SDS was reset?
                                // in any case, it probably makes sense to just clear their lastSyncCompleted timestamp and abort this attempt
                                endpointService.clearUserEndpointLastSyncCompleted(ue);
                                auditService.doAudit(user, AuditSeverity.WARN, "endpoint population",
                                        "cleared SDS lastSyncCompleted timestamp and aborting population for " + endpoint.getName());
                                break;
                            }

                        } finally {
                            updateProgress(endpoint, dataSet, ProgressStatus.COMPLETED);
                            notifyDataSetUpdated(dataSet, endpoint);
                        }
                    }

                } finally {
                    if (loadFromEndpoint) {
                        try {
                            for (Future<Void> future : futures) {
                                future.get(); // waits until this task completes
                            }
                            logger.info("Successfully shared all data from {} to SDS", endpoint.getName());
                            UserEndpoint userEndpoint = endpointService.getUserEndpoint(user, endpoint);
                            endpointService.updateUserEndpointLastSyncCompleted(userEndpoint);

                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IllegalStateException("Interrupted while sharing data to SDS", e);

                        } catch (ExecutionException | CancellationException e) {
                            logger.error("Failed while sharing data to SDS", e);
                        }
                    }
                }

                long runtime = System.currentTimeMillis() - start;
                logger.info("DONE populating for endpoint={} for session={} (took {} ms)", endpoint.getName(), sessionId, runtime);

                notifyEndpointPopulationComplete(endpoint);
                notifyIfAllComplete();

                return null;
            }
        };

        EndpointReadProgressModel progressModel = new EndpointReadProgressModel(endpoint, ! loadFromEndpoint);
        endpointReadProgressMap.put(endpoint.getId(), progressModel);
        Future<Void> future = executorService.submit(callable);
        progressModel.setFuture(future);
        logger.info("Submitted callable for endpoint={} for session {}", endpoint.getIss(), sessionId);
    }

    public void clearCacheAndCredentials() {
        logger.info("clearing cache and credentials for session={}", sessionId);

        cache.invalidateAll();
        cache.cleanUp();

        userEndpointCredentialsMap.clear();
        endpointPatientIdMap.clear();
    }

    public void shutdown(boolean force) {
        if ( ! shutdown.compareAndSet(false, true) ) {
            logger.debug("workspace for session={} already shut down", sessionId);
            return;
        }
        closeEmitterIfPresent();

        if (force) {
            terminateRemainingProgress();
        } else {
            waitUntilAllProgressComplete();
        }

        sdsService.shutdown(sessionId);
        ExecutorUtil.shutdownAndAwaitTermination(executorService, 10);
        secretKey = null;
        endpointReadProgressMap.clear();
        clearCacheAndCredentials();
        shutdownJobs();
    }

    private void shutdownJobs() {
        logger.info("clearing triggers and jobs for session={}", sessionId);
        Scheduler scheduler = ctx.getBean(Scheduler.class);
        try {
            for (TriggerKey triggerKey : scheduler.getTriggerKeys(GroupMatcher.groupEquals(sessionId))) {
                logger.debug("unscheduling trigger: {}", triggerKey.getName());
                scheduler.unscheduleJob(triggerKey);
            }

            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.groupEquals(sessionId))) {
                logger.debug("deleting job: {}", jobKey.getName());
                scheduler.deleteJob(jobKey);
            }

        } catch (SchedulerException e) {
            logger.error("caught {} shutting down jobs for session {} - {}", e.getClass().getName(), sessionId, e.getMessage(), e);
        }
    }

    private void setupAutoShutdownJob() {
        Scheduler scheduler = ctx.getBean(Scheduler.class);

        Date shutdownTimestamp;
        try {
            shutdownTimestamp = parseExpiresAt(launchCredentials.getBearerToken());

        } catch (JWTDecodeException e) {
            logger.warn("couldn't parse token for session={} - will auto-shutdown workspace after 1 day", sessionId);
            logger.debug("caught {} parsing bearer token for session={} - {}", e.getClass().getName(), sessionId, e.getMessage(), e);

            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            cal.add(Calendar.DATE, 1);
            shutdownTimestamp = cal.getTime();
        }

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(ShutdownWorkspaceJob.JOBDATA_APPLICATIONCONTEXT, ctx);
        jobDataMap.put(ShutdownWorkspaceJob.JOBDATA_SESSIONID, sessionId);

        JobKey jobKey = new JobKey("shutdownWorkspaceJob-" + sessionId, sessionId);
        JobDetail job = JobBuilder.newJob(ShutdownWorkspaceJob.class)
                .storeDurably()
                .withIdentity(jobKey)
                .withDescription("Auto-shutdown User Workspace for session " + sessionId + " at " + shutdownTimestamp)
                .usingJobData(jobDataMap)
                .build();

        JobDetailFactoryBean jobDetailFactory = new JobDetailFactoryBean();
        jobDetailFactory.setJobClass(ShutdownWorkspaceJob.class);
        jobDetailFactory.setDescription("Invoke Shutdown User Workspace Job service...");
        jobDetailFactory.setDurability(true);

        Trigger trigger = TriggerBuilder.newTrigger().forJob(job)
                .withIdentity("shutdownWorkspaceTrigger-" + sessionId, sessionId)
                .withDescription("Shutdown Workspace trigger")
                .startAt(shutdownTimestamp)
                .build();

        try {
            if ( ! scheduler.isStarted() ) {
                scheduler.start();
            }

            if (scheduler.checkExists(jobKey)) {
                JobDetail jobDetail = scheduler.getJobDetail(jobKey);
                logger.warn("found pre-existing auto-shutdown job for session {}, but this should have been cleared earlier, it shouldn't have gotten this far.  ???", sessionId);
                logger.info("deleting job: {}", jobDetail.getDescription());
                scheduler.deleteJob(jobKey);
            }

            logger.info("scheduling job: {}", job.getDescription());
            scheduler.scheduleJob(job, trigger);

        } catch (SchedulerException e) {
            throw new RuntimeException(e);
        }
    }

    private Date parseExpiresAt(String bearerToken) throws JWTDecodeException {
        String[] parts = bearerToken.split("\\.");
        String payloadJSON = new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        JWTParser parser = new JWTParser();
        Payload payload = parser.parsePayload(payloadJSON);
        return payload.getExpiresAt();
    }

    public ResourceTransformer getResourceTransformer(EndpointProviderType endpointProviderType) {
        // todo : this needs to return an appropriate transformer based on the endpoint provider type
        //        for now, just return GenericResourceTransformer

        ResourceCategorizationService rcs = ctx.getBean(ResourceCategorizationService.class);

        return new GenericResourceTransformer(rcs);
    }

    public void setCurrentlyLaunchingEndpoint(Endpoint endpoint) {
        currentlyLaunchingEndpoint = endpoint;
    }

    public Endpoint getCurrentlyLaunchingEndpoint() {
        return currentlyLaunchingEndpoint;
    }

    public List<EndpointModel> getAllActiveEndpointModels() {
        List<EndpointModel> list = new ArrayList<>();

        Date now = new Date();
        Iterator<UserEndpointCredentials> uecIterator = userEndpointCredentialsMap.values().iterator();
        while (uecIterator.hasNext()) {
            UserEndpointCredentials uec = uecIterator.next();
            if (uec.getExpiresAt().after(now)) {
                list.add(new EndpointModel(uec.getUserEndpoint().getEndpoint()));
            } else {
                uecIterator.remove();
            }
        }

        return list;
    }

    private UserEndpointCredentials getUserEndpointCredentials(Endpoint endpoint) {
        if (userEndpointCredentialsMap.containsKey(endpoint.getId())) {
            UserEndpointCredentials uec = userEndpointCredentialsMap.get(endpoint.getId());
            if (uec.getExpiresAt().after(new Date())) {
                return uec;
            } else {
                userEndpointCredentialsMap.remove(endpoint.getId());
            }
        }
        return null;
    }

    private static final class UserEndpointCredentials {
        private final UserEndpoint userEndpoint;
        private final FHIRCredentialsWithClient credentialsWithClient;
        private final Date expiresAt;

        public UserEndpointCredentials(UserEndpoint userEndpoint, FHIRCredentialsWithClient credentialsWithClient, Date expiresAt) {
            this.userEndpoint = userEndpoint;
            this.credentialsWithClient = credentialsWithClient;
            this.expiresAt = expiresAt;
        }

        public UserEndpoint getUserEndpoint() {
            return userEndpoint;
        }

        public FHIRCredentialsWithClient getCredentialsWithClient() {
            return credentialsWithClient;
        }

        public Date getExpiresAt() {
            return expiresAt;
        }
    }

    public <T extends BaseDataSetModel<?>> List<T> getAllDataSetModels(DataSet<T> dataSet) {
        List<T> list = new ArrayList<>();
        for (UserEndpoint ue : endpointService.getAllUserEndpoints(user)) {
            Endpoint endpoint = null;
            if (ue.getLastSyncCompleted() != null) {
                endpoint = ue.getEndpoint();
            } else {
                UserEndpointCredentials uec = getUserEndpointCredentials(ue.getEndpoint());
                if (uec != null) {
                    endpoint = ue.getEndpoint();
                }
            }
            if (endpoint != null) {
                List<T> dataSetModels = getCachedDataSetModelsForEndpoint(dataSet, endpoint);
                if (dataSetModels != null) {
                    list.addAll(dataSetModels);
                }
            }
        }
        return list;
    }


///////////////////////////////////////////////////////////////////////////////////////
/// Data Set Caching Functions

    private String buildDataSetEndpointKey(DataSet<?> dataSet, Endpoint endpoint) {
        return dataSet.getName() + "|" + endpoint.getIss();  // use iss instead of name.  it's possible that multiple
                                                             // data sets will have different names but point to the same
                                                             // iss.  ultimately, it's the iss we care about, irrespective
                                                             // of what the user sees.  this will help prevent duplicates.
    }

    @SuppressWarnings("unchecked")
    public <T extends BaseDataSetModel<?>> List<T> getCachedDataSetModelsForEndpoint(DataSet<T> dataSet, Endpoint endpoint) {
        List<T> list = (List<T>) cache.getIfPresent(buildDataSetEndpointKey(dataSet, endpoint));
        return list != null ?
                list :
                new ArrayList<>();
    }


    @SuppressWarnings("unchecked")
    public <T extends BaseDataSetModel<?>> List<T> getDataSetModelsForEndpoint(DataSet<T> dataSet, Endpoint endpoint, IDataSetBuilder dataSetBuilder) {
        return (List<T>) cache.get(buildDataSetEndpointKey(dataSet, endpoint), s -> {
            long start = System.currentTimeMillis();
            logger.info("BEGIN build {} for session={}, userId={}, endpoint={}", dataSet.getName(), sessionId, user.getId(),
                    endpoint.getName());

            List<? extends BaseDataSetModel<?>> list = null;
            try {
                if (DataSet.PATIENT.equals(dataSet)) {
                    list = dataSetBuilder.buildPatients(sessionId, endpoint);
                } else if (DataSet.CARE_PLANS.equals(dataSet)) {
                    list = dataSetBuilder.buildCarePlans(sessionId, endpoint);
                } else if (DataSet.CARE_TEAMS.equals(dataSet)) {
                    list = dataSetBuilder.buildCareTeams(sessionId, endpoint);
                } else if (DataSet.CLINICAL_NOTES.equals(dataSet)) {
                    list = dataSetBuilder.buildClinicalNotes(sessionId, endpoint);
                } else if (DataSet.CONDITIONS.equals(dataSet)) {
                    list = dataSetBuilder.buildConditions(sessionId, endpoint);
                } else if (DataSet.DIAGNOSTIC_REPORTS.equals(dataSet)) {
                    list = dataSetBuilder.buildDiagnosticReports(sessionId, endpoint);
                } else if (DataSet.ENCOUNTERS.equals(dataSet)) {
                    list = dataSetBuilder.buildEncounters(sessionId, endpoint);
                } else if (DataSet.GOALS.equals(dataSet)) {
                    list = dataSetBuilder.buildGoals(sessionId, endpoint);
                } else if (DataSet.IMMUNIZATIONS.equals(dataSet)) {
                    list = dataSetBuilder.buildImmunizations(sessionId, endpoint);
                } else if (DataSet.LAB_RESULTS.equals(dataSet)) {
                    list = dataSetBuilder.buildLabResults(sessionId, endpoint);
                } else if (DataSet.MEDICATIONS.equals(dataSet)) {
                    list = dataSetBuilder.buildMedications(sessionId, endpoint);
                } else if (DataSet.PROCEDURES.equals(dataSet)) {
                    list = dataSetBuilder.buildProcedures(sessionId, endpoint);
                } else if (DataSet.QUESTIONNAIRE_RESPONSES.equals(dataSet)) {
                    list = dataSetBuilder.buildQuestionnaireResponses(sessionId, endpoint);
                } else if (DataSet.SERVICE_REQUESTS.equals(dataSet)) {
                    list = dataSetBuilder.buildServiceRequests(sessionId, endpoint);
                } else if (DataSet.SOCIAL_HISTORIES.equals(dataSet)) {
                    list = dataSetBuilder.buildSocialHistories(sessionId, endpoint);
                } else if (DataSet.SURVEY_OBSERVATIONS.equals(dataSet)) {
                    list = dataSetBuilder.buildSurveyObservations(sessionId, endpoint);
                } else if (DataSet.VITALS.equals(dataSet)) {
                    list = dataSetBuilder.buildVitals(sessionId, endpoint);
                } else {
                    throw new CaseNotHandledException("Case not handled for data set: " + dataSet.getName());
                }

                if (dataSetBuilder instanceof EndpointService) {
                    auditService.doAudit(user, AuditSeverity.INFO, "cache population", "got " + list.size() + " resource(s) for dataSet=" + dataSet.getName() +
                            " from " + endpoint.getName() + " (took " + (System.currentTimeMillis() - start) + "ms)");
                }

            } catch (Exception e) {
                final String endpointNameForLogging = dataSetBuilder instanceof SDSService ?
                        "SDS for " + endpoint.getName() :
                        endpoint.getName();

                if (e instanceof ForbiddenOperationException foe) {
                    logger.error("attempt to retrieve {} from {} was forbidden - {}",
                            dataSet.getName(), endpointNameForLogging, foe.getMessage());

                    if (DataSet.PATIENT.equals(dataSet)) {
                        logger.error("Patient is required for system operation; aborting -");
                        throw foe;

                    } else {
                        auditService.doAudit(user, AuditSeverity.ERROR, "cache population", "retrieving " + dataSet.getName() +
                                " from " + endpointNameForLogging + " was forbidden");
                        addProgressError(endpoint, dataSet, foe.getMessage());
                    }

                } else if (e instanceof InvalidRequestException ire) {
                    logger.error("attempt to retrieve {} from {} triggered an InvalidRequestException - {}",
                            dataSet.getName(), endpointNameForLogging, ire.getMessage());

                    if (DataSet.PATIENT.equals(dataSet)) {
                        logger.error("Patient is required for system operation; aborting -");
                        throw ire;

                    } else {
                        auditService.doAudit(user, AuditSeverity.ERROR, "cache population", "invalid request retrieving " +
                                dataSet.getName() + " from " + endpointNameForLogging);
                        addProgressError(endpoint, dataSet, e.getMessage());
                    }

                } else if (e instanceof AuthenticationException ae) {
                    // access token expired
                    // handle gracefully if possible, otherwise abort
                    throw ae;

                } else if (e instanceof RuntimeException re) {
                    throw re;

                } else {
                    throw new RuntimeException(e);
                }
            }

            logger.info("DONE building {} for session={}, userId={}, endpoint={} (took {} ms)", dataSet.getName(), sessionId,
                    user.getId(), endpoint.getName(), (System.currentTimeMillis() - start));

            return list != null ?
                    list :
                    List.of();
        });
    }
}
