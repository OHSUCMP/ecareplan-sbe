package edu.ohsu.cmp.ecareplan.service;

import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.entity.User;
import edu.ohsu.cmp.ecareplan.entity.UserEndpoint;
import edu.ohsu.cmp.ecareplan.exception.ConfigurationException;
import edu.ohsu.cmp.ecareplan.exception.DataException;
import edu.ohsu.cmp.ecareplan.model.EndpointModel;
import edu.ohsu.cmp.ecareplan.model.QueryModel;
import edu.ohsu.cmp.ecareplan.model.dataset.*;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRStrategy;
import edu.ohsu.cmp.ecareplan.model.fhir.ResourceWithBundle;
import edu.ohsu.cmp.ecareplan.repository.EndpointRepository;
import edu.ohsu.cmp.ecareplan.repository.UserEndpointRepository;
import edu.ohsu.cmp.ecareplan.transform.ResourceTransformer;
import edu.ohsu.cmp.ecareplan.util.CryptoUtil;
import edu.ohsu.cmp.ecareplan.util.FhirUtil;
import edu.ohsu.cmp.ecareplan.workspace.UserWorkspace;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidParameterSpecException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Service
public class EndpointService extends BaseService implements IDataSetBuilder {
    private static final Logger logger = LoggerFactory.getLogger(EndpointService.class);

    @Value("${endpoint.patientLaunch.name}")
    private String patientEndpointName;

    @Value("${endpoint.careTeamLaunch.name}")
    private String careTeamEndpointName;

    @Autowired
    private EndpointRepository endpointRepository;

    @Autowired
    private UserEndpointRepository userEndpointRepository;

    @Autowired
    private FHIRService fhirService;

    @Autowired
    private QueryService queryService;

    @Autowired
    private MedicationFlagService medicationFlagService;

    public Endpoint getPatientLaunchEndpoint() {
        return endpointRepository.findByName(patientEndpointName);
    }

    public Endpoint getCareTeamLaunchEndpoint() {
        return endpointRepository.findByName(careTeamEndpointName);
    }

    public Endpoint getEndpoint(Long endpointId) {
        return endpointRepository.findById(endpointId).orElseThrow();
    }

    public List<EndpointModel> getAllThirdPartyEndpoints() {
        List<EndpointModel> list = new ArrayList<>();
        for (Endpoint endpoint : endpointRepository.findAll(Sort.by("name").ascending())) {
            if (endpoint.getName().equals(patientEndpointName) ||
                    endpoint.getName().equals(careTeamEndpointName)) {
                continue;
            }
            list.add(new EndpointModel(endpoint));
        }
        return list;
    }

    public List<UserEndpoint> getAllUserEndpoints(User user) {
        return userEndpointRepository.findByUserId(user.getId());
    }

    public UserEndpoint getUserEndpoint(User user, Endpoint endpoint) {
        return userEndpointRepository.findByUserIdAndEndpointId(user.getId(), endpoint.getId()).orElseThrow();
    }

