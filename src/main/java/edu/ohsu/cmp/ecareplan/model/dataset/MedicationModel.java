package edu.ohsu.cmp.ecareplan.model.dataset;

import edu.ohsu.cmp.ecareplan.util.CodeSystemUtil;
import org.apache.commons.lang3.Strings;
import org.hl7.fhir.r4.model.Coding;
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

    public MedicationModel(MedicationRequest medicationRequest, String category) {
        super(medicationRequest);


    }

    public Medication getSourceMedication() {
        return sourceMedication;
    }

    public void setSourceMedication(Medication sourceMedication) {
        this.sourceMedication = sourceMedication;
    }

    public boolean matches(String system, String code) {
        if (sourceResource.hasMedicationCodeableConcept()) {
            for (Coding c : sourceResource.getMedicationCodeableConcept().getCoding()) {
                if (CodeSystemUtil.matches(c.getSystem(), system) && Strings.CS.equals(c.getCode(), code)) {
                    return true;
                }
            }

        } else if (sourceResource.hasMedicationReference() && sourceMedication != null) {
            for (Coding c : sourceMedication.getCode().getCoding()) {
                if (CodeSystemUtil.matches(c.getSystem(), system) && Strings.CS.equals(c.getCode(), code)) {
                    return true;
                }
            }
        }

        return false;
    }
}
