package edu.ohsu.cmp.ecareplan.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.exception.ConfigurationException;
import edu.ohsu.cmp.ecareplan.exception.DataException;
import edu.ohsu.cmp.ecareplan.model.ProgressModel;
import edu.ohsu.cmp.ecareplan.model.ProgressStatus;
import edu.ohsu.cmp.ecareplan.model.QueryModel;
import edu.ohsu.cmp.ecareplan.model.dataset.*;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.ecareplan.transform.ResourceTransformer;
import edu.ohsu.cmp.ecareplan.util.ExecutorUtil;
import edu.ohsu.cmp.ecareplan.util.FhirUtil;
import edu.ohsu.cmp.ecareplan.workspace.UserWorkspace;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class SDSService extends BaseService implements IDataSetBuilder {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String PARTITION_HEADER = "X-Partition-Name";
    private static final int POOL_SIZE = 5;

    @Value("${socket.timeout:300000}")
    private Integer socketTimeout;

    @Value("${sds.fhirEndpointUrl}")
    private String sdsFhirEndpointUrl;

    @Autowired
    private EndpointService endpointService;

    @Autowired
    private QueryService queryService;

    private final ExecutorService executorService;
    private final Map<String, Map<String, ProgressModel>> sessionIdProgressMap;

    public SDSService() {
        executorService = Executors.newFixedThreadPool(POOL_SIZE);
        sessionIdProgressMap = Collections.synchronizedMap(new HashMap<>());
    }

    public void shutdown() {
        ExecutorUtil.shutdownAndAwaitTermination(executorService, 60);
        sessionIdProgressMap.clear();
    }

    public List<ProgressModel> getCurrentProgress(String sessionId) {
        return sessionIdProgressMap.containsKey(sessionId) ?
                new ArrayList<>(sessionIdProgressMap.get(sessionId).values()) :
                null;
    }

    public void clearCompletedProgress(String sessionId) {
        if (sessionIdProgressMap.containsKey(sessionId)) {
            Iterator<ProgressModel> iter = sessionIdProgressMap.get(sessionId).values().iterator();
            while (iter.hasNext()) {
                ProgressModel pm = iter.next();
                if (pm.getStatus() == ProgressStatus.COMPLETED) {
                    iter.remove();
                }
            }
            if (sessionIdProgressMap.get(sessionId).isEmpty()) {
                sessionIdProgressMap.remove(sessionId);
            }
        }
    }

    public void shareToSDS(String sessionId, DataSet<?> dataSet, Endpoint endpoint) {
        if ( ! sessionIdProgressMap.containsKey(sessionId) ) {
            sessionIdProgressMap.put(sessionId, Collections.synchronizedMap(new LinkedHashMap<>()));
        }

        if (sessionIdProgressMap.get(sessionId).containsKey(dataSet.getName())) {
            ProgressModel progress = sessionIdProgressMap.get(sessionId).get(dataSet.getName());
            switch (progress.getStatus()) {
                case INITIALIZING:
                case RUNNING:
                    logger.warn("Sharing of {} for session={} is already in progress", dataSet.getName(), sessionId);
                    return;
                case COMPLETED:
                    sessionIdProgressMap.get(sessionId).remove(dataSet.getName());
            }
        }

        IGenericClient client = buildClient(sessionId);

        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        List<? extends BaseDataSetModel<?>> list = workspace.getCachedDataSetModelsForEndpoint(dataSet, endpoint);

        ProgressStatus status;
        String message;
        if (list.isEmpty()) {
            status = ProgressStatus.COMPLETED;
            message = "No data to share.";
        } else {
            status = ProgressStatus.INITIALIZING;
            message = "Initializing...";
        }

        final ProgressModel progress = new ProgressModel("Sharing " + dataSet.getName() + " resources to SDS", status, message, 0, list.size());;

        sessionIdProgressMap.get(sessionId).put(dataSet.getName(), progress);

        if (ProgressStatus.COMPLETED.equals(progress.getStatus())) {
            return;
        }

        Runnable shareRunnable = new Runnable() {
            @Override
            public void run() {
                if (ProgressStatus.INITIALIZING.equals(progress.getStatus())) {
                    progress.setStatus(ProgressStatus.RUNNING);
                    progress.setMessage("Processing...");
                }

                final int maxAttempts = 10;

                for (BaseDataSetModel<?> item : list) {
                    try {
                        final IDomainResource resource = item.toResourceForSDSExport();
                        String id = FhirUtil.toRelativeReference(resource.getId());

                        int attempt = 0;
                        boolean success = false;
                        while ( ! success && attempt++ < maxAttempts ) {
                            if (attempt > 1) {
                                logger.info("Re-attempting share of {} with id={} from {} for session {} ({}/{})",
                                        resource.getClass().getSimpleName(), id, endpoint.getName(), sessionId, attempt, maxAttempts);
                            }

                            try {
                                MethodOutcome outcome = client.update()
                                        .resource(resource)
                                        .withId(id)
                                        .withAdditionalHeader(PARTITION_HEADER, endpoint.getIss())
                                        .execute();

                                success = outcome.getResponseStatusCode() >= 200 && outcome.getResponseStatusCode() < 300;
                                if (success) {
                                    logger.info("Successfully shared {} with id={} from {} for session={}",
                                            resource.getClass().getSimpleName(), id, endpoint.getName(), sessionId);

                                } else {
                                    logger.debug("Failed sharing {} with id={} from {} with status code {} ({}/{})",
                                            resource.getClass().getSimpleName(), id, endpoint.getName(),
                                            outcome.getResponseStatusCode(), attempt, maxAttempts);
                                }

                            } catch (Exception e) {
                                logger.error("caught {} sharing {} with id={} from {} for session={} - {}", e.getClass().getSimpleName(),
                                        resource.getClass().getSimpleName(), id, endpoint.getName(), sessionId, e.getMessage());
                                logger.debug(e.getMessage(), e);
                            }
                        }

                        if ( ! success ) {
                            progress.addError("Failed to share " + resource.getClass().getSimpleName() + " with id=" + id);
                        }

                    } catch (Exception e) {
                        logger.error("caught {} sharing {} with id={} from {} for session={} - {}", e.getClass().getSimpleName(),
                                dataSet.getName(), item.getId(), endpoint.getName(), sessionId, e.getMessage());
                        logger.debug(e.getMessage(), e);

                    } finally {
                        if (progress.getCurrent() < progress.getMax()) {
                            progress.incrementCurrent();
                        }

                        if (progress.getCurrent().equals(progress.getMax())) {
                            progress.setStatus(ProgressStatus.COMPLETED);
                            progress.setMessage("Sharing complete.");
                        }
                    }
                }

                if ( ! progress.getCurrent().equals(progress.getMax()) ) {
                    logger.warn("somehow got through all list items for " + dataSet.getName() +
                            " from " + endpoint.getName() + ", but progress current != max?  that's weird.  investigate?");
                    progress.setStatus(ProgressStatus.COMPLETED);
                    progress.setMessage("Sharing complete.");
                }
            }
        };

        executorService.submit(shareRunnable);
    }

    @Override
    public List<PatientModel> buildPatients(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Patient from SDS for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        IGenericClient client = buildClient(sessionId);

        // note : we don't store the Patient "query" in the database as we do with everything else, since we will always
        //        read the Patient resource directly by reference.  this is so standard that we're able to safely hardcode it

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, e.getIss());

        PatientModel patientModel = rt.transformPatient(
                fhirService.readByReference(client, Patient.class, "Patient/" + workspace.getPatientIdForEndpoint(e), headers)
        );

        patientModel.setSourceEndpointName(e.getName());
        patientModel.setSourceEndpointIss(e.getIss());

        return List.of(patientModel);
    }

    @Override
    public List<CarePlanModel> buildCarePlans(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Care Plans from SDS for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        IGenericClient client = buildClient(sessionId);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, e.getIss());

        List<CarePlanModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.CARE_PLANS, e)) {
            list.addAll(
                    rt.transformCarePlans(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(workspace.getPatientIdForEndpoint(e), qm.getQuery()), headers)
                    )
            );
        }

        for (CarePlanModel item : list) {
            item.setSourceEndpointName(e.getName());
            item.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<CareTeamModel> buildCareTeams(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Care Teams from SDS for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        IGenericClient client = buildClient(sessionId);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, e.getIss());

        List<CareTeamModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.CARE_TEAMS, e)) {
            list.addAll(
                    rt.transformCareTeams(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(workspace.getPatientIdForEndpoint(e), qm.getQuery()), headers)
                    )
            );
        }

        for (CareTeamModel item : list) {
            item.setSourceEndpointName(e.getName());
            item.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<ClinicalNoteModel> buildClinicalNotes(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Clinical Notes from SDS for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        IGenericClient client = buildClient(sessionId);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, e.getIss());

        List<ClinicalNoteModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.CLINICAL_NOTES, e)) {
            list.addAll(
                    rt.transformClinicalNotes(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(workspace.getPatientIdForEndpoint(e), qm.getQuery()), headers)
                    )
            );
        }

        for (ClinicalNoteModel item : list) {
            item.setSourceEndpointName(e.getName());
            item.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<ConditionModel> buildConditions(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Conditions from SDS for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        IGenericClient client = buildClient(sessionId);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, e.getIss());

        List<ConditionModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.CONDITIONS, e)) {
            list.addAll(
                    rt.transformConditions(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(workspace.getPatientIdForEndpoint(e), qm.getQuery()), headers)
                    )
            );
        }

        for (ConditionModel item : list) {
            item.setSourceEndpointName(e.getName());
            item.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<DiagnosticReportModel> buildDiagnosticReports(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Diagnostic Reports from SDS for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        IGenericClient client = buildClient(sessionId);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, e.getIss());

        List<DiagnosticReportModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.DIAGNOSTIC_REPORTS, e)) {
            list.addAll(
                    rt.transformDiagnosticReports(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(workspace.getPatientIdForEndpoint(e), qm.getQuery()), headers)
                    )
            );
        }

        for (DiagnosticReportModel item : list) {
            item.setSourceEndpointName(e.getName());
            item.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<EncounterModel> buildEncounters(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Encounters from SDS for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        IGenericClient client = buildClient(sessionId);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, e.getIss());

        List<EncounterModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.ENCOUNTERS, e)) {
            list.addAll(
                    rt.transformEncounters(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(workspace.getPatientIdForEndpoint(e), qm.getQuery()), headers)
                    )
            );
        }

        for (EncounterModel item : list) {
            item.setSourceEndpointName(e.getName());
            item.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<GoalModel> buildGoals(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Goals from SDS for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        IGenericClient client = buildClient(sessionId);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, e.getIss());

        List<GoalModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.GOALS, e)) {
            list.addAll(
                    rt.transformGoals(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(workspace.getPatientIdForEndpoint(e), qm.getQuery()), headers)
                    )
            );
        }

        for (GoalModel item : list) {
            item.setSourceEndpointName(e.getName());
            item.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<ImmunizationModel> buildImmunizations(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Immunizations from SDS for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        IGenericClient client = buildClient(sessionId);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, e.getIss());

        List<ImmunizationModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.IMMUNIZATIONS, e)) {
            list.addAll(
                    rt.transformImmunizations(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(workspace.getPatientIdForEndpoint(e), qm.getQuery()), headers)
                    )
            );
        }

        for (ImmunizationModel item : list) {
            item.setSourceEndpointName(e.getName());
            item.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<LabResultModel> buildLabResults(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Lab Results from SDS for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        IGenericClient client = buildClient(sessionId);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, e.getIss());

        List<LabResultModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.LAB_RESULTS, e)) {
            list.addAll(
                    rt.transformLabResults(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(workspace.getPatientIdForEndpoint(e), qm.getQuery()), headers)
                    )
            );
        }

        for (LabResultModel item : list) {
            item.setSourceEndpointName(e.getName());
            item.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<MedicationModel> buildMedications(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Medications from SDS for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        IGenericClient client = buildClient(sessionId);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, e.getIss());

        List<MedicationModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.MEDICATIONS, e)) {
            list.addAll(
                    rt.transformMedications(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(workspace.getPatientIdForEndpoint(e), qm.getQuery()), headers)
                    )
            );
        }

        for (MedicationModel item : list) {
            item.setSourceEndpointName(e.getName());
            item.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<ProcedureModel> buildProcedures(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Procedures from SDS for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        IGenericClient client = buildClient(sessionId);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, e.getIss());

        List<ProcedureModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.PROCEDURES, e)) {
            list.addAll(
                    rt.transformProcedures(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(workspace.getPatientIdForEndpoint(e), qm.getQuery()), headers)
                    )
            );
        }

        for (ProcedureModel item : list) {
            item.setSourceEndpointName(e.getName());
            item.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<QuestionnaireResponseModel> buildQuestionnaireResponses(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Questionnaire Responses from SDS for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        IGenericClient client = buildClient(sessionId);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, e.getIss());

        List<QuestionnaireResponseModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.QUESTIONNAIRE_RESPONSES, e)) {
            list.addAll(
                    rt.transformQuestionnaireResponses(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(workspace.getPatientIdForEndpoint(e), qm.getQuery()), headers)
                    )
            );
        }

        for (QuestionnaireResponseModel item : list) {
            item.setSourceEndpointName(e.getName());
            item.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<ServiceRequestModel> buildServiceRequests(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Service Requests from SDS for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        IGenericClient client = buildClient(sessionId);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, e.getIss());

        List<ServiceRequestModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.SERVICE_REQUESTS, e)) {
            list.addAll(
                    rt.transformServiceRequests(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(workspace.getPatientIdForEndpoint(e), qm.getQuery()), headers)
                    )
            );
        }

        for (ServiceRequestModel item : list) {
            item.setSourceEndpointName(e.getName());
            item.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<SocialHistoryModel> buildSocialHistories(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Social Histories from SDS for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        IGenericClient client = buildClient(sessionId);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, e.getIss());

        List<SocialHistoryModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.SOCIAL_HISTORIES, e)) {
            list.addAll(
                    rt.transformSocialHistories(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(workspace.getPatientIdForEndpoint(e), qm.getQuery()), headers)
                    )
            );
        }

        for (SocialHistoryModel item : list) {
            item.setSourceEndpointName(e.getName());
            item.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<SurveyObservationModel> buildSurveyObservations(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Survey Observations from SDS for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        IGenericClient client = buildClient(sessionId);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, e.getIss());

        List<SurveyObservationModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.SURVEY_OBSERVATIONS, e)) {
            list.addAll(
                    rt.transformSurveyObservations(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(workspace.getPatientIdForEndpoint(e), qm.getQuery()), headers)
                    )
            );
        }

        for (SurveyObservationModel item : list) {
            item.setSourceEndpointName(e.getName());
            item.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<VitalsModel> buildVitals(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Vitals from SDS for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        IGenericClient client = buildClient(sessionId);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, e.getIss());

        List<VitalsModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.VITALS, e)) {
            list.addAll(
                    rt.transformVitals(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(workspace.getPatientIdForEndpoint(e), qm.getQuery()), headers)
                    )
            );
        }

        for (VitalsModel item : list) {
            item.setSourceEndpointName(e.getName());
            item.setSourceEndpointIss(e.getIss());
        }

        return list;
    }


