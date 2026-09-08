package edu.ohsu.cmp.ecareplan.service;

import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.entity.User;
import edu.ohsu.cmp.ecareplan.entity.UserEndpoint;
import edu.ohsu.cmp.ecareplan.exception.ConfigurationException;
import edu.ohsu.cmp.ecareplan.exception.DataException;
import edu.ohsu.cmp.ecareplan.model.EndpointModel;
import edu.ohsu.cmp.ecareplan.model.QueryModel;
import edu.ohsu.cmp.ecareplan.model.dataset.*;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentials;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRStrategy;
import edu.ohsu.cmp.ecareplan.model.fhir.ResourceWithBundle;
import edu.ohsu.cmp.ecareplan.repository.EndpointRepository;
import edu.ohsu.cmp.ecareplan.repository.UserEndpointRepository;
import edu.ohsu.cmp.ecareplan.transform.ResourceTransformer;
import edu.ohsu.cmp.ecareplan.util.CryptoUtil;
import edu.ohsu.cmp.ecareplan.util.FhirUtil;
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
public class EndpointService extends BaseDataSetBuilderService implements IDataSetBuilder {
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

    @Override
    protected ServerValidationModeEnum getServerValidationMode() {
        return ServerValidationModeEnum.ONCE;
    }

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
        ue.setUser(user);
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
    public List<PatientModel> buildPatients(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final FHIRCredentials fc = cfg.credentials();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final FHIRCredentialsWithClient fcc = buildCredentialsWithClient(fc);

        logger.info("building Patient for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        // note : we don't store the Patient "query" in the database as we do with everything else, since we will always
        //        read the Patient resource directly by reference.  this is so standard that we're able to safely hardcode it

        PatientModel patientModel = rt.transformPatient(
                fhirService.readByReference(fcc, FHIRStrategy.PATIENT, Patient.class, "Patient/" + fcc.getCredentials().getPatientId())
        );

        patientModel.setSourceEndpointName(ue.getEndpoint().getName());
        patientModel.setSourceEndpointIss(ue.getEndpoint().getIss());

        return List.of(patientModel);
    }

    @Override
    public List<CarePlanModel> buildCarePlans(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final FHIRCredentials fc = cfg.credentials();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final FHIRCredentialsWithClient fcc = buildCredentialsWithClient(fc);

        logger.info("building Care Plans for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        List<CarePlanModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.CARE_PLANS, ue.getEndpoint())) {
            list.addAll(
                    rt.transformCarePlans(
                            fhirService.search(fcc, qm.getStrategy(), doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()))
                    )
            );
        }

        for (CarePlanModel cp : list) {
            cp.setSourceEndpointName(ue.getEndpoint().getName());
            cp.setSourceEndpointIss(ue.getEndpoint().getIss());
        }

        return list;
    }

    @Override
    public List<CareTeamModel> buildCareTeams(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final FHIRCredentials fc = cfg.credentials();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final FHIRCredentialsWithClient fcc = buildCredentialsWithClient(fc);

        logger.info("building Care Teams for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        List<CareTeamModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.CARE_TEAMS, ue.getEndpoint())) {
            list.addAll(
                    rt.transformCareTeams(
                            fhirService.search(fcc, qm.getStrategy(), doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()))
                    )
            );
        }

        for (CareTeamModel ct : list) {
            ct.setSourceEndpointName(ue.getEndpoint().getName());
            ct.setSourceEndpointIss(ue.getEndpoint().getIss());
        }

