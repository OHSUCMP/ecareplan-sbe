package edu.ohsu.cmp.ecareplan.model.dataset;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CareTeam;

import java.util.ArrayList;
import java.util.List;

public class CareTeamModel extends BaseDataSetModel<CareTeam> {
    private String name;
    private String category;
    private String period;
    private List<ParticipantInfo> participants;

    public CareTeamModel(CareTeam careTeam) {
        super(careTeam);

        if (careTeam.hasName()) {
            name = careTeam.getName();
        }

        if (careTeam.hasCategory()) {
            category = getConceptNameFromCodeableConcept(careTeam.getCategoryFirstRep());
        }

        if (careTeam.hasPeriod()) {
            period = formatPeriod(careTeam.getPeriod());
        }

        if (careTeam.hasParticipant()) {
            for (CareTeam.CareTeamParticipantComponent participant : careTeam.getParticipant()) {
                if (participant.hasMember() && participant.getMember().hasDisplay()) {
                    String name = participant.getMember().getDisplay();;
                    String role = participant.hasRole() ?
                            getConceptNameFromCodeableConcept(participant.getRoleFirstRep()) :
                            null;
                    String period = participant.hasPeriod() ?
                            formatPeriod(participant.getPeriod()) :
                            null;
                    if (participants == null) participants = new ArrayList<>();
                    participants.add(new ParticipantInfo(name, role, period));
                }
            }
        }
    }

    @Override
    public CareTeam toResourceForSDSExport() {
        return sourceResource;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        if (StringUtils.isNotBlank(name)) {
            return name;
        } else if (StringUtils.isNotBlank(category)) {
            return category;
        } else {
            return "(No description)";
        }
    }

    public String getPeriod() {
        return period;
    }

    public List<ParticipantInfo> getParticipants() {
        return participants;
    }

    public static final class ParticipantInfo {
        private final String name;
        private final String role;
        private final String period;

        public ParticipantInfo(String name, String role, String period) {
            this.name = name;
            this.role = role;
            this.period = period;
        }

        public String getName() {
            return name;
        }

        public String getRole() {
            return role;
        }

        public String getPeriod() {
            return period;
        }
    }
}
