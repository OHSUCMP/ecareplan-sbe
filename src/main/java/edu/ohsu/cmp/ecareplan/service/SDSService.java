package edu.ohsu.cmp.ecareplan.service;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.entity.UserEndpoint;
import edu.ohsu.cmp.ecareplan.exception.ConfigurationException;
import edu.ohsu.cmp.ecareplan.exception.DataException;
import edu.ohsu.cmp.ecareplan.model.ProgressStatus;
import edu.ohsu.cmp.ecareplan.model.QueryModel;
import edu.ohsu.cmp.ecareplan.model.dataset.*;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentials;
import edu.ohsu.cmp.ecareplan.model.progress.ConsolidatedShareProgressModel;
import edu.ohsu.cmp.ecareplan.model.progress.IProgress;
import edu.ohsu.cmp.ecareplan.model.progress.ShareProgressModel;
import edu.ohsu.cmp.ecareplan.task.ShareTask;
import edu.ohsu.cmp.ecareplan.transform.ResourceTransformer;
import edu.ohsu.cmp.ecareplan.util.FhirUtil;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

@Service
public class SDSService extends BaseDataSetBuilderService implements IDataSetBuilder {
    private static final Logger logger = LoggerFactory.getLogger(SDSService.class);

    private static final String PARTITION_HEADER = "X-Partition-Name";

    @Value("${sds.fhirEndpointUrl}")
    private String sdsFhirEndpointUrl;

    @Autowired
    private QueryService queryService;

    @Autowired
    private MedicationFlagService medicationFlagService;

    @Autowired
    private BackgroundTaskService backgroundTaskService;

    private final ConcurrentMap<String, ConcurrentMap<String, ShareProgressModel>> sessionIdProgressMap;

    public SDSService() {
        sessionIdProgressMap = new ConcurrentHashMap<>();
    }

    @Override
    protected ServerValidationModeEnum getServerValidationMode() {
        return ServerValidationModeEnum.NEVER;
    }

    public void clearProgressForSession(String sessionId) {
        sessionIdProgressMap.remove(sessionId);
    }

    private List<ShareProgressModel> progressSnapshot(String sessionId) {
        Map<String, ShareProgressModel> progress = sessionIdProgressMap.get(sessionId);
        return progress == null ? List.of() : new ArrayList<>(progress.values());
    }

    public List<IProgress> getCurrentProgress(String sessionId) {
        List<ShareProgressModel> snapshot = progressSnapshot(sessionId);
        if (!snapshot.isEmpty()) {
            Map<String, List<ShareProgressModel>> map = snapshot.stream()
                    .collect(Collectors.groupingBy(ShareProgressModel::getEndpointName));

            List<IProgress> list = new ArrayList<>();
            for (List<ShareProgressModel> endpointProgressList : map.values()) {
                list.add(new ConsolidatedShareProgressModel(endpointProgressList));
            }

            return list;
        }

        return null;
    }

    public List<IProgress> getCurrentProgress(String sessionId, DataSet<?> dataSet) {
        List<IProgress> list = null;
        for (ShareProgressModel pm : progressSnapshot(sessionId)) {
            if (pm.getDataSet().equals(dataSet)) {
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(pm);
            }
        }
        return list;
    }

    public List<IProgress> getCurrentProgress(String sessionId, Endpoint endpoint) {
        List<IProgress> list = null;
        for (ShareProgressModel pm : progressSnapshot(sessionId)) {
            if (pm.getEndpoint().getId().equals(endpoint.getId())) {
                if (list == null) {
                    list = new ArrayList<>();
                }
                list.add(pm);
            }
        }
        return list;
    }

    public void clearAllCompletedProgress(String sessionId) {
        Map<String, ShareProgressModel> progress = sessionIdProgressMap.get(sessionId);
        if (progress != null) {
            progress.values().removeIf(pm -> pm.getFuture() == null || pm.getFuture().isDone());
        }
    }

    private String buildKey(DataSet<?> dataSet, Endpoint endpoint) {
        return dataSet.getName() + "|" + endpoint.getIss();
    }

