package edu.ohsu.cmp.ecareplan.model.dataset;

import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationRequest;

public class MedicationModel extends BaseDataSetModel {
    private Medication sourceMedication;

    public MedicationModel(MedicationRequest medicationRequest) {
        super(medicationRequest);
    }

    public Medication getSourceMedication() {
        return sourceMedication;
    }

    public void setSourceMedication(Medication sourceMedication) {
        this.sourceMedication = sourceMedication;
    }
}
