package edu.ohsu.cmp.ecareplan.transform;

import edu.ohsu.cmp.ecareplan.model.dataset.*;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;

import java.util.List;

public interface ResourceTransformer {
    PatientModel transformPatient(Patient patient);
    List<AssessmentModel> transformAssessments(Bundle bundle);
    List<CarePlanModel> transformCarePlans(Bundle bundle);
    List<CareTeamModel> transformCareTeams(Bundle bundle);
    List<ClinicalNoteModel> transformClinicalNotes(Bundle bundle);
    List<ConditionModel> transformConditions(Bundle bundle);
    List<DiagnosticReportModel> transformDiagnosticReports(Bundle bundle);
    List<GoalModel> transformGoals(Bundle bundle);
    List<ImmunizationModel> transformImmunizations(Bundle bundle);
    List<InteractionModel> transformInteractions(Bundle bundle);
    List<MedicationModel> transformMedications(Bundle bundle);
    List<ProcedureModel> transformProcedures(Bundle bundle);
    List<ServiceRequestModel> transformServiceRequests(Bundle bundle);
    List<SocialHistoryModel> transformSocialHistories(Bundle bundle);
    List<SurveyObservationModel> transformSurveyObservations(Bundle bundle);
    List<TestModel> transformTests(Bundle bundle);
    List<VitalsModel> transformVitals(Bundle bundle);
}
