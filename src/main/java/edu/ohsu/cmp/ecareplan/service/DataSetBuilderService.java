package edu.ohsu.cmp.ecareplan.service;

import edu.ohsu.cmp.ecareplan.entity.Endpoint;
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

    public PatientModel buildPatient(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Patient for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());

        // note : we don't store the Patient "query" in the database as we do with everything else, since we will always
        //        read the Patient resource directly by reference.  this is so standard that we're able to safely hardcode it

        return rt.transformPatient(
                fhirService.readByReference(fcc, FHIRStrategy.PATIENT, Patient.class, "Patient/" + fcc.getCredentials().getPatientId())
        );
    }

    public List<AssessmentModel> buildAssessments(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Assessments for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<AssessmentModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.ASSESSMENTS, e)) {
            list.addAll(
                    rt.transformAssessments(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<CarePlanModel> buildCarePlans(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Care Plans for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<CarePlanModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.CARE_PLANS, e)) {
            list.addAll(
                    rt.transformCarePlans(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<CareTeamModel> buildCareTeams(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Care Teams for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<CareTeamModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.CARE_TEAMS, e)) {
            list.addAll(
                    rt.transformCareTeams(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<ClinicalNoteModel> buildClinicalNotes(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Clinical Notes for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<ClinicalNoteModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.CLINICAL_NOTES, e)) {

            // todo : this needs to be augmented to read and integrate referenced Binary resources

            list.addAll(
                    rt.transformClinicalNotes(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<ConcernModel> buildConcerns(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Concerns for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<ConcernModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.CONCERNS, e)) {
            list.addAll(
                    rt.transformConcerns(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<DiagnosticReportModel> buildDiagnosticReports(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Diagnostic Reports for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<DiagnosticReportModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.DIAGNOSTIC_REPORTS, e)) {
            list.addAll(
                    rt.transformDiagnosticReports(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<GoalModel> buildGoals(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Goals for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<GoalModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.GOALS, e)) {
            list.addAll(
                    rt.transformGoals(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<ImmunizationModel> buildImmunizations(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Immunizations for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<ImmunizationModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.IMMUNIZATIONS, e)) {
            list.addAll(
                    rt.transformImmunizations(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<InteractionModel> buildInteractions(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Interactions for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<InteractionModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.INTERACTIONS, e)) {
            list.addAll(
                    rt.transformInteractions(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<MedicationModel> buildMedications(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Medications for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<MedicationModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.MEDICATIONS, e)) {

            // todo : this needs to be augmented to read and integrate referenced Medication resources

            list.addAll(
                    rt.transformMedications(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<ProcedureModel> buildProcedures(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Procedures for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<ProcedureModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.PROCEDURES, e)) {
            list.addAll(
                    rt.transformProcedures(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<ServiceRequestModel> buildServiceRequests(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Service Requests for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<ServiceRequestModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.SERVICE_REQUESTS, e)) {
            list.addAll(
                    rt.transformServiceRequests(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<SocialHistoryModel> buildSocialHistories(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Social Histories for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<SocialHistoryModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.SOCIAL_HISTORIES, e)) {
            list.addAll(
                    rt.transformSocialHistories(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<SurveyObservationModel> buildSurveyObservations(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Survey Observations for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<SurveyObservationModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.SURVEY_OBSERVATIONS, e)) {
            list.addAll(
                    rt.transformSurveyObservations(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<TestModel> buildTests(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Tests for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<TestModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.TESTS, e)) {
            list.addAll(
                    rt.transformTests(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }

    public List<VitalsModel> buildVitals(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        logger.info("building Vitals for session={}, user={}, endpoint={}", sessionId, workspace.getUserId(), e.getIss());
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(e);
        ResourceTransformer rt = workspace.getResourceTransformer(e.getProviderType());
        List<VitalsModel> list = new ArrayList<>();
        for (QueryModel qm : queryService.getDataSetQueriesForEndpoint(DataSetName.VITALS,e)) {
            list.addAll(
                    rt.transformVitals(
                            fhirService.search(fcc, qm.getStrategy(), qm.getQuery())
                    )
            );
        }
        return list;
    }
}