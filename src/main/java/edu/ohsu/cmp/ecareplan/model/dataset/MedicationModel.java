package edu.ohsu.cmp.ecareplan.model.dataset;

import org.hl7.fhir.r4.model.Medication;
import org.hl7.fhir.r4.model.MedicationRequest;

import java.util.List;

public class MedicationModel extends BaseDataSetModel<MedicationRequest> {
    private Medication sourceMedication;

    private String category;                // based on valueset association
    private String status;                  // medreq.status
    private String conceptName;
    private String authoredOn;
    private String requester;
    private String dosageInstruction;
    private List<String> reasons;
    private List<String> notes;
    private String learnMore;
    private List<String> rxCui;
    private List<String> rxClass;
    private List<String> flags;

    public MedicationModel(MedicationRequest medicationRequest, Medication sourceMedication, String category) {
        super(medicationRequest);
        this.sourceMedication = sourceMedication;
        this.category = category;
    }

    public Medication getSourceMedication() {
        return sourceMedication;
    }


}
