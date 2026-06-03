package edu.ohsu.cmp.ecareplan.service;

import edu.ohsu.cmp.ecareplan.entity.UserEndpoint;
import edu.ohsu.cmp.ecareplan.exception.ConfigurationException;
import edu.ohsu.cmp.ecareplan.exception.DataException;
import edu.ohsu.cmp.ecareplan.model.QueryModel;
import edu.ohsu.cmp.ecareplan.model.dataset.*;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRStrategy;
import edu.ohsu.cmp.ecareplan.transform.ResourceTransformer;
import edu.ohsu.cmp.ecareplan.workspace.UserWorkspace;
import org.hl7.fhir.r4.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * DataSetService
 * This service is responsible for executing FHIR queries and returning either individual resources (e.g. Patient), or
 * Bundles of resources.  It handles filtering resources by modifier elements and other general requirements to ensure
 * that only "good" resources are included in results.  No other filtering takes place here.
 */
@Service
public class DataSetService extends BaseService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private FHIRService fhirService;

    @Autowired
    private QueryService queryService;

    public PatientModel getPatient(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
        logger.info("building Patient for session={}, user={}, endpoint={}", sessionId, ue.getUserId(), ue.getEndpoint().getIss());
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient(ue);
        ResourceTransformer rt = workspace.getResourceTransformer(ue.getEndpoint().getProviderType());
        return rt.transformPatient(
                fhirService.readByReference(fcc, FHIRStrategy.PATIENT, Patient.class, "Patient/" + fcc.getCredentials().getPatientId())
        );
    }

    public List<AssessmentModel> getAssessments(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
        logger.info("building Assessments for session={}, user={}, endpoint={}", sessionId, ue.getUserId(), ue.getEndpoint().getIss());
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient(ue);
        ResourceTransformer rt = workspace.getResourceTransformer(ue.getEndpoint().getProviderType());
        List<AssessmentModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.ASSESSMENTS, ue.getEndpoint().getId())) {
            list.addAll(
                    rt.transformAssessments(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<CarePlanModel> getCarePlan(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
        logger.info("building Care Plan for session={}, user={}, endpoint={}", sessionId, ue.getUserId(), ue.getEndpoint().getIss());
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient(ue);
        ResourceTransformer rt = workspace.getResourceTransformer(ue.getEndpoint().getProviderType());
        List<CarePlanModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.CARE_PLAN, ue.getEndpoint().getId())) {
            list.addAll(
                    rt.transformCarePlans(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<CareTeamModel> getCareTeam(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
        logger.info("building Care Team for session={}, user={}, endpoint={}", sessionId, ue.getUserId(), ue.getEndpoint().getIss());
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient(ue);
        ResourceTransformer rt = workspace.getResourceTransformer(ue.getEndpoint().getProviderType());
        List<CareTeamModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.CARE_TEAM, ue.getEndpoint().getId())) {
            list.addAll(
                    rt.transformCareTeams(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<ClinicalNoteModel> getClinicalNotes(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
        logger.info("building Clinical Notes for session={}, user={}, endpoint={}", sessionId, ue.getUserId(), ue.getEndpoint().getIss());
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient(ue);
        ResourceTransformer rt = workspace.getResourceTransformer(ue.getEndpoint().getProviderType());
        List<ClinicalNoteModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.CLINICAL_NOTES, ue.getEndpoint().getId())) {

            // todo : this needs to be augmented to read and integrate referenced Binary resources

            list.addAll(
                    rt.transformClinicalNotes(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<ConcernModel> getConcerns(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
        logger.info("building Concerns for session={}, user={}, endpoint={}", sessionId, ue.getUserId(), ue.getEndpoint().getIss());
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient(ue);
        ResourceTransformer rt = workspace.getResourceTransformer(ue.getEndpoint().getProviderType());
        List<ConcernModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.CONCERNS, ue.getEndpoint().getId())) {
            list.addAll(
                    rt.transformConcerns(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<DiagnosticReportModel> getDiagnosticReports(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
        logger.info("building Diagnostic Reports for session={}, user={}, endpoint={}", sessionId, ue.getUserId(), ue.getEndpoint().getIss());
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient(ue);
        ResourceTransformer rt = workspace.getResourceTransformer(ue.getEndpoint().getProviderType());
        List<DiagnosticReportModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.DIAGNOSTIC_REPORTS, ue.getEndpoint().getId())) {
            list.addAll(
                    rt.transformDiagnosticReports(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<GoalModel> getGoals(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
        logger.info("building Goals for session={}, user={}, endpoint={}", sessionId, ue.getUserId(), ue.getEndpoint().getIss());
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient(ue);
        ResourceTransformer rt = workspace.getResourceTransformer(ue.getEndpoint().getProviderType());
        List<GoalModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.GOALS, ue.getEndpoint().getId())) {
            list.addAll(
                    rt.transformGoals(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<ImmunizationModel> getImmunizations(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
        logger.info("building Immunizations for session={}, user={}, endpoint={}", sessionId, ue.getUserId(), ue.getEndpoint().getIss());
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient(ue);
        ResourceTransformer rt = workspace.getResourceTransformer(ue.getEndpoint().getProviderType());
        List<ImmunizationModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.IMMUNIZATIONS, ue.getEndpoint().getId())) {
            list.addAll(
                    rt.transformImmunizations(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<InteractionModel> getInteractions(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
        logger.info("building Interactions for session={}, user={}, endpoint={}", sessionId, ue.getUserId(), ue.getEndpoint().getIss());
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient(ue);
        ResourceTransformer rt = workspace.getResourceTransformer(ue.getEndpoint().getProviderType());
        List<InteractionModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.INTERACTIONS, ue.getEndpoint().getId())) {
            list.addAll(
                    rt.transformInteractions(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<MedicationModel> getMedications(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
        logger.info("building Medications for session={}, user={}, endpoint={}", sessionId, ue.getUserId(), ue.getEndpoint().getIss());
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient(ue);
        ResourceTransformer rt = workspace.getResourceTransformer(ue.getEndpoint().getProviderType());
        List<MedicationModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.MEDICATIONS, ue.getEndpoint().getId())) {

            // todo : this needs to be augmented to read and integrate referenced Medication resources

            list.addAll(
                    rt.transformMedications(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<ProcedureModel> getProcedures(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
        logger.info("building Procedures for session={}, user={}, endpoint={}", sessionId, ue.getUserId(), ue.getEndpoint().getIss());
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient(ue);
        ResourceTransformer rt = workspace.getResourceTransformer(ue.getEndpoint().getProviderType());
        List<ProcedureModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.PROCEDURES, ue.getEndpoint().getId())) {
            list.addAll(
                    rt.transformProcedures(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<ServiceRequestModel> getServiceRequests(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
        logger.info("building Service Requests for session={}, user={}, endpoint={}", sessionId, ue.getUserId(), ue.getEndpoint().getIss());
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient(ue);
        ResourceTransformer rt = workspace.getResourceTransformer(ue.getEndpoint().getProviderType());
        List<ServiceRequestModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.SERVICE_REQUESTS, ue.getEndpoint().getId())) {
            list.addAll(
                    rt.transformServiceRequests(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<SocialHistoryModel> getSocialHistory(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
        logger.info("building Social History for session={}, user={}, endpoint={}", sessionId, ue.getUserId(), ue.getEndpoint().getIss());
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient(ue);
        ResourceTransformer rt = workspace.getResourceTransformer(ue.getEndpoint().getProviderType());
        List<SocialHistoryModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.SOCIAL_HISTORY, ue.getEndpoint().getId())) {
            list.addAll(
                    rt.transformSocialHistories(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<SurveyObservationModel> getSurveyObservations(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
        logger.info("building Survey Observations for session={}, user={}, endpoint={}", sessionId, ue.getUserId(), ue.getEndpoint().getIss());
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient(ue);
        ResourceTransformer rt = workspace.getResourceTransformer(ue.getEndpoint().getProviderType());
        List<SurveyObservationModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.SURVEY_OBSERVATIONS, ue.getEndpoint().getId())) {
            list.addAll(
                    rt.transformSurveyObservations(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<TestModel> getTests(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
        logger.info("building Tests for session={}, user={}, endpoint={}", sessionId, ue.getUserId(), ue.getEndpoint().getIss());
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient(ue);
        ResourceTransformer rt = workspace.getResourceTransformer(ue.getEndpoint().getProviderType());
        List<TestModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.TESTS, ue.getEndpoint().getId())) {
            list.addAll(
                    rt.transformTests(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<VitalsModel> getVitals(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
        logger.info("building Vitals for session={}, user={}, endpoint={}", sessionId, ue.getUserId(), ue.getEndpoint().getIss());
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient(ue);
        ResourceTransformer rt = workspace.getResourceTransformer(ue.getEndpoint().getProviderType());
        List<VitalsModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.VITALS, ue.getEndpoint().getId())) {
            list.addAll(
                    rt.transformVitals(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }
}