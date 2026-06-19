package edu.ohsu.cmp.ecareplan.model.dataset;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Encounter;

import java.util.ArrayList;
import java.util.List;

public class EncounterModel extends BaseDataSetModel<Encounter> {
    private List<String> types;
    private String serviceType;
    private String period;
    private List<String> reasons;
    private List<String> participants;

    public EncounterModel(Encounter encounter) {
        super(encounter);

        if (encounter.hasType()) {
            for (CodeableConcept cc : encounter.getType()) {
                String type = getConceptNameFromCodeableConcept(cc);
                if (type != null) {
                    if (types == null) types = new ArrayList<>();
                    types.add(type);
                }
            }
        }

        if (encounter.hasServiceType()) {
            serviceType = getConceptNameFromCodeableConcept(encounter.getServiceType());
        }

        if (encounter.hasPeriod()) {
            period = formatPeriod(encounter.getPeriod());
        }

        if (encounter.hasReasonCode()) {
            reasons = getConceptNamesFromCodeableConcept(encounter.getReasonCode());

        } else if (encounter.hasReasonReference()) {
            reasons = getDisplayValuesFromReferences(encounter.getReasonReference());
        }

        if (encounter.hasParticipant()) {
            for (Encounter.EncounterParticipantComponent participant : encounter.getParticipant()) {
                if (participant.hasIndividual() && participant.getIndividual().hasDisplay()) {
                    if (participants == null) participants = new ArrayList<>();
                    participants.add(participant.getIndividual().getDisplay());
                }
            }
        }
    }

    public List<String> getTypes() {
        return types;
    }

    public String getDescription() {
        return types != null ?
                String.join(", ", types) :
                "(No description)";
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getPeriod() {
        return period;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public List<String> getParticipants() {
        return participants;
    }
}
