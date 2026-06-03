package edu.ohsu.cmp.ecareplan.transform;

import edu.ohsu.cmp.ecareplan.model.dataset.*;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;

import java.util.List;

public class GenericResourceTransformer extends BaseResourceTransformer {
    @Override
    public PatientModel transformPatient(Patient patient) {
        return null;
    }

    @Override
    public List<AssessmentModel> transformAssessments(Bundle bundle) {
        return List.of();
    }

    @Override
    public List<CarePlanModel> transformCarePlans(Bundle bundle) {
        return List.of();
    }

    @Override
    public List<CareTeamModel> transformCareTeams(Bundle bundle) {
        return List.of();
    }

    @Override
    public List<ClinicalNoteModel> transformClinicalNotes(Bundle bundle) {
        return List.of();
    }

    @Override
    public List<ConcernModel> transformConcerns(Bundle bundle) {
        return List.of();
    }

    @Override
    public List<DiagnosticReportModel> transformDiagnosticReports(Bundle bundle) {
        return List.of();
    }

    @Override
    public List<GoalModel> transformGoals(Bundle bundle) {
        return List.of();
    }

    @Override
    public List<ImmunizationModel> transformImmunizations(Bundle bundle) {
        return List.of();
    }

    @Override
    public List<InteractionModel> transformInteractions(Bundle bundle) {
        return List.of();
    }

    @Override
    public List<MedicationModel> transformMedications(Bundle bundle) {
        return List.of();
    }

    @Override
    public List<ProcedureModel> transformProcedures(Bundle bundle) {
        return List.of();
    }

    @Override
    public List<ServiceRequestModel> transformServiceRequests(Bundle bundle) {
        return List.of();
    }

    @Override
    public List<SocialHistoryModel> transformSocialHistories(Bundle bundle) {
        return List.of();
    }

    @Override
    public List<SurveyObservationModel> transformSurveyObservations(Bundle bundle) {
        return List.of();
    }

    @Override
    public List<TestModel> transformTests(Bundle bundle) {
        return List.of();
    }

    @Override
    public List<VitalsModel> transformVitals(Bundle bundle) {
        return List.of();
    }
}
