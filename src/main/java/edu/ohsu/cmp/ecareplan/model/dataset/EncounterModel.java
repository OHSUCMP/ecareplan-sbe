package edu.ohsu.cmp.ecareplan.model.dataset;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Encounter;

import java.util.*;

public class EncounterModel extends BaseDataSetModel<Encounter> {
    private List<String> types;
    private String serviceType;
    private String period;
    private Date effectiveDate;
    private Set<String> reasons;
    private Set<String> participants;

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
            if (encounter.getPeriod().hasStart()) {
                effectiveDate = encounter.getPeriod().getStart();
            } else if (encounter.getPeriod().hasEnd()) {
                effectiveDate = encounter.getPeriod().getEnd();
            }
        }

        if (encounter.hasReasonCode()) {
            reasons = getDistinctConceptNamesFromCodeableConcept(encounter.getReasonCode());

        } else if (encounter.hasReasonReference()) {
            reasons = getDistinctDisplayValuesFromReferences(encounter.getReasonReference());
        }

        if (encounter.hasParticipant()) {
            for (Encounter.EncounterParticipantComponent participant : encounter.getParticipant()) {
                if (participant.hasIndividual() && participant.getIndividual().hasDisplay()) {
                    if (participants == null) participants = new LinkedHashSet<>();
                    participants.add(participant.getIndividual().getDisplay());
                }
            }
        }
    }

    @Override
    public Encounter toResourceForSDSExport() {
        return sourceResource;
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

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public Set<String> getReasons() {
        return reasons;
    }

    public Set<String> getParticipants() {
        return participants;
    }
}
