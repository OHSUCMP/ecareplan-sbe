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
import edu.ohsu.cmp.ecareplan.model.dataset.*;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentials;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.ecareplan.service.*;
import edu.ohsu.cmp.ecareplan.transform.GenericResourceTransformer;
import edu.ohsu.cmp.ecareplan.transform.ResourceTransformer;
import edu.ohsu.cmp.ecareplan.util.CryptoUtil;
import edu.ohsu.cmp.ecareplan.util.FhirUtil;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidParameterSpecException;
import java.util.*;
import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class UserWorkspace {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final int POOL_SIZE = 5;

    private final ApplicationContext ctx;
    private final String sessionId;
    private final Audience audience;
    private final Integer socketTimeout;
    private final FHIRCredentials launchCredentials;
    private final Long userId;

    private final Map<Long, UserEndpointCredentials> userEndpointCredentialsMap;
    private final Cache<String, List<? extends BaseDataSetModel<?>>> cache;

    private final ExecutorService executorService;

    private final DataSetBuilderService dataSetBuilderService;
    private final EndpointService endpointService;
    private final AuditService auditService;

    private SecretKey secretKey;

    private Long currentlyLaunchingEndpointId = null;

    protected UserWorkspace(ApplicationContext ctx, String sessionId, Audience audience,
                            FHIRCredentials launchCredentials, Integer socketTimeout) {
        this.ctx = ctx;
        this.sessionId = sessionId;
        this.audience = audience;
        this.launchCredentials = launchCredentials;
        this.socketTimeout = socketTimeout;

        dataSetBuilderService = ctx.getBean(DataSetBuilderService.class);
        endpointService = ctx.getBean(EndpointService.class);
        auditService = ctx.getBean(AuditService.class);

        UserService userService = ctx.getBean(UserService.class);
        User user = userService.getUser(
                launchCredentials.getPatientId()
        );

        userId = user.getId();

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
        addEndpointWithCredentials(getLaunchUserEndpoint(), launchCredentials);

        cache = Caffeine.newBuilder()
                .expireAfterWrite(6, TimeUnit.HOURS)
                .build();

        executorService = Executors.newFixedThreadPool(POOL_SIZE);

        setupAutoShutdownJob();
    }

    private UserEndpoint getLaunchUserEndpoint() {
        Endpoint endpoint;
        if (Audience.PATIENT.equals(audience)) {
            endpoint = endpointService.getPatientLaunchEndpoint();
        } else if (Audience.CARE_TEAM.equals(audience)) {
            endpoint = endpointService.getCareTeamLaunchEndpoint();
        } else {
            throw new CaseNotHandledException("no case for audience: " + audience);
        }

        return endpointService.getUserEndpoint(userId, endpoint.getId());
    }

    private RefreshTokenData getRefreshTokenData(Endpoint endpoint) throws InvalidAlgorithmParameterException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, BadPaddingException, InvalidKeyException {
        return endpointService.getRefreshTokenData(userId, endpoint.getId(), secretKey);
    }

    private void setRefreshTokenData(Endpoint endpoint, RefreshTokenData refreshTokenData) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, InvalidParameterSpecException, BadPaddingException, InvalidKeyException {
        endpointService.setRefreshTokenData(userId, endpoint.getId(), secretKey, refreshTokenData);
    }

    public String getSessionId() {
        return sessionId;
    }

    public Audience getAudience() {
        return audience;
    }

    public FHIRCredentialsWithClient getCredentialsWithClientForEndpoint(Endpoint e) {
        return userEndpointCredentialsMap.containsKey(e.getId()) ?
                userEndpointCredentialsMap.get(e.getId()).getCredentialsWithClient() :
                null;
    }

    public boolean addEndpointWithCredentials(UserEndpoint userEndpoint, FHIRCredentials credentials) {
        if ( ! userEndpointCredentialsMap.containsKey(userEndpoint.getEndpoint().getId()) ) {
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

            return true;

        } else {
            return false;
        }
    }

    public Long getUserId() {
        return userId;
    }

    public void populate() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                logger.info("BEGIN populating workspace for session={}", sessionId);
                for (UserEndpointCredentials uec : userEndpointCredentialsMap.values()) {

                    // todo : eventually, a refresh token should be stored on the UserEndpoint object, and
                    //        this function should use that to automatically obtain a fresh authentication token
                    //        if a valid one isn't present, prior to populating data sets

                    Endpoint e = uec.getUserEndpoint().getEndpoint();
                    long endpointStart = System.currentTimeMillis();
                    logger.info("BEGIN populating for endpoint={} for session={}", e.getName(), sessionId);
                    for (DataSet<?> dataSet : DataSet.ALL_DATASETS) {
                        getCachedDataSetForEndpoint(dataSet, e);
                    }
                    logger.info("DONE populating for endpoint={} for session={} (took {} ms)", e.getName(), sessionId, (System.currentTimeMillis() - endpointStart));
                }
                logger.info("DONE populating workspace for session={} (took {} ms)", sessionId, (System.currentTimeMillis() - start));
            }
        };
        executorService.submit(runnable);
    }

    public void clearCacheAndCredentials() {
        logger.info("clearing cache and credentials for session={}", sessionId);

        cache.invalidateAll();
        cache.cleanUp();

        userEndpointCredentialsMap.clear();
    }

    public void clearCache() {
        logger.info("clearing cache for session={}", sessionId);

        cache.invalidateAll();
        cache.cleanUp();
    }

    public void shutdown() {
        logger.info("shutting down workspace for session={}", sessionId);
        secretKey = null;
        executorService.shutdown();
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

    public void setCurrentlyLaunchingEndpointId(Long endpointId) {
        currentlyLaunchingEndpointId = endpointId;
    }

    public Long getCurrentlyLaunchingEndpointId() {
        return currentlyLaunchingEndpointId;
    }

    public List<EndpointModel> getAllActiveEndpointModels() {
        List<EndpointModel> list = new ArrayList<>();
        for (UserEndpoint ue : getAllActiveUserEndpoints()) {
            list.add(new EndpointModel(ue.getEndpoint()));
        }
        return list;
    }

    private List<UserEndpoint> getAllActiveUserEndpoints() {
        List<UserEndpoint> list = new ArrayList<>();

        Date now = new Date();
        Iterator<UserEndpointCredentials> uecIterator = userEndpointCredentialsMap.values().iterator();
        while (uecIterator.hasNext()) {
            UserEndpointCredentials uec = uecIterator.next();
            if (uec.getExpiresAt().after(now)) {
                list.add(uec.getUserEndpoint());
            } else {
                uecIterator.remove();
            }
        }

        return list;
    }

    private static final class UserEndpointCredentials {
        private UserEndpoint userEndpoint;
        private FHIRCredentialsWithClient credentialsWithClient;
        private Date expiresAt;

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
        for (UserEndpoint ue : getAllActiveUserEndpoints()) {
            List<T> dataSetModels = getCachedDataSetForEndpoint(dataSet, ue.getEndpoint());
            if (dataSetModels != null) {
                list.addAll(dataSetModels);
            }
        }
        return list;
    }


///////////////////////////////////////////////////////////////////////////////////////
/// Data Set Caching Functions

    private String buildCacheKey(DataSet<?> dataSet, Endpoint e) {
        return dataSet.getName() + "-" + e.getIss();  // use iss instead of name.  it's possible that multiple
                                                      // data sets will have different names but point to the same
                                                      // iss.  ultimately, it's the iss we care about, irrespective
                                                      // of what the user sees.  this will help prevent duplicates.
    }

    @SuppressWarnings("unchecked")
    private <T extends BaseDataSetModel<?>> List<T> getCachedDataSetForEndpoint(DataSet<T> dataSet, Endpoint endpoint) {
        return (List<T>) cache.get(buildCacheKey(dataSet, endpoint), s -> {
            long start = System.currentTimeMillis();
            logger.info("BEGIN build {} for session={}, userId={}, endpoint={}", dataSet.getName(), sessionId, userId,
                    endpoint.getName());

            List<? extends BaseDataSetModel<?>> list = null;
            try {
                if (DataSet.PATIENT.equals(dataSet)) {
                    list = dataSetBuilderService.buildPatients(sessionId, endpoint);
                } else if (DataSet.ASSESSMENTS.equals(dataSet)) {
                    list = dataSetBuilderService.buildAssessments(sessionId, endpoint);
                } else if (DataSet.CARE_PLANS.equals(dataSet)) {
                    list = dataSetBuilderService.buildCarePlans(sessionId, endpoint);
                } else if (DataSet.CARE_TEAMS.equals(dataSet)) {
                    list = dataSetBuilderService.buildCareTeams(sessionId, endpoint);
                } else if (DataSet.CLINICAL_NOTES.equals(dataSet)) {
                    list = dataSetBuilderService.buildClinicalNotes(sessionId, endpoint);
                } else if (DataSet.CONDITIONS.equals(dataSet)) {
                    list = dataSetBuilderService.buildConditions(sessionId, endpoint);
                } else if (DataSet.DIAGNOSTIC_REPORTS.equals(dataSet)) {
                    list = dataSetBuilderService.buildDiagnosticReports(sessionId, endpoint);
                } else if (DataSet.ENCOUNTERS.equals(dataSet)) {
                    list = dataSetBuilderService.buildEncounters(sessionId, endpoint);
                } else if (DataSet.GOALS.equals(dataSet)) {
                    list = dataSetBuilderService.buildGoals(sessionId, endpoint);
                } else if (DataSet.IMMUNIZATIONS.equals(dataSet)) {
                    list = dataSetBuilderService.buildImmunizations(sessionId, endpoint);
                } else if (DataSet.LAB_RESULTS.equals(dataSet)) {
                    list = dataSetBuilderService.buildLabResults(sessionId, endpoint);
                } else if (DataSet.MEDICATIONS.equals(dataSet)) {
                    list = dataSetBuilderService.buildMedications(sessionId, endpoint);
                } else if (DataSet.PROCEDURES.equals(dataSet)) {
                    list = dataSetBuilderService.buildProcedures(sessionId, endpoint);
                } else if (DataSet.SERVICE_REQUESTS.equals(dataSet)) {
                    list = dataSetBuilderService.buildServiceRequests(sessionId, endpoint);
                } else if (DataSet.SOCIAL_HISTORIES.equals(dataSet)) {
                    list = dataSetBuilderService.buildSocialHistories(sessionId, endpoint);
                } else if (DataSet.SURVEY_OBSERVATIONS.equals(dataSet)) {
                    list = dataSetBuilderService.buildSurveyObservations(sessionId, endpoint);
                } else if (DataSet.VITALS.equals(dataSet)) {
                    list = dataSetBuilderService.buildVitals(sessionId, endpoint);
                } else {
                    throw new CaseNotHandledException("Case not handled for data set: " + dataSet.getName());
                }

            } catch (Exception e) {
                if (e instanceof ForbiddenOperationException) {
                    logger.warn("attempt to retrieve {} from {} was forbidden - will not include {} for this session",
                            dataSet.getName(), endpoint.getName(), dataSet.getName());
                    auditService.doAudit(sessionId, AuditSeverity.WARN, "cache population", "retrieving " + dataSet.getName() +
                            " from " + endpoint.getName() + " was forbidden");

                    if (DataSet.PATIENT.equals(dataSet)) {
                        logger.error("Patient is required for system operation; aborting -");
                        throw (ForbiddenOperationException) e;
                    }

                } else if (e instanceof InvalidRequestException) {
                    logger.error("attempt to retrieve {} from {} triggered an InvalidRequestException - will not include {} for this session",
                            dataSet.getName(), endpoint.getName(), dataSet.getName());
                    auditService.doAudit(sessionId, AuditSeverity.ERROR, "cache population", "invalid request retrieving " +
                            dataSet.getName() + " from " + endpoint.getName());

                    if (DataSet.PATIENT.equals(dataSet)) {
                        logger.error("Patient is required for system operation; aborting -");
                        throw (InvalidRequestException) e;
                    }

                } else if (e instanceof AuthenticationException ae) {
                    // access token expired
                    // handle gracefully if possible, otherwise abort
                    throw (RuntimeException) e;

                } else if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;

                } else {
                    throw new RuntimeException(e);
                }
            }

            logger.info("DONE building {} for session={}, userId={}, endpoint={} (took {} ms)", dataSet.getName(), sessionId,
                    userId, endpoint.getName(), (System.currentTimeMillis() - start));

            return list;
        });
    }
}
