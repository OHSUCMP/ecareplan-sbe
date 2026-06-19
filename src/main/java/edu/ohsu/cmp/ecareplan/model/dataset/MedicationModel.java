package edu.ohsu.cmp.ecareplan.model.dataset;

import edu.ohsu.cmp.ecareplan.util.FhirUtil;
import org.hl7.fhir.r4.model.*;

import java.util.Date;
import java.util.List;

public class MedicationModel extends BaseDataSetModel<MedicationRequest> {
    private Medication sourceMedication;
    private Practitioner sourceRequester;

    private String category;
    private String status;
    private String conceptName;
    private Date authoredOn;
    private String requester;
    private String dosageInstruction;
    private List<String> reasons;
    private List<String> notes;
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
            reasons = getConceptNamesFromCodeableConcept(medicationRequest.getReasonCode());

        } else if (medicationRequest.hasReasonReference()) {
            reasons = getDisplayValuesFromReferences(medicationRequest.getReasonReference());
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