//////////////////////////////////////////////////////////////
/// private methods
///

    private IGenericClient buildClient(String sessionId) {
        Endpoint patientLaunchEndpoint = endpointService.getPatientLaunchEndpoint();
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(patientLaunchEndpoint);
        return FhirUtil.buildClient(sdsFhirEndpointUrl, fcc.getCredentials().getBearerToken(), socketTimeout, false);
    }

    private String doTokenReplacements(String patientId, String fhirQuery) {
        if (fhirQuery == null) return null;

        Map<String, String> params = new LinkedHashMap<>();
        int start = fhirQuery.indexOf("?");
        if (start > 0) {
            String[] parts = fhirQuery.substring(start + 1).split("&");
            for (String part : parts) {
                String[] keyValue = part.split("=");
                if (keyValue.length == 2) {
                    params.put(keyValue[0], keyValue[1]);
                }
            }
            fhirQuery = fhirQuery.substring(0, start);
        }

        Iterator<Map.Entry<String, String>> iter = params.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, String> entry = iter.next();
            if (entry.getValue().equals("{PATIENT}")) {             // replace patient ID placeholder with actual
                entry.setValue(patientId);
            } else if (entry.getValue().contains("_YEARS_AGO}")) {  // remove any date filters
                iter.remove();
            }
        }

        List<String> paramList = new ArrayList<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            paramList.add(entry.getKey() + "=" + entry.getValue());
        }

        return params.isEmpty() ?
                fhirQuery :
                fhirQuery + "?" + String.join("&", paramList);
    }
}
