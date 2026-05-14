package edu.ohsu.cmp.ecareplan.transform;

import edu.ohsu.cmp.ecareplan.model.*;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Patient;

import java.util.List;

public interface ResourceTransformer {
    PatientModel transformPatient(Patient patient);
    List<AssessmentModel> transformAssessments(Bundle bundle);
    List<CareTeamModel> transformCareTeams(Bundle bundle);
    List<ClinicalNoteModel> transformClinicalNotes(Bundle bundle);
    List<GoalModel> transformGoals(Bundle bundle);
    List<HealthConcernModel> transformHealthConcerns(Bundle bundle);
    List<ImmunizationModel> transformImmunizations(Bundle bundle);
    List<InteractionModel> transformInteractions(Bundle bundle);
    List<LabTestModel> transformLabTests(Bundle bundle);
    List<MedicationModel> transformMedications(Bundle bundle);
    List<VitalsModel> transformVitals(Bundle bundle);
}
