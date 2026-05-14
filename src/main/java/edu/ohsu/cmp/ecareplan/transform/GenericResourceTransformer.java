package edu.ohsu.cmp.ecareplan.transform;

import edu.ohsu.cmp.ecareplan.model.*;
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
    public List<CareTeamModel> transformCareTeams(Bundle bundle) {
        return List.of();
    }

    @Override
    public List<ClinicalNoteModel> transformClinicalNotes(Bundle bundle) {
        return List.of();
    }

    @Override
    public List<GoalModel> transformGoals(Bundle bundle) {
        return List.of();
    }

    @Override
    public List<HealthConcernModel> transformHealthConcerns(Bundle bundle) {
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
    public List<LabTestModel> transformLabTests(Bundle bundle) {
        return List.of();
    }

    @Override
    public List<MedicationModel> transformMedications(Bundle bundle) {
        return List.of();
    }

    @Override
    public List<VitalsModel> transformVitals(Bundle bundle) {
        return List.of();
    }
}