    public UserEndpoint createUserEndpoint(User user, Endpoint endpoint, String fhirPatientId, String refreshToken, SecretKey secretKey) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, InvalidParameterSpecException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        UserEndpoint ue = new UserEndpoint();
        ue.setUserId(user.getId());
        ue.setEndpoint(endpoint);
        ue.setEncryptedPatientId(CryptoUtil.encrypt(fhirPatientId, secretKey));
        if (refreshToken != null) ue.setEncryptedRefreshToken(CryptoUtil.encrypt(refreshToken, secretKey));
        ue.setCreated(new Date());
        userEndpointRepository.save(ue);
        return ue;
    }

    public UserEndpoint updateUserEndpointLastSyncCompleted(UserEndpoint ue) {
        ue.setLastSyncCompleted(new Date());
        return userEndpointRepository.save(ue);
    }

    public UserEndpoint clearUserEndpointLastSyncCompleted(UserEndpoint ue) {
        ue.setLastSyncCompleted(null);
        return userEndpointRepository.save(ue);
    }

    public UserEndpoint updateUserEndpointRefreshToken(UserEndpoint userEndpoint, String refreshToken, SecretKey secretKey) throws NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, InvalidParameterSpecException, BadPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        userEndpoint.setEncryptedRefreshToken(CryptoUtil.encrypt(refreshToken, secretKey));
        return userEndpointRepository.save(userEndpoint);
    }

    @Override
    public List<PatientModel> buildPatients(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Patient for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());

        // note : we don't store the Patient "query" in the database as we do with everything else, since we will always
        //        read the Patient resource directly by reference.  this is so standard that we're able to safely hardcode it

        PatientModel patientModel = rt.transformPatient(
                fhirService.readByReference(fcc, FHIRStrategy.PATIENT, Patient.class, "Patient/" + fcc.getCredentials().getPatientId())
        );

        patientModel.setSourceEndpointName(e.getName());
        patientModel.setSourceEndpointIss(e.getIss());

        return List.of(patientModel);
    }

    @Override
    public List<CarePlanModel> buildCarePlans(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Care Plans for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<CarePlanModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.CARE_PLANS, e)) {
            list.addAll(
                    rt.transformCarePlans(
                            fhirService.search(fcc, qm.getStrategy(), doTokenReplacements(fcc.getCredentials().getPatientId(), qm.getQuery()))
                    )
            );
        }

        for (CarePlanModel cp : list) {
            cp.setSourceEndpointName(e.getName());
            cp.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<CareTeamModel> buildCareTeams(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Care Teams for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<CareTeamModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.CARE_TEAMS, e)) {
            list.addAll(
                    rt.transformCareTeams(
                            fhirService.search(fcc, qm.getStrategy(), doTokenReplacements(fcc.getCredentials().getPatientId(), qm.getQuery()))
                    )
            );
        }

        for (CareTeamModel ct : list) {
            ct.setSourceEndpointName(e.getName());
            ct.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<ClinicalNoteModel> buildClinicalNotes(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Clinical Notes for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<ClinicalNoteModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.CLINICAL_NOTES, e)) {
            list.addAll(
                    rt.transformClinicalNotes(
                            fhirService.search(fcc, qm.getStrategy(),
                                    doTokenReplacements(fcc.getCredentials().getPatientId(), qm.getQuery()),
                                    null,
                                    new Function<ResourceWithBundle, List<Resource>>() {
                                        @Override
                                        public List<Resource> apply(ResourceWithBundle resourceWithBundle) {
                                            if (resourceWithBundle.getResource() instanceof DocumentReference) {
                                                DocumentReference dr = (DocumentReference) resourceWithBundle.getResource();
                                                List<Resource> list = new ArrayList<>();
                                                if (dr.hasContent()) {
                                                    for (DocumentReference.DocumentReferenceContentComponent content : dr.getContent()) {
                                                        if (content.hasAttachment() && content.getAttachment().hasUrl()) {
                                                            if ( ! FhirUtil.bundleContainsReference(resourceWithBundle.getBundle(), content.getAttachment().getUrl()) ) {
                                                                try {
                                                                    list.add(
                                                                            fhirService.readByReference(fcc, FHIRStrategy.PATIENT, Binary.class, content.getAttachment().getUrl())
                                                                    );
                                                                } catch (Exception e) {
                                                                    logger.error("Error reading binary reference: " + content.getAttachment().getUrl(), e);
                                                                    if (e instanceof AuthenticationException ae) throw ae;
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                return list;
                                            }
                                            return null;
                                        }
                                    })
                    )
            );
        }

        for (ClinicalNoteModel cn : list) {
            cn.setSourceEndpointName(e.getName());
            cn.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<ConditionModel> buildConditions(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Conditions for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<ConditionModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.CONDITIONS, e)) {
            list.addAll(
                    rt.transformConditions(
                            fhirService.search(fcc, qm.getStrategy(), doTokenReplacements(fcc.getCredentials().getPatientId(), qm.getQuery()))
                    )
            );
        }

        for (ConditionModel cm : list) {
            cm.setSourceEndpointName(e.getName());
            cm.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<DiagnosticReportModel> buildDiagnosticReports(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Diagnostic Reports for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<DiagnosticReportModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.DIAGNOSTIC_REPORTS, e)) {
            list.addAll(
                    rt.transformDiagnosticReports(
                            fhirService.search(fcc, qm.getStrategy(), doTokenReplacements(fcc.getCredentials().getPatientId(), qm.getQuery()))
                    )
            );
        }

        for (DiagnosticReportModel dr : list) {
            dr.setSourceEndpointName(e.getName());
            dr.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<EncounterModel> buildEncounters(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Encounters for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<EncounterModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.ENCOUNTERS, e)) {
            list.addAll(
                    rt.transformEncounters(
                            fhirService.search(fcc, qm.getStrategy(), doTokenReplacements(fcc.getCredentials().getPatientId(), qm.getQuery()))
                    )
            );
        }

        for (EncounterModel em : list) {
            em.setSourceEndpointName(e.getName());
            em.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<GoalModel> buildGoals(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Goals for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<GoalModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.GOALS, e)) {
            list.addAll(
                    rt.transformGoals(
                            fhirService.search(fcc, qm.getStrategy(), doTokenReplacements(fcc.getCredentials().getPatientId(), qm.getQuery()))
                    )
            );
        }

        for (GoalModel gm : list) {
            gm.setSourceEndpointName(e.getName());
            gm.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<ImmunizationModel> buildImmunizations(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Immunizations for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<ImmunizationModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.IMMUNIZATIONS, e)) {
            list.addAll(
                    rt.transformImmunizations(
                            fhirService.search(fcc, qm.getStrategy(), doTokenReplacements(fcc.getCredentials().getPatientId(), qm.getQuery()))
                    )
            );
        }

        for (ImmunizationModel im : list) {
            im.setSourceEndpointName(e.getName());
            im.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<LabResultModel> buildLabResults(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Lab Results for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<LabResultModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.LAB_RESULTS, e)) {
            list.addAll(
                    rt.transformLabResults(
                            fhirService.search(fcc, qm.getStrategy(), doTokenReplacements(fcc.getCredentials().getPatientId(), qm.getQuery()))
                    )
            );
        }

        for (LabResultModel tm : list) {
            tm.setSourceEndpointName(e.getName());
            tm.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<MedicationModel> buildMedications(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Medications for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<MedicationModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.MEDICATIONS, e)) {
            list.addAll(
                    rt.transformMedications(
                            fhirService.search(fcc, qm.getStrategy(),
                                    doTokenReplacements(fcc.getCredentials().getPatientId(), qm.getQuery()),
                                    null,
                                    new Function<ResourceWithBundle, List<Resource>>() {
                                        @Override
                                        public List<Resource> apply(ResourceWithBundle resourceWithBundle) {
                                            if (resourceWithBundle.getResource() instanceof MedicationRequest) {
                                                MedicationRequest mr = (MedicationRequest) resourceWithBundle.getResource();
                                                List<Resource> list = new ArrayList<>();
                                                if (mr.hasMedicationReference() && ! FhirUtil.bundleContainsReference(resourceWithBundle.getBundle(), mr.getMedicationReference())) {
                                                    try {
                                                        list.add(
                                                                fhirService.readByReference(fcc, FHIRStrategy.PATIENT, Medication.class, mr.getMedicationReference())
                                                        );
                                                    } catch (Exception e) {
                                                        logger.error("Error reading medication reference: " + mr.getMedicationReference().getReference(), e);
                                                        if (e instanceof AuthenticationException ae) throw ae;
                                                    }
                                                }

                                                if (mr.hasRequester() && mr.getRequester().hasReference() && ! FhirUtil.bundleContainsReference(resourceWithBundle.getBundle(), mr.getRequester().getReference())) {
                                                    try {
                                                        list.add(
                                                                fhirService.readByReference(fcc, FHIRStrategy.PATIENT, Practitioner.class, mr.getRequester().getReference())
                                                        );
                                                    } catch (Exception e) {
                                                        logger.error("Error reading requester reference: " + mr.getRequester().getReference(), e);
                                                        if (e instanceof AuthenticationException ae) throw ae;
                                                    }
                                                }
                                                return list;
                                            }
                                            return null;
                                        }
                                    })
                    )
            );
        }

        for (MedicationModel mm : list) {
            medicationFlagService.appendMedicationFlags(mm);
        }

        for (MedicationModel mm : list) {
            mm.setSourceEndpointName(e.getName());
            mm.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<ProcedureModel> buildProcedures(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Procedures for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<ProcedureModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.PROCEDURES, e)) {
            list.addAll(
                    rt.transformProcedures(
                            fhirService.search(fcc, qm.getStrategy(), doTokenReplacements(fcc.getCredentials().getPatientId(), qm.getQuery()))
                    )
            );
        }

        for (ProcedureModel pm : list) {
            pm.setSourceEndpointName(e.getName());
            pm.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<QuestionnaireResponseModel> buildQuestionnaireResponses(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Questionnaire Responses for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<QuestionnaireResponseModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.QUESTIONNAIRE_RESPONSES, e)) {
            list.addAll(
                    rt.transformQuestionnaireResponses(
                            fhirService.search(fcc, qm.getStrategy(), doTokenReplacements(fcc.getCredentials().getPatientId(), qm.getQuery()))
                    )
            );
        }

        for (QuestionnaireResponseModel qrm : list) {
            qrm.setSourceEndpointName(e.getName());
            qrm.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<ServiceRequestModel> buildServiceRequests(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Service Requests for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<ServiceRequestModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.SERVICE_REQUESTS, e)) {
            list.addAll(
                    rt.transformServiceRequests(
                            fhirService.search(fcc, qm.getStrategy(), doTokenReplacements(fcc.getCredentials().getPatientId(), qm.getQuery()))
                    )
            );
        }

        for (ServiceRequestModel sr : list) {
            sr.setSourceEndpointName(e.getName());
            sr.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<SocialHistoryModel> buildSocialHistories(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Social Histories for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<SocialHistoryModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.SOCIAL_HISTORIES, e)) {
            list.addAll(
                    rt.transformSocialHistories(
                            fhirService.search(fcc, qm.getStrategy(), doTokenReplacements(fcc.getCredentials().getPatientId(), qm.getQuery()))
                    )
            );
        }

        for (SocialHistoryModel sh : list) {
            sh.setSourceEndpointName(e.getName());
            sh.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<SurveyObservationModel> buildSurveyObservations(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Survey Observations for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<SurveyObservationModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.SURVEY_OBSERVATIONS, e)) {
            list.addAll(
                    rt.transformSurveyObservations(
                            fhirService.search(fcc, qm.getStrategy(), doTokenReplacements(fcc.getCredentials().getPatientId(), qm.getQuery()))
                    )
            );
        }

        for (SurveyObservationModel so : list) {
            so.setSourceEndpointName(e.getName());
            so.setSourceEndpointIss(e.getIss());
        }

        return list;
    }

    @Override
    public List<VitalsModel> buildVitals(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Vitals for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<VitalsModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.VITALS,e)) {
            list.addAll(
                    rt.transformVitals(
                            fhirService.search(fcc, qm.getStrategy(), doTokenReplacements(fcc.getCredentials().getPatientId(), qm.getQuery()))
                    )
            );
        }

        for (VitalsModel vm : list) {
            vm.setSourceEndpointName(e.getName());
            vm.setSourceEndpointIss(e.getIss());
        }

        return list;
    }


///////////////////////////////////////////////////////////////////////
/// private methods
///

    private static final DateFormat FHIR_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private String doTokenReplacements(String patientId, String fhirQuery) {
        if (fhirQuery == null) return null;

        fhirQuery = fhirQuery.replaceAll("\\{PATIENT}", patientId);

        if (fhirQuery.contains("{TWO_YEARS_AGO}")) {
            fhirQuery = fhirQuery.replaceAll("\\{TWO_YEARS_AGO}", getDateParamForXYearsAgo(2));
        }

        if (fhirQuery.contains("{THREE_YEARS_AGO}")) {
            fhirQuery = fhirQuery.replaceAll("\\{THREE_YEARS_AGO}", getDateParamForXYearsAgo(3));
        }

        if (fhirQuery.contains("{TEN_YEARS_AGO}")) {
            fhirQuery = fhirQuery.replaceAll("\\{TEN_YEARS_AGO}", getDateParamForXYearsAgo(10));
        }

        return fhirQuery;
    }

    private String getDateParamForXYearsAgo(int x) {
        return FHIR_DATE_FORMAT.format(getDateXYearsAgo(x));
    }

    private Date getDateXYearsAgo(int x) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.YEAR, -1 * x);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }
}