    public Future<Void> shareToSDS(String sessionId, DataSet<?> dataSet, Endpoint endpoint, FHIRCredentials credentials,
                                   List<? extends BaseDataSetModel<?>> resources) {
        ConcurrentMap<String, ShareProgressModel> sessionProgress =
                sessionIdProgressMap.computeIfAbsent(sessionId, ignored -> new ConcurrentHashMap<>());
        String key = buildKey(dataSet, endpoint);

        // Publish the progress and its Future together. Concurrent callers for the same
        // key reuse the active task, even if it has reported COMPLETED but is still exiting.
        ShareProgressModel progress = sessionProgress.compute(key, (ignored, existing) -> {
            if (existing != null && existing.getFuture() != null && ! existing.getFuture().isDone()) {
                logger.warn("Sharing of {} from {} for session={} is already in progress", dataSet.getName(), endpoint.getName(), sessionId);
                return existing;
            }
            ProgressStatus status = resources.isEmpty() ?
                    ProgressStatus.COMPLETED :
                    ProgressStatus.WAITING_TO_START;
            ShareProgressModel created = new ShareProgressModel(dataSet, endpoint, status, 0, resources.size());
            if ( ! resources.isEmpty() ) {
                IGenericClient client = buildClient(credentials);
                ShareTask task = new ShareTask(sessionId, dataSet, endpoint, client, resources, created, auditService);
                created.setFuture(backgroundTaskService.submit(task));
            }
            return created;
        });
        return progress.getFuture();
    }

    @Override
    public List<PatientModel> buildPatients(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final IGenericClient client = buildClient(cfg.credentials());

        logger.info("building Patient from SDS for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        // note : we don't store the Patient "query" in the database as we do with everything else, since we will always
        //        read the Patient resource directly by reference.  this is so standard that we're able to safely hardcode it

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, ue.getEndpoint().getIss());

        PatientModel patientModel = rt.transformPatient(
                fhirService.readByReference(client, Patient.class, "Patient/" + cfg.endpointPatientId(), headers)
        );

        patientModel.setSourceEndpointName(ue.getEndpoint().getName());
        patientModel.setSourceEndpointIss(ue.getEndpoint().getIss());
        patientModel.setSourcedFromSDS(true);

        return List.of(patientModel);
    }