        return list;
    }

    @Override
    public List<ClinicalNoteModel> buildClinicalNotes(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final FHIRCredentials fc = cfg.credentials();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final FHIRCredentialsWithClient fcc = buildCredentialsWithClient(fc);

        logger.info("building Clinical Notes for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        List<ClinicalNoteModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.CLINICAL_NOTES, ue.getEndpoint())) {
            list.addAll(
                    rt.transformClinicalNotes(
                            fhirService.search(fcc, qm.getStrategy(),
                                    doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()),
                                    null,
                                    (Function<ResourceWithBundle, List<Resource>>) resourceWithBundle -> {
                                        if (resourceWithBundle.getResource() instanceof DocumentReference dr) {
                                            List<Resource> list1 = new ArrayList<>();
                                            if (dr.hasContent()) {
                                                for (DocumentReference.DocumentReferenceContentComponent content : dr.getContent()) {
                                                    if (content.hasAttachment() && content.getAttachment().hasUrl()) {
                                                        if (!FhirUtil.bundleContainsReference(resourceWithBundle.getBundle(), content.getAttachment().getUrl())) {
                                                            try {
                                                                list1.add(
                                                                        fhirService.readByReference(fcc, FHIRStrategy.PATIENT, Binary.class, content.getAttachment().getUrl())
                                                                );
                                                            } catch (Exception e) {
                                                                logger.error("Error reading binary reference: " + content.getAttachment().getUrl(), e);
                                                                if (e instanceof AuthenticationException ae)
                                                                    throw ae;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            return list1;
                                        }
                                        return null;
                                    })
                    )
            );
        }

        for (ClinicalNoteModel cn : list) {
            cn.setSourceEndpointName(ue.getEndpoint().getName());
            cn.setSourceEndpointIss(ue.getEndpoint().getIss());
        }

        return list;
    }

    @Override
    public List<ConditionModel> buildConditions(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final FHIRCredentials fc = cfg.credentials();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final FHIRCredentialsWithClient fcc = buildCredentialsWithClient(fc);

        logger.info("building Conditions for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        List<ConditionModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.CONDITIONS, ue.getEndpoint())) {
            list.addAll(
                    rt.transformConditions(
                            fhirService.search(fcc, qm.getStrategy(), doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()))
                    )
            );
        }

        for (ConditionModel cm : list) {
            cm.setSourceEndpointName(ue.getEndpoint().getName());
            cm.setSourceEndpointIss(ue.getEndpoint().getIss());
        }

        return list;
    }

    @Override
    public List<DiagnosticReportModel> buildDiagnosticReports(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final FHIRCredentials fc = cfg.credentials();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final FHIRCredentialsWithClient fcc = buildCredentialsWithClient(fc);

        logger.info("building Diagnostic Reports for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        List<DiagnosticReportModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.DIAGNOSTIC_REPORTS, ue.getEndpoint())) {
            list.addAll(
                    rt.transformDiagnosticReports(
                            fhirService.search(fcc, qm.getStrategy(), doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()))
                    )
            );
        }

        for (DiagnosticReportModel dr : list) {
            dr.setSourceEndpointName(ue.getEndpoint().getName());
            dr.setSourceEndpointIss(ue.getEndpoint().getIss());
        }

        return list;
    }

    @Override
    public List<EncounterModel> buildEncounters(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final FHIRCredentials fc = cfg.credentials();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final FHIRCredentialsWithClient fcc = buildCredentialsWithClient(fc);

        logger.info("building Encounters for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        List<EncounterModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.ENCOUNTERS, ue.getEndpoint())) {
            list.addAll(
                    rt.transformEncounters(
                            fhirService.search(fcc, qm.getStrategy(), doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()))
                    )
            );
        }

        for (EncounterModel em : list) {
            em.setSourceEndpointName(ue.getEndpoint().getName());
            em.setSourceEndpointIss(ue.getEndpoint().getIss());
        }

        return list;
    }

    @Override
    public List<GoalModel> buildGoals(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final FHIRCredentials fc = cfg.credentials();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final FHIRCredentialsWithClient fcc = buildCredentialsWithClient(fc);

        logger.info("building Goals for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        List<GoalModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.GOALS, ue.getEndpoint())) {
            list.addAll(
                    rt.transformGoals(
                            fhirService.search(fcc, qm.getStrategy(), doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()))
                    )
            );
        }

        for (GoalModel gm : list) {
            gm.setSourceEndpointName(ue.getEndpoint().getName());
            gm.setSourceEndpointIss(ue.getEndpoint().getIss());
        }

        return list;
    }

    @Override
    public List<ImmunizationModel> buildImmunizations(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final FHIRCredentials fc = cfg.credentials();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final FHIRCredentialsWithClient fcc = buildCredentialsWithClient(fc);

        logger.info("building Immunizations for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        List<ImmunizationModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.IMMUNIZATIONS, ue.getEndpoint())) {
            list.addAll(
                    rt.transformImmunizations(
                            fhirService.search(fcc, qm.getStrategy(), doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()))
                    )
            );
        }

        for (ImmunizationModel im : list) {
            im.setSourceEndpointName(ue.getEndpoint().getName());
            im.setSourceEndpointIss(ue.getEndpoint().getIss());
        }

        return list;
    }

    @Override
    public List<LabResultModel> buildLabResults(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final FHIRCredentials fc = cfg.credentials();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final FHIRCredentialsWithClient fcc = buildCredentialsWithClient(fc);

        logger.info("building Lab Results for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        List<LabResultModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.LAB_RESULTS, ue.getEndpoint())) {
            list.addAll(
                    rt.transformLabResults(
                            fhirService.search(fcc, qm.getStrategy(), doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()))
                    )
            );
        }

        for (LabResultModel tm : list) {
            tm.setSourceEndpointName(ue.getEndpoint().getName());
            tm.setSourceEndpointIss(ue.getEndpoint().getIss());
        }

        return list;
    }

    @Override
    public List<MedicationModel> buildMedications(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final FHIRCredentials fc = cfg.credentials();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final FHIRCredentialsWithClient fcc = buildCredentialsWithClient(fc);

        logger.info("building Medications for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        List<MedicationModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.MEDICATIONS, ue.getEndpoint())) {
            list.addAll(
                    rt.transformMedications(
                            fhirService.search(fcc, qm.getStrategy(),
                                    doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()),
                                    null,
                                    (Function<ResourceWithBundle, List<Resource>>) resourceWithBundle -> {
                                        if (resourceWithBundle.getResource() instanceof MedicationRequest mr) {
                                            List<Resource> list1 = new ArrayList<>();
                                            if (mr.hasMedicationReference() && !FhirUtil.bundleContainsReference(resourceWithBundle.getBundle(), mr.getMedicationReference())) {
                                                try {
                                                    list1.add(
                                                            fhirService.readByReference(fcc, FHIRStrategy.PATIENT, Medication.class, mr.getMedicationReference())
                                                    );
                                                } catch (Exception e) {
                                                    logger.error("Error reading medication reference: " + mr.getMedicationReference().getReference(), e);
                                                    if (e instanceof AuthenticationException ae) throw ae;
                                                }
                                            }

                                            if (mr.hasRequester() && mr.getRequester().hasReference() && !FhirUtil.bundleContainsReference(resourceWithBundle.getBundle(), mr.getRequester().getReference())) {
                                                try {
                                                    list1.add(
                                                            fhirService.readByReference(fcc, FHIRStrategy.PATIENT, Practitioner.class, mr.getRequester().getReference())
                                                    );
                                                } catch (Exception e) {
                                                    logger.error("Error reading requester reference: " + mr.getRequester().getReference(), e);
                                                    if (e instanceof AuthenticationException ae) throw ae;
                                                }
                                            }
                                            return list1;
                                        }
                                        return null;
                                    })
                    )
            );
        }

        for (MedicationModel mm : list) {
            medicationFlagService.appendMedicationFlags(mm);
        }

        for (MedicationModel mm : list) {
            mm.setSourceEndpointName(ue.getEndpoint().getName());
            mm.setSourceEndpointIss(ue.getEndpoint().getIss());
        }

        return list;
    }

    @Override
    public List<ProcedureModel> buildProcedures(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final FHIRCredentials fc = cfg.credentials();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final FHIRCredentialsWithClient fcc = buildCredentialsWithClient(fc);

        logger.info("building Procedures for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        List<ProcedureModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.PROCEDURES, ue.getEndpoint())) {
            list.addAll(
                    rt.transformProcedures(
                            fhirService.search(fcc, qm.getStrategy(), doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()))
                    )
            );
        }

        for (ProcedureModel pm : list) {
            pm.setSourceEndpointName(ue.getEndpoint().getName());
            pm.setSourceEndpointIss(ue.getEndpoint().getIss());
        }

        return list;
    }

    @Override
    public List<QuestionnaireResponseModel> buildQuestionnaireResponses(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final FHIRCredentials fc = cfg.credentials();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final FHIRCredentialsWithClient fcc = buildCredentialsWithClient(fc);

        logger.info("building Questionnaire Responses for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        List<QuestionnaireResponseModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.QUESTIONNAIRE_RESPONSES, ue.getEndpoint())) {
            list.addAll(
                    rt.transformQuestionnaireResponses(
                            fhirService.search(fcc, qm.getStrategy(), doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()))
                    )
            );
        }

        for (QuestionnaireResponseModel qrm : list) {
            qrm.setSourceEndpointName(ue.getEndpoint().getName());
            qrm.setSourceEndpointIss(ue.getEndpoint().getIss());
        }

        return list;
    }

    @Override
    public List<ServiceRequestModel> buildServiceRequests(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final FHIRCredentials fc = cfg.credentials();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final FHIRCredentialsWithClient fcc = buildCredentialsWithClient(fc);

        logger.info("building Service Requests for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        List<ServiceRequestModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.SERVICE_REQUESTS, ue.getEndpoint())) {
            list.addAll(
                    rt.transformServiceRequests(
                            fhirService.search(fcc, qm.getStrategy(), doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()))
                    )
            );
        }

        for (ServiceRequestModel sr : list) {
            sr.setSourceEndpointName(ue.getEndpoint().getName());
            sr.setSourceEndpointIss(ue.getEndpoint().getIss());
        }

        return list;
    }

    @Override
    public List<SocialHistoryModel> buildSocialHistories(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final FHIRCredentials fc = cfg.credentials();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final FHIRCredentialsWithClient fcc = buildCredentialsWithClient(fc);

        logger.info("building Social Histories for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        List<SocialHistoryModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.SOCIAL_HISTORIES, ue.getEndpoint())) {
            list.addAll(
                    rt.transformSocialHistories(
                            fhirService.search(fcc, qm.getStrategy(), doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()))
                    )
            );
        }

        for (SocialHistoryModel sh : list) {
            sh.setSourceEndpointName(ue.getEndpoint().getName());
            sh.setSourceEndpointIss(ue.getEndpoint().getIss());
        }

        return list;
    }

    @Override
    public List<SurveyObservationModel> buildSurveyObservations(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final FHIRCredentials fc = cfg.credentials();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final FHIRCredentialsWithClient fcc = buildCredentialsWithClient(fc);

        logger.info("building Survey Observations for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        List<SurveyObservationModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.SURVEY_OBSERVATIONS, ue.getEndpoint())) {
            list.addAll(
                    rt.transformSurveyObservations(
                            fhirService.search(fcc, qm.getStrategy(), doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()))
                    )
            );
        }

        for (SurveyObservationModel so : list) {
            so.setSourceEndpointName(ue.getEndpoint().getName());
            so.setSourceEndpointIss(ue.getEndpoint().getIss());
        }

        return list;
    }

    @Override
    public List<VitalsModel> buildVitals(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException {
        final UserEndpoint ue = cfg.userEndpoint();
        final FHIRCredentials fc = cfg.credentials();
        final ResourceTransformer rt = getResourceTransformer(ue.getEndpoint().getProviderType());
        final FHIRCredentialsWithClient fcc = buildCredentialsWithClient(fc);

        logger.info("building Vitals for user={}, endpoint={}", ue.getUser().getId(), ue.getEndpoint().getIss());

        List<VitalsModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSet.VITALS,ue.getEndpoint())) {
            list.addAll(
                    rt.transformVitals(
                            fhirService.search(fcc, qm.getStrategy(), doTokenReplacements(cfg.endpointPatientId(), qm.getQuery()))
                    )
            );
        }

        for (VitalsModel vm : list) {
            vm.setSourceEndpointName(ue.getEndpoint().getName());
            vm.setSourceEndpointIss(ue.getEndpoint().getIss());
        }

        return list;
    }


///////////////////////////////////////////////////////////////////////
/// private methods
///

    private FHIRCredentialsWithClient buildCredentialsWithClient(FHIRCredentials fc) {
        return new FHIRCredentialsWithClient(fc, FhirUtil.buildClient(fhirContext, fc.getServerURL(), fc.getBearerToken()));
    }

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
