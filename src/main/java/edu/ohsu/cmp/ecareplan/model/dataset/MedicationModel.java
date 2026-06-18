package edu.ohsu.cmp.ecareplan.model.dataset;

import edu.ohsu.cmp.ecareplan.util.FhirUtil;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MedicationModel extends BaseDataSetModel<MedicationRequest> {
    private Medication sourceMedication;
    private Practitioner sourceRequester;

    private String category;                // based on valueset association
    private String status;                  // medreq.status
    private String conceptName;             // medreq.medication name from codeableconcept or reference
    private Date authoredOn;                // medreq.authoredOn
    private String requester;               // medreq.requester name from practitioner or reference
    private String dosageInstruction;       // medreq.dosageInstruction.patientInstruction
    private List<String> reasons;           // medreq.reasonCode.text or medreq.reasonReference.display
    private List<String> notes;             // medreq.note.text
    private String learnMore;               // complex; skip for now // todo : populate this
    private List<String> flags;             // todo : populate this

    public MedicationModel(MedicationRequest medicationRequest, Medication sourceMedication, Practitioner sourceRequester,
                           String category) {
        super(medicationRequest);
        this.sourceMedication = sourceMedication;
        this.sourceRequester = sourceRequester;
        this.category = category;

        status = medicationRequest.getStatus().getDisplay();

        if (medicationRequest.hasMedicationCodeableConcept()) {
            conceptName = getConceptNameFromCodeableConcept(medicationRequest.getMedicationCodeableConcept());

        } else if (medicationRequest.hasMedicationReference()) {
            Reference r = medicationRequest.getMedicationReference();
            if (r.hasDisplay()) {
                conceptName = r.getDisplay();
            } else if (r.hasReference() && sourceMedication != null && FhirUtil.references(r.getReference(), sourceMedication)) {
                if (sourceMedication.hasCode()) {
                    conceptName = getConceptNameFromCodeableConcept(sourceMedication.getCode());
                }
            }
        }

        if (medicationRequest.hasAuthoredOn()) {
            authoredOn = medicationRequest.getAuthoredOn();
        }

        if (medicationRequest.hasRequester()) {
            Reference r = medicationRequest.getRequester();
            if (r.hasReference() && sourceRequester != null && FhirUtil.references(r.getReference(), sourceRequester) &&
                    sourceRequester.hasName()) {
                requester = getPreferredName(sourceRequester.getName());

            } else if (r.hasDisplay()) {
                requester = r.getDisplay();
            }
        }

        if (medicationRequest.hasDosageInstruction()) {
            for (Dosage d : medicationRequest.getDosageInstruction()) {
                if (d.hasPatientInstruction()) {
                    dosageInstruction = d.getPatientInstruction();
                    break;
                }
            }
        }

        if (medicationRequest.hasReasonCode()) {
            for (CodeableConcept cc : medicationRequest.getReasonCode()) {
                String reason = getConceptNameFromCodeableConcept(cc);
                if (reason != null) {
                    if (reasons == null) reasons = new ArrayList<>();
                    reasons.add(reason);
                }
            }

        } else if (medicationRequest.hasReasonReference()) {
            medicationRequest.getReasonReference();
            for (Reference r : medicationRequest.getReasonReference()) {

                // todo : consider augmenting this part to go get the referenced resource.  might be overkill

                if (r.hasDisplay()) {
                    if (reasons == null) reasons = new ArrayList<>();
                    reasons.add(r.getDisplay());
                }
            }
        }

        if (medicationRequest.hasNote()) {
            notes = buildNotes(medicationRequest.getNote());
        }
    }

    public Medication getSourceMedication() {
        return sourceMedication;
    }

    public Practitioner getSourceRequester() {
        return sourceRequester;
    }

    public String getCategory() {
        return category;
    }

    public String getStatus() {
        return status;
    }

    public String getConceptName() {
        return conceptName;
    }

    public Date getAuthoredOn() {
        return authoredOn;
    }

    public String getRequester() {
        return requester;
    }

    public String getDosageInstruction() {
        return dosageInstruction;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public List<String> getNotes() {
        return notes;
    }

    public String getLearnMore() {
        return learnMore;
    }

    public List<String> getFlags() {
        return flags;
    }
}