    @Override
    public List<CarePlanModel> buildCarePlans(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final IGenericClient client = buildClient(cfg.credentials());

        logger.info("building Care Plans from SDS for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, ue.getEndpoint().getIss());

        List<CarePlanModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.CARE_PLANS, ue.getEndpoint())) {
            list.addAll(
                    rt.transformCarePlans(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()), headers)
                    )
            );
        }

        for (CarePlanModel item : list) {
            item.setSourceEndpointName(ue.getEndpoint().getName());
            item.setSourceEndpointIss(ue.getEndpoint().getIss());
            item.setSourcedFromSDS(true);
        }

        return list;
    }

    @Override
    public List<CareTeamModel> buildCareTeams(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final IGenericClient client = buildClient(cfg.credentials());

        logger.info("building Care Teams from SDS for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, ue.getEndpoint().getIss());

        List<CareTeamModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.CARE_TEAMS, ue.getEndpoint())) {
            list.addAll(
                    rt.transformCareTeams(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()), headers)
                    )
            );
        }

        for (CareTeamModel item : list) {
            item.setSourceEndpointName(ue.getEndpoint().getName());
            item.setSourceEndpointIss(ue.getEndpoint().getIss());
            item.setSourcedFromSDS(true);
        }

        return list;
    }

    @Override
    public List<ClinicalNoteModel> buildClinicalNotes(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final IGenericClient client = buildClient(cfg.credentials());

        logger.info("building Clinical Notes from SDS for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, ue.getEndpoint().getIss());

        List<ClinicalNoteModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.CLINICAL_NOTES, ue.getEndpoint())) {
            list.addAll(
                    rt.transformClinicalNotes(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()), headers)
                    )
            );
        }

        for (ClinicalNoteModel item : list) {
            item.setSourceEndpointName(ue.getEndpoint().getName());
            item.setSourceEndpointIss(ue.getEndpoint().getIss());
            item.setSourcedFromSDS(true);
        }

        return list;
    }

    @Override
    public List<ConditionModel> buildConditions(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final IGenericClient client = buildClient(cfg.credentials());

        logger.info("building Conditions from SDS for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, ue.getEndpoint().getIss());

        List<ConditionModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.CONDITIONS, ue.getEndpoint())) {
            list.addAll(
                    rt.transformConditions(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()), headers)
                    )
            );
        }

        for (ConditionModel item : list) {
            item.setSourceEndpointName(ue.getEndpoint().getName());
            item.setSourceEndpointIss(ue.getEndpoint().getIss());
            item.setSourcedFromSDS(true);
        }

        return list;
    }

    @Override
    public List<DiagnosticReportModel> buildDiagnosticReports(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final IGenericClient client = buildClient(cfg.credentials());

        logger.info("building Diagnostic Reports from SDS for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, ue.getEndpoint().getIss());

        List<DiagnosticReportModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.DIAGNOSTIC_REPORTS, ue.getEndpoint())) {
            list.addAll(
                    rt.transformDiagnosticReports(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()), headers)
                    )
            );
        }

        for (DiagnosticReportModel item : list) {
            item.setSourceEndpointName(ue.getEndpoint().getName());
            item.setSourceEndpointIss(ue.getEndpoint().getIss());
            item.setSourcedFromSDS(true);
        }

        return list;
    }

    @Override
    public List<EncounterModel> buildEncounters(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final IGenericClient client = buildClient(cfg.credentials());

        logger.info("building Encounters from SDS for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, ue.getEndpoint().getIss());

        List<EncounterModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.ENCOUNTERS, ue.getEndpoint())) {
            list.addAll(
                    rt.transformEncounters(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()), headers)
                    )
            );
        }

        for (EncounterModel item : list) {
            item.setSourceEndpointName(ue.getEndpoint().getName());
            item.setSourceEndpointIss(ue.getEndpoint().getIss());
            item.setSourcedFromSDS(true);
        }

        return list;
    }

    @Override
    public List<GoalModel> buildGoals(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final IGenericClient client = buildClient(cfg.credentials());

        logger.info("building Goals from SDS for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, ue.getEndpoint().getIss());

        List<GoalModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.GOALS, ue.getEndpoint())) {
            list.addAll(
                    rt.transformGoals(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()), headers)
                    )
            );
        }

        for (GoalModel item : list) {
            item.setSourceEndpointName(ue.getEndpoint().getName());
            item.setSourceEndpointIss(ue.getEndpoint().getIss());
            item.setSourcedFromSDS(true);
        }

        return list;
    }

    @Override
    public List<ImmunizationModel> buildImmunizations(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final IGenericClient client = buildClient(cfg.credentials());

        logger.info("building Immunizations from SDS for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, ue.getEndpoint().getIss());

        List<ImmunizationModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.IMMUNIZATIONS, ue.getEndpoint())) {
            list.addAll(
                    rt.transformImmunizations(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()), headers)
                    )
            );
        }

        for (ImmunizationModel item : list) {
            item.setSourceEndpointName(ue.getEndpoint().getName());
            item.setSourceEndpointIss(ue.getEndpoint().getIss());
            item.setSourcedFromSDS(true);
        }

        return list;
    }

    @Override
    public List<LabResultModel> buildLabResults(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final IGenericClient client = buildClient(cfg.credentials());

        logger.info("building Lab Results from SDS for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, ue.getEndpoint().getIss());

        List<LabResultModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.LAB_RESULTS, ue.getEndpoint())) {
            list.addAll(
                    rt.transformLabResults(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()), headers)
                    )
            );
        }

        for (LabResultModel item : list) {
            item.setSourceEndpointName(ue.getEndpoint().getName());
            item.setSourceEndpointIss(ue.getEndpoint().getIss());
            item.setSourcedFromSDS(true);
        }

        return list;
    }

    @Override
    public List<MedicationModel> buildMedications(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final IGenericClient client = buildClient(cfg.credentials());

        logger.info("building Medications from SDS for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, ue.getEndpoint().getIss());

        List<MedicationModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.MEDICATIONS, ue.getEndpoint())) {
            list.addAll(
                    rt.transformMedications(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()), headers)
                    )
            );
        }

        for (MedicationModel mm : list) {
            medicationFlagService.appendMedicationFlags(mm);
        }

        for (MedicationModel item : list) {
            item.setSourceEndpointName(ue.getEndpoint().getName());
            item.setSourceEndpointIss(ue.getEndpoint().getIss());
            item.setSourcedFromSDS(true);
        }

        return list;
    }

    @Override
    public List<ProcedureModel> buildProcedures(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final IGenericClient client = buildClient(cfg.credentials());

        logger.info("building Procedures from SDS for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, ue.getEndpoint().getIss());

        List<ProcedureModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.PROCEDURES, ue.getEndpoint())) {
            list.addAll(
                    rt.transformProcedures(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()), headers)
                    )
            );
        }

        for (ProcedureModel item : list) {
            item.setSourceEndpointName(ue.getEndpoint().getName());
            item.setSourceEndpointIss(ue.getEndpoint().getIss());
            item.setSourcedFromSDS(true);
        }

        return list;
    }

    @Override
    public List<QuestionnaireResponseModel> buildQuestionnaireResponses(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final IGenericClient client = buildClient(cfg.credentials());

        logger.info("building Questionnaire Responses from SDS for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, ue.getEndpoint().getIss());

        List<QuestionnaireResponseModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.QUESTIONNAIRE_RESPONSES, ue.getEndpoint())) {
            list.addAll(
                    rt.transformQuestionnaireResponses(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()), headers)
                    )
            );
        }

        for (QuestionnaireResponseModel item : list) {
            item.setSourceEndpointName(ue.getEndpoint().getName());
            item.setSourceEndpointIss(ue.getEndpoint().getIss());
            item.setSourcedFromSDS(true);
        }

        return list;
    }

    @Override
    public List<ServiceRequestModel> buildServiceRequests(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final IGenericClient client = buildClient(cfg.credentials());

        logger.info("building Service Requests from SDS for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, ue.getEndpoint().getIss());

        List<ServiceRequestModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.SERVICE_REQUESTS, ue.getEndpoint())) {
            list.addAll(
                    rt.transformServiceRequests(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()), headers)
                    )
            );
        }

        for (ServiceRequestModel item : list) {
            item.setSourceEndpointName(ue.getEndpoint().getName());
            item.setSourceEndpointIss(ue.getEndpoint().getIss());
            item.setSourcedFromSDS(true);
        }

        return list;
    }

    @Override
    public List<SocialHistoryModel> buildSocialHistories(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final IGenericClient client = buildClient(cfg.credentials());

        logger.info("building Social Histories from SDS for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, ue.getEndpoint().getIss());

        List<SocialHistoryModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.SOCIAL_HISTORIES, ue.getEndpoint())) {
            list.addAll(
                    rt.transformSocialHistories(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()), headers)
                    )
            );
        }

        for (SocialHistoryModel item : list) {
            item.setSourceEndpointName(ue.getEndpoint().getName());
            item.setSourceEndpointIss(ue.getEndpoint().getIss());
            item.setSourcedFromSDS(true);
        }

        return list;
    }

    @Override
    public List<SurveyObservationModel> buildSurveyObservations(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final IGenericClient client = buildClient(cfg.credentials());

        logger.info("building Survey Observations from SDS for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, ue.getEndpoint().getIss());

        List<SurveyObservationModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.SURVEY_OBSERVATIONS, ue.getEndpoint())) {
            list.addAll(
                    rt.transformSurveyObservations(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()), headers)
                    )
            );
        }

        for (SurveyObservationModel item : list) {
            item.setSourceEndpointName(ue.getEndpoint().getName());
            item.setSourceEndpointIss(ue.getEndpoint().getIss());
            item.setSourcedFromSDS(true);
        }

        return list;
    }

    @Override
    public List<VitalsModel> buildVitals(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final IGenericClient client = buildClient(cfg.credentials());

        logger.info("building Vitals from SDS for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put(PARTITION_HEADER, ue.getEndpoint().getIss());

        List<VitalsModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.VITALS, ue.getEndpoint())) {
            list.addAll(
                    rt.transformVitals(
                            fhirService.search(client, sdsFhirEndpointUrl, doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()), headers)
                    )
            );
        }

        for (VitalsModel item : list) {
            item.setSourceEndpointName(ue.getEndpoint().getName());
            item.setSourceEndpointIss(ue.getEndpoint().getIss());
            item.setSourcedFromSDS(true);
        }

        return list;
    }


//////////////////////////////////////////////////////////////
/// private methods
///

    private IGenericClient buildClient(FHIRCredentials fc) {
        return FhirUtil.buildClient(fhirContext, sdsFhirEndpointUrl, fc.getBearerToken());
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
