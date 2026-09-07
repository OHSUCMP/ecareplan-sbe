package edu.ohsu.cmp.ecareplan.workspace;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.impl.JWTParser;
import com.auth0.jwt.interfaces.Payload;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.entity.User;
import edu.ohsu.cmp.ecareplan.entity.UserEndpoint;
import edu.ohsu.cmp.ecareplan.exception.CaseNotHandledException;
import edu.ohsu.cmp.ecareplan.exception.DataException;
import edu.ohsu.cmp.ecareplan.model.Audience;
import edu.ohsu.cmp.ecareplan.model.EndpointModel;
import edu.ohsu.cmp.ecareplan.model.ProgressStatus;
import edu.ohsu.cmp.ecareplan.model.dataset.BaseDataSetModel;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSet;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSetBuilderRequestConfiguration;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentials;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.ecareplan.model.progress.EndpointReadProgressModel;
import edu.ohsu.cmp.ecareplan.model.progress.IProgress;
import edu.ohsu.cmp.ecareplan.service.*;
import edu.ohsu.cmp.ecareplan.task.EndpointPopulationTask;
import edu.ohsu.cmp.ecareplan.util.CryptoUtil;
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class UserWorkspace {
    private static final Logger logger = LoggerFactory.getLogger(UserWorkspace.class);

    private final ApplicationContext ctx;
    private final String sessionId;
    private final Audience audience;
    private final Integer socketTimeout;
    private final FHIRCredentials launchCredentials;
    private final User user;
    private final Map<Long, UserEndpointCredentials> userEndpointCredentialsMap;
    private final Cache<String, List<? extends BaseDataSetModel<?>>> cache;
    private final EndpointService endpointService;
    private final SDSService sdsService;
    private final AuditService auditService;
    private final BackgroundTaskService backgroundTaskService;
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
        backgroundTaskService = ctx.getBean(BackgroundTaskService.class);

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

        Endpoint launcherEndpoint = getLauncherEndpoint();
        UserEndpoint launchUserEndpoint = getOrCreateUserEndpoint(launcherEndpoint, launchCredentials.getPatientId());
        configureUserEndpointCredentials(launchUserEndpoint, launchCredentials);

        endpointReadProgressMap = Collections.synchronizedMap(new LinkedHashMap<>());

        cache = Caffeine.newBuilder()
                .expireAfterWrite(6, TimeUnit.HOURS)
                .build();

        shutdown = new AtomicBoolean(false);
        setupAutoShutdownJob();
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

    public void notifyDataSetUpdated(DataSet<?> dataSet, Endpoint endpoint) {
        sendUpdateNotification("dataset-update", Map.of(
                "dataSet", dataSet.toString(),
                "endpoint", endpoint.getName()
        ));
    }

    public void notifyEndpointPopulationStarted(Endpoint endpoint) {
        sendUpdateNotification("endpoint-population-started", Map.of(
                "endpoint", endpoint.getName()
        ));
    }

    public void notifyEndpointPopulationComplete(Endpoint endpoint) {
        sendUpdateNotification("endpoint-population-complete", Map.of(
                "endpoint", endpoint.getName()
        ));
    }

    public void notifyIfAllComplete() {
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

    public void populate() throws DataException {
        clearAllCompletedProgress();
        for (UserEndpoint ue : endpointService.getAllUserEndpoints(user)) {
            populateEndpoint(ue.getEndpoint());
        }
    }

    public void populateEndpoint(Endpoint endpoint) throws DataException {
        // todo : eventually, a refresh token should be stored on the UserEndpoint object, and
        //        this function should use that to automatically obtain a fresh authentication token
        //        if a valid one isn't present, prior to populating data sets

        final UserEndpoint ue = endpointService.getUserEndpoint(user, endpoint);
        final UserEndpointCredentials uec = getUserEndpointCredentials(endpoint);

        // preliminary sanity check
        if (uec == null && ue.getLastSyncCompleted() == null) {
            logger.warn("Endpoint {} is not configured for OAuth, and has no record of data synced to the SDS.  How did we get here?", endpoint.getName());
            return;
        }

        clearAllCompletedProgress();

        boolean loadFromEndpoint = uec != null;

        EndpointReadProgressModel endpointProgress = new EndpointReadProgressModel(endpoint, ! loadFromEndpoint);
        endpointReadProgressMap.put(endpoint.getId(), endpointProgress);

        FHIRCredentials credentials = loadFromEndpoint ?
                uec.getCredentialsWithClient().getCredentials() :
                launchCredentials;
        String endpointPatientId = getPatientIdForEndpoint(endpoint);
        DataSetBuilderRequestConfiguration cfg = new DataSetBuilderRequestConfiguration(ue, credentials, endpointPatientId);

        EndpointPopulationTask task = new EndpointPopulationTask(sessionId, loadFromEndpoint, cfg,
                launchCredentials, endpointProgress,
                ctx.getBean(UserWorkspaceService.class),
                endpointService, sdsService, backgroundTaskService, auditService);

        Future<Void> endpointFuture = backgroundTaskService.submit(task);
        endpointProgress.setFuture(endpointFuture);
    }

    private String getPatientIdForEndpoint(Endpoint endpoint) throws DataException {
        try {
            UserEndpoint userEndpoint = endpointService.getUserEndpoint(user, endpoint);
            return CryptoUtil.decrypt(userEndpoint.getEncryptedPatientId(), secretKey);

        } catch (Exception e) {
            throw new DataException(e);
        }
    }

    public void shutdown() {
        if ( ! shutdown.compareAndSet(false, true) ) {
            logger.debug("workspace for session={} already shut down", sessionId);
            return;
        }

        shutdownJobs();
        closeEmitterIfPresent();

        sdsService.clearProgressForSession(sessionId);
        secretKey = null;
        endpointReadProgressMap.clear();
        cache.invalidateAll();
        cache.cleanUp();
        userEndpointCredentialsMap.clear();
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
            if (ue.getLastSyncCompleted() != null) {    // could we get resources from the SDS?
                endpoint = ue.getEndpoint();

            } else {                                    // if not, could we get resources directly from the endpoint?
                UserEndpointCredentials uec = getUserEndpointCredentials(ue.getEndpoint());
                if (uec != null) {
                    endpoint = ue.getEndpoint();
                }
            }

            if (endpoint != null) {                     // okay, we *can* get resources for this endpoint, somehow.  get them.
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

    public synchronized void invalidateCache(DataSet<?> dataSet, Endpoint endpoint) {
        cache.invalidate(buildDataSetEndpointKey(dataSet, endpoint));
    }

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

    public synchronized void addToCache(DataSet<?> dataSet, Endpoint endpoint, List<? extends BaseDataSetModel<?>> resources) {
        cache.put(buildDataSetEndpointKey(dataSet, endpoint), resources);
        updateProgress(endpoint, dataSet, ProgressStatus.COMPLETED);
        notifyDataSetUpdated(dataSet, endpoint);
    }
}
