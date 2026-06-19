package edu.ohsu.cmp.ecareplan.workspace;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import com.auth0.jwt.impl.JWTParser;
import com.auth0.jwt.interfaces.Payload;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.entity.User;
import edu.ohsu.cmp.ecareplan.entity.UserEndpoint;
import edu.ohsu.cmp.ecareplan.exception.CaseNotHandledException;
import edu.ohsu.cmp.ecareplan.model.Audience;
import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import edu.ohsu.cmp.ecareplan.model.EndpointProviderType;
import edu.ohsu.cmp.ecareplan.model.dataset.*;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentials;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.ecareplan.service.*;
import edu.ohsu.cmp.ecareplan.transform.GenericResourceTransformer;
import edu.ohsu.cmp.ecareplan.transform.ResourceTransformer;
import edu.ohsu.cmp.ecareplan.util.FhirUtil;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;

import java.nio.charset.StandardCharsets;
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
    private final FHIRCredentialsWithClient launchCredentialsWithClient;
    private final Long userId;

    private final List<UserEndpointCredentials> userEndpointCredentialsList;
    private final Cache<String, Object> cache;

    private final ExecutorService executorService;

    private final DataSetBuilderService dataSetBuilderService;
    private final AuditService auditService;


    protected UserWorkspace(ApplicationContext ctx, String sessionId, Audience audience,
                            FHIRCredentialsWithClient launchCredentialsWithClient, Integer socketTimeout) {
        this.ctx = ctx;
        this.sessionId = sessionId;
        this.audience = audience;
        this.launchCredentialsWithClient = launchCredentialsWithClient;
        this.socketTimeout = socketTimeout;

        this.dataSetBuilderService = ctx.getBean(DataSetBuilderService.class);
        this.auditService = ctx.getBean(AuditService.class);

        UserService userService = ctx.getBean(UserService.class);
        User user = userService.getUser(
                launchCredentialsWithClient.getCredentials().getPatientId()
        );

        this.userId = user.getId();

        userEndpointCredentialsList = new ArrayList<>();
        UserEndpoint ue = getOrCreateUserEndpointIfMissing();
        UserEndpointCredentials uec = new UserEndpointCredentials(ue, launchCredentialsWithClient);
        userEndpointCredentialsList.add(uec);

        cache = Caffeine.newBuilder()
                .expireAfterWrite(6, TimeUnit.HOURS)
                .build();

        executorService = Executors.newFixedThreadPool(POOL_SIZE);

        setupAutoShutdownJob();
    }

    private UserEndpoint getOrCreateUserEndpointIfMissing() {
        EndpointService endpointService = ctx.getBean(EndpointService.class);

        Endpoint endpoint;
        if (Audience.PATIENT.equals(audience)) {
            endpoint = endpointService.getPatientLaunchEndpoint();
        } else if (Audience.CARE_TEAM.equals(audience)) {
            endpoint = endpointService.getCareTeamLaunchEndpoint();
        } else {
            throw new CaseNotHandledException("no case for audience: " + audience);
        }

        UserEndpoint ue;
        try {
            ue = endpointService.getUserEndpoint(userId, endpoint.getId());

        } catch (NoSuchElementException nsee) {
            ue = endpointService.createUserEndpoint(userId,
                    launchCredentialsWithClient.getCredentials().getPatientId(),
                    launchCredentialsWithClient.getCredentials().getUserId(),
                    endpoint);
        }

        return ue;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Audience getAudience() {
        return audience;
    }

    public FHIRCredentialsWithClient getCredentialsWithClientForEndpoint(Endpoint e) {
        for (UserEndpointCredentials uec : userEndpointCredentialsList) {
            if (uec.getUserEndpoint().getEndpoint().getId().equals(e.getId())) {
                return uec.getCredentialsWithClient();
            }
        }
        return null;
    }

    public boolean addEndpointWithCredentials(Endpoint endpoint, FHIRCredentials credentials) {
        EndpointService endpointService = ctx.getBean(EndpointService.class);
        UserEndpoint ue = endpointService.getUserEndpoint(userId, endpoint.getId());
        FHIRCredentialsWithClient fcc = getCredentialsWithClientForEndpoint(ue.getEndpoint());
        if (fcc == null) {
            IGenericClient client = FhirUtil.buildClient(
                    credentials.getServerURL(),
                    credentials.getBearerToken(),
                    socketTimeout
            );
            fcc = new FHIRCredentialsWithClient(credentials, client);
            userEndpointCredentialsList.add(new UserEndpointCredentials(ue, fcc));
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
                for (UserEndpointCredentials uec : userEndpointCredentialsList) {

                    // todo : eventually, a refresh token should be stored on the UserEndpoint object, and
                    //        this function should use that to automatically obtain a fresh authentication token
                    //        if a valid one isn't present, prior to populating data sets

                    Endpoint e = uec.getUserEndpoint().getEndpoint();
                    long endpointStart = System.currentTimeMillis();
                    logger.info("BEGIN populating for endpoint={} for session={}", e.getName(), sessionId);
                    getPatient(e);
                    getAssessments(e);
                    getCarePlans(e);
                    getCareTeams(e);
                    getClinicalNotes(e);
                    getConditions(e);
                    getDiagnosticReports(e);
                    getGoals(e);
                    getImmunizations(e);
                    getInteractions(e);
                    getMedications(e);
                    getProcedures(e);
                    getServiceRequests(e);
                    getSocialHistories(e);
                    getSurveyObservations(e);
                    getTests(e);
                    getVitals(e);
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

        userEndpointCredentialsList.clear();
    }

    public void clearCache() {
        logger.info("clearing cache for session={}", sessionId);

        cache.invalidateAll();
        cache.cleanUp();
    }

    public void shutdown() {
        logger.info("shutting down workspace for session={}", sessionId);
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
        Date shutdownTimestamp = deriveWorkspaceAutoShutdownExpirationFromToken(launchCredentialsWithClient.getCredentials().getBearerToken());

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

    private Date deriveWorkspaceAutoShutdownExpirationFromToken(String bearerToken) {
        try {
            return parseExpiresAt(bearerToken);

        } catch (Exception e) {
            logger.warn("couldn't parse token for session={} - will auto-shutdown workspace after 1 day", sessionId);
            logger.debug("caught {} parsing bearer token for session={} - {}", e.getClass().getName(), sessionId, e.getMessage(), e);

            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            cal.add(Calendar.DATE, 1);
            return cal.getTime();
        }
    }

    private Date parseExpiresAt(String bearerToken) {
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

    private List<UserEndpoint> getAllActiveEndpoints() {
        List<UserEndpoint> list = new ArrayList<>();

        Date now = new Date();
        for (UserEndpointCredentials uec : userEndpointCredentialsList) {
            String bearerToken = uec.getCredentialsWithClient().getCredentials().getBearerToken();
            if (bearerToken == null) continue;

            Date expiresAt = parseExpiresAt(bearerToken);
            if (expiresAt.after(now)) {
                list.add(uec.getUserEndpoint());
            }
        }

        return list;
    }

    private static final class UserEndpointCredentials {
        private UserEndpoint userEndpoint;
        private FHIRCredentialsWithClient credentialsWithClient;

        public UserEndpointCredentials(UserEndpoint userEndpoint, FHIRCredentialsWithClient credentialsWithClient) {
            this.userEndpoint = userEndpoint;
            this.credentialsWithClient = credentialsWithClient;
        }

        public UserEndpoint getUserEndpoint() {
            return userEndpoint;
        }

        public FHIRCredentialsWithClient getCredentialsWithClient() {
            return credentialsWithClient;
        }
    }

    public List<PatientModel> getAllPatientModels() {
        List<PatientModel> list = new ArrayList<>();
        for (UserEndpoint ue : getAllActiveEndpoints()) {
            list.add(getPatient(ue.getEndpoint()));
        }
        return list;
    }

    public List<AssessmentModel> getAllAssessmentModels() {
        List<AssessmentModel> list = new ArrayList<>();
        for (UserEndpoint ue : getAllActiveEndpoints()) {
            list.addAll(getAssessments(ue.getEndpoint()));
        }
        return list;
    }

    public List<CarePlanModel> getAllCarePlanModels() {
        List<CarePlanModel> list = new ArrayList<>();
        for (UserEndpoint ue : getAllActiveEndpoints()) {
            list.addAll(getCarePlans(ue.getEndpoint()));
        }
        return list;
    }

    public List<CareTeamModel> getAllCareTeamModels() {
        List<CareTeamModel> list = new ArrayList<>();
        for (UserEndpoint ue : getAllActiveEndpoints()) {
            list.addAll(getCareTeams(ue.getEndpoint()));
        }
        return list;
    }

    public List<ClinicalNoteModel> getAllClinicalNoteModels() {
        List<ClinicalNoteModel> list = new ArrayList<>();
        for (UserEndpoint ue : getAllActiveEndpoints()) {
            list.addAll(getClinicalNotes(ue.getEndpoint()));
        }
        return list;
    }

    public List<ConditionModel> getAllConditionModels() {
        List<ConditionModel> list = new ArrayList<>();
        for (UserEndpoint ue : getAllActiveEndpoints()) {
            list.addAll(getConditions(ue.getEndpoint()));
        }
        return list;
    }

    public List<DiagnosticReportModel> getAllDiagnosticReportModels() {
        List<DiagnosticReportModel> list = new ArrayList<>();
        for (UserEndpoint ue : getAllActiveEndpoints()) {
            list.addAll(getDiagnosticReports(ue.getEndpoint()));
        }
        return list;
    }

    public List<GoalModel> getAllGoalModels() {
        List<GoalModel> list = new ArrayList<>();
        for (UserEndpoint ue : getAllActiveEndpoints()) {
            list.addAll(getGoals(ue.getEndpoint()));
        }
        return list;
    }

    public List<ImmunizationModel> getAllImmunizationModels() {
        List<ImmunizationModel> list = new ArrayList<>();
        for (UserEndpoint ue : getAllActiveEndpoints()) {
            list.addAll(getImmunizations(ue.getEndpoint()));
        }
        return list;
    }

    public List<InteractionModel> getAllInteractionModels() {
        List<InteractionModel> list = new ArrayList<>();
        for (UserEndpoint ue : getAllActiveEndpoints()) {
            list.addAll(getInteractions(ue.getEndpoint()));
        }
        return list;
    }

    public List<MedicationModel> getAllMedicationModels() {
        List<MedicationModel> list = new ArrayList<>();
        for (UserEndpoint ue : getAllActiveEndpoints()) {
            list.addAll(getMedications(ue.getEndpoint()));
        }
        return list;
    }

    public List<ProcedureModel> getAllProcedureModels() {
        List<ProcedureModel> list = new ArrayList<>();
        for (UserEndpoint ue : getAllActiveEndpoints()) {
            list.addAll(getProcedures(ue.getEndpoint()));
        }
        return list;
    }

    public List<ServiceRequestModel> getAllServiceRequestModels() {
        List<ServiceRequestModel> list = new ArrayList<>();
        for (UserEndpoint ue : getAllActiveEndpoints()) {
            list.addAll(getServiceRequests(ue.getEndpoint()));
        }
        return list;
    }

    public List<SocialHistoryModel> getAllSocialHistoryModels() {
        List<SocialHistoryModel> list = new ArrayList<>();
        for (UserEndpoint ue : getAllActiveEndpoints()) {
            list.addAll(getSocialHistories(ue.getEndpoint()));
        }
        return list;
    }

    public List<SurveyObservationModel> getAllSurveyObservationModels() {
        List<SurveyObservationModel> list = new ArrayList<>();
        for (UserEndpoint ue : getAllActiveEndpoints()) {
            list.addAll(getSurveyObservations(ue.getEndpoint()));
        }
        return list;
    }

    public List<TestModel> getAllTestModels() {
        List<TestModel> list = new ArrayList<>();
        for (UserEndpoint ue : getAllActiveEndpoints()) {
            list.addAll(getTests(ue.getEndpoint()));
        }
        return list;
    }

    public List<VitalsModel> getAllVitalsModels() {
        List<VitalsModel> list = new ArrayList<>();
        for (UserEndpoint ue : getAllActiveEndpoints()) {
            list.addAll(getVitals(ue.getEndpoint()));
        }
        return list;
    }

///////////////////////////////////////////////////////////////////////////////////////
/// Data Set Caching Functions

    public PatientModel getPatient(Endpoint e) {
        return (PatientModel) getCachedDataSetForEndpoint(DataSetName.PATIENT, e);
    }

    @SuppressWarnings("unchecked")
    public List<AssessmentModel> getAssessments(Endpoint e) {
        return (List<AssessmentModel>) getCachedDataSetForEndpoint(DataSetName.ASSESSMENTS, e);
    }

    @SuppressWarnings("unchecked")
    public List<CarePlanModel> getCarePlans(Endpoint e) {
        return (List<CarePlanModel>) getCachedDataSetForEndpoint(DataSetName.CARE_PLANS, e);
    }

    @SuppressWarnings("unchecked")
    public List<CareTeamModel> getCareTeams(Endpoint e) {
        return (List<CareTeamModel>) getCachedDataSetForEndpoint(DataSetName.CARE_TEAMS, e);
    }

    @SuppressWarnings("unchecked")
    public List<ClinicalNoteModel> getClinicalNotes(Endpoint e) {
        return (List<ClinicalNoteModel>) getCachedDataSetForEndpoint(DataSetName.CLINICAL_NOTES, e);
    }

    @SuppressWarnings("unchecked")
    public List<ConditionModel> getConditions(Endpoint e) {
        return (List<ConditionModel>) getCachedDataSetForEndpoint(DataSetName.CONDITIONS, e);
    }

    @SuppressWarnings("unchecked")
    public List<DiagnosticReportModel> getDiagnosticReports(Endpoint e) {
        return (List<DiagnosticReportModel>) getCachedDataSetForEndpoint(DataSetName.DIAGNOSTIC_REPORTS, e);
    }

    @SuppressWarnings("unchecked")
    public List<GoalModel> getGoals(Endpoint e) {
        return (List<GoalModel>) getCachedDataSetForEndpoint(DataSetName.GOALS, e);
    }

    @SuppressWarnings("unchecked")
    public List<ImmunizationModel> getImmunizations(Endpoint e) {
        return (List<ImmunizationModel>) getCachedDataSetForEndpoint(DataSetName.IMMUNIZATIONS, e);
    }

    @SuppressWarnings("unchecked")
    public List<InteractionModel> getInteractions(Endpoint e) {
        return (List<InteractionModel>) getCachedDataSetForEndpoint(DataSetName.INTERACTIONS, e);
    }

    @SuppressWarnings("unchecked")
    public List<MedicationModel> getMedications(Endpoint e) {
        return (List<MedicationModel>) getCachedDataSetForEndpoint(DataSetName.MEDICATIONS, e);
    }

    @SuppressWarnings("unchecked")
    public List<ProcedureModel> getProcedures(Endpoint e) {
        return (List<ProcedureModel>) getCachedDataSetForEndpoint(DataSetName.PROCEDURES, e);
    }

    @SuppressWarnings("unchecked")
    public List<ServiceRequestModel> getServiceRequests(Endpoint e) {
        return (List<ServiceRequestModel>) getCachedDataSetForEndpoint(DataSetName.SERVICE_REQUESTS, e);
    }

    @SuppressWarnings("unchecked")
    public List<SocialHistoryModel> getSocialHistories(Endpoint e) {
        return (List<SocialHistoryModel>) getCachedDataSetForEndpoint(DataSetName.SOCIAL_HISTORIES, e);
    }

    @SuppressWarnings("unchecked")
    public List<SurveyObservationModel> getSurveyObservations(Endpoint e) {
        return (List<SurveyObservationModel>) getCachedDataSetForEndpoint(DataSetName.SURVEY_OBSERVATIONS, e);
    }

    @SuppressWarnings("unchecked")
    public List<TestModel> getTests(Endpoint e) {
        return (List<TestModel>) getCachedDataSetForEndpoint(DataSetName.TESTS, e);
    }

    @SuppressWarnings("unchecked")
    public List<VitalsModel> getVitals(Endpoint e) {
        return (List<VitalsModel>) getCachedDataSetForEndpoint(DataSetName.VITALS, e);
    }

    private String buildCacheKey(DataSetName dataSetName, Endpoint e) {
        return dataSetName + "-" + e.getIss();  // use iss instead of name.  it's possible that multiple
                                                // data sets will have different names but point to the same
                                                // iss.  ultimately, it's the iss we care about, irrespective
                                                // of what the user sees.  this will help prevent duplicates.
    }

    private Object getCachedDataSetForEndpoint(DataSetName dataSetName, Endpoint endpoint) {
        return cache.get(buildCacheKey(dataSetName, endpoint), s -> {
            long start = System.currentTimeMillis();
            logger.info("BEGIN build {} for session={}, userId={}, endpoint={}", dataSetName, sessionId, userId,
                    endpoint.getName());

            Object obj = null;
            try {
                switch (dataSetName) {
                    case PATIENT:
                        obj = dataSetBuilderService.buildPatient(sessionId, endpoint);
                        break;
                    case ASSESSMENTS:
                        obj = dataSetBuilderService.buildAssessments(sessionId, endpoint);
                        break;
                    case CARE_PLANS:
                        obj = dataSetBuilderService.buildCarePlans(sessionId, endpoint);
                        break;
                    case CARE_TEAMS:
                        obj = dataSetBuilderService.buildCareTeams(sessionId, endpoint);
                        break;
                    case CLINICAL_NOTES:
                        obj = dataSetBuilderService.buildClinicalNotes(sessionId, endpoint);
                        break;
                    case CONDITIONS:
                        obj = dataSetBuilderService.buildConditions(sessionId, endpoint);
                        break;
                    case DIAGNOSTIC_REPORTS:
                        obj = dataSetBuilderService.buildDiagnosticReports(sessionId, endpoint);
                        break;
                    case GOALS:
                        obj = dataSetBuilderService.buildGoals(sessionId, endpoint);
                        break;
                    case IMMUNIZATIONS:
                        obj = dataSetBuilderService.buildImmunizations(sessionId, endpoint);
                        break;
                    case INTERACTIONS:
                        obj = dataSetBuilderService.buildInteractions(sessionId, endpoint);
                        break;
                    case MEDICATIONS:
                        obj = dataSetBuilderService.buildMedications(sessionId, endpoint);
                        break;
                    case PROCEDURES:
                        obj = dataSetBuilderService.buildProcedures(sessionId, endpoint);
                        break;
                    case SERVICE_REQUESTS:
                        obj = dataSetBuilderService.buildServiceRequests(sessionId, endpoint);
                        break;
                    case SOCIAL_HISTORIES:
                        obj = dataSetBuilderService.buildSocialHistories(sessionId, endpoint);
                        break;
                    case SURVEY_OBSERVATIONS:
                        obj = dataSetBuilderService.buildSurveyObservations(sessionId, endpoint);
                        break;
                    case TESTS:
                        obj = dataSetBuilderService.buildTests(sessionId, endpoint);
                        break;
                    case VITALS:
                        obj = dataSetBuilderService.buildVitals(sessionId, endpoint);
                        break;
                    default:
                        throw new CaseNotHandledException("couldn't handle case for DataSetName=" + dataSetName);
                }

            } catch (Exception e) {
                if (e instanceof ForbiddenOperationException) {
                    logger.warn("attempt to retrieve {} from {} was forbidden - will not include {} for this session",
                            dataSetName, endpoint.getName(), dataSetName);
                    auditService.doAudit(sessionId, AuditSeverity.WARN, "cache population", "retrieving " + dataSetName +
                            " from " + endpoint.getName() + " was forbidden");

                    if (DataSetName.PATIENT.equals(dataSetName)) {
                        logger.error("Patient is required for system operation; aborting -");
                        throw (ForbiddenOperationException) e;
                    }

                } else if (e instanceof InvalidRequestException) {
                    logger.error("attempt to retrieve {} from {} triggered an InvalidRequestException - will not include {} for this session",
                            dataSetName, endpoint.getName(), dataSetName);
                    auditService.doAudit(sessionId, AuditSeverity.ERROR, "cache population", "invalid request retrieving " +
                            dataSetName + " from " + endpoint.getName());

                    if (DataSetName.PATIENT.equals(dataSetName)) {
                        logger.error("Patient is required for system operation; aborting -");
                        throw (InvalidRequestException) e;
                    }

                } else if (e instanceof RuntimeException) {
                    throw (RuntimeException) e;

                } else {
                    throw new RuntimeException(e);
                }
            }

            logger.info("DONE building {} for session={}, userId={}, endpoint={} (took {} ms)", dataSetName, sessionId,
                    userId, endpoint.getName(), (System.currentTimeMillis() - start));

            return obj;
        });
    }
}
