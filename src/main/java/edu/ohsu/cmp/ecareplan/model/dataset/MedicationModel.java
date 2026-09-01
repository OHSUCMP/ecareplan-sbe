package edu.ohsu.cmp.ecareplan.model.dataset;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.ohsu.cmp.ecareplan.entity.MedicationFlag;
import edu.ohsu.cmp.ecareplan.util.CodeSystemUtil;
import edu.ohsu.cmp.ecareplan.util.FhirUtil;
import org.hl7.fhir.r4.model.*;

import java.util.*;

public class MedicationModel extends BaseDataSetModel<MedicationRequest> {
    @JsonIgnore
    private final Medication sourceMedication;

    @JsonIgnore
    private final Practitioner sourceRequester;

    private final String category;
    private final String status;
    private String conceptName;
    private Date authoredOn;
    private String requester;
    private String dosageInstruction;
    private Set<String> reasons;
    private List<String> notes;
    private String learnMore;               // complex; skip for now // todo : populate this
    private List<MedicationFlag> flags;

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
            reasons = getDistinctConceptNamesFromCodeableConcept(medicationRequest.getReasonCode());

        } else if (medicationRequest.hasReasonReference()) {
            reasons = getDistinctDisplayValuesFromReferences(medicationRequest.getReasonReference());
        }

        if (medicationRequest.hasNote()) {
            notes = buildNotes(medicationRequest.getNote());
        }
    }

    @Override
    public MedicationRequest toResourceForSDSExport() {
        // if MedicationRequest resource references a Medication, integrate information from the Medication into the
        // MedicationRequest resource
        if (sourceMedication != null && sourceMedication.hasCode()) {
            MedicationRequest medicationRequest = sourceResource.copy();

            if ( ! medicationRequest.hasMedicationCodeableConcept() ) {
                medicationRequest.setMedication(new CodeableConcept());
            }

            CodeableConcept cc = sourceMedication.getCode();
            if (cc.hasCoding()) {
                for (Coding coding : cc.getCoding()) {
                    if (!FhirUtil.hasCoding(medicationRequest.getMedicationCodeableConcept(), coding)) {
                        medicationRequest.getMedicationCodeableConcept().addCoding(coding);
                    }
                }
            }
            if (cc.hasText()) {
                if ( ! medicationRequest.getMedicationCodeableConcept().hasText() ) {
                    medicationRequest.getMedicationCodeableConcept().setText(cc.getText());
                }
            }

            return medicationRequest;
        }

        return sourceResource;
    }

    @JsonIgnore
    public List<Coding> getCodings() {
        return getCodings(null);
    }

    public List<Coding> getCodings(String system) {
        Map<String, Coding> map = new LinkedHashMap<>();

        if (sourceResource.hasMedicationCodeableConcept()) {
            for (Coding coding : sourceResource.getMedicationCodeableConcept().getCoding()) {
                if (system == null || (coding.hasSystem() && CodeSystemUtil.matches(system, coding.getSystem())) ) {
                    String key = coding.getSystem() + "|" + coding.getCode();
                    map.put(key, coding);
                }
            }
        }

        if (sourceMedication != null && sourceMedication.hasCode()) {
            for (Coding coding : sourceMedication.getCode().getCoding()) {
                if (system == null || (coding.hasSystem() && CodeSystemUtil.matches(system, coding.getSystem())) ) {
                    String key = coding.getSystem() + "|" + coding.getCode();
                    map.put(key, coding);
                }
            }
        }

        return new ArrayList<>(map.values());
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

    public Set<String> getReasons() {
        return reasons;
    }

    public List<String> getNotes() {
        return notes;
    }

    public String getLearnMore() {
        return learnMore;
    }

    public List<MedicationFlag> getFlags() {
        return flags;
    }

    public void setFlags(List<MedicationFlag> flags) {
        this.flags = flags;
    }
}
