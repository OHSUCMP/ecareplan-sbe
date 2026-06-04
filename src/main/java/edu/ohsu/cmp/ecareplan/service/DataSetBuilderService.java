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
public class DataSetBuilderService extends BaseService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private FHIRService fhirService;

    @Autowired
    private QueryService queryService;

    public PatientModel buildPatient(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
        logger.info("building Patient for session={}, user={}, endpoint={}", sessionId, ue.getUserId(), ue.getEndpoint().getIss());
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient(ue);
        ResourceTransformer rt = workspace.getResourceTransformer(ue.getEndpoint().getProviderType());

        // note : we don't store the Patient "query" in the database as we do with everything else, since we will always
        //        read the Patient resource directly by reference.  this is so standard that we're able to safely hardcode it

        return rt.transformPatient(
                fhirService.readByReference(fcc, FHIRStrategy.PATIENT, Patient.class, "Patient/" + fcc.getCredentials().getPatientId())
        );
    }

    public List<AssessmentModel> buildAssessments(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
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

    public List<CarePlanModel> buildCarePlans(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
        logger.info("building Care Plans for session={}, user={}, endpoint={}", sessionId, ue.getUserId(), ue.getEndpoint().getIss());
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient(ue);
        ResourceTransformer rt = workspace.getResourceTransformer(ue.getEndpoint().getProviderType());
        List<CarePlanModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.CARE_PLANS, ue.getEndpoint().getId())) {
            list.addAll(
                    rt.transformCarePlans(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<CareTeamModel> buildCareTeams(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
        logger.info("building Care Teams for session={}, user={}, endpoint={}", sessionId, ue.getUserId(), ue.getEndpoint().getIss());
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient(ue);
        ResourceTransformer rt = workspace.getResourceTransformer(ue.getEndpoint().getProviderType());
        List<CareTeamModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.CARE_TEAMS, ue.getEndpoint().getId())) {
            list.addAll(
                    rt.transformCareTeams(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<ClinicalNoteModel> buildClinicalNotes(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
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

    public List<ConcernModel> buildConcerns(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
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

    public List<DiagnosticReportModel> buildDiagnosticReports(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
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

    public List<GoalModel> buildGoals(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
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

    public List<ImmunizationModel> buildImmunizations(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
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

    public List<InteractionModel> buildInteractions(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
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

    public List<MedicationModel> buildMedications(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
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

    public List<ProcedureModel> buildProcedures(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
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

    public List<ServiceRequestModel> buildServiceRequests(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
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

    public List<SocialHistoryModel> buildSocialHistories(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
        logger.info("building Social Histories for session={}, user={}, endpoint={}", sessionId, ue.getUserId(), ue.getEndpoint().getIss());
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getFhirCredentialsWithClient(ue);
        ResourceTransformer rt = workspace.getResourceTransformer(ue.getEndpoint().getProviderType());
        List<SocialHistoryModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.SOCIAL_HISTORIES, ue.getEndpoint().getId())) {
            list.addAll(
                    rt.transformSocialHistories(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<SurveyObservationModel> buildSurveyObservations(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
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

    public List<TestModel> buildTests(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
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

    public List<VitalsModel> buildVitals(String sessionId, UserEndpoint ue) throws DataException, ConfigurationException, IOException {
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