package edu.ohsu.cmp.ecareplan.model.dataset;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CareTeam;

import java.util.ArrayList;
import java.util.List;

public class CareTeamModel extends BaseDataSetModel<CareTeam> {
    private String name;
    private String period;
    private List<ParticipantInfo> participants;

    public CareTeamModel(CareTeam careTeam) {
        super(careTeam);

        if (careTeam.hasName()) {
            name = careTeam.getName();
        }

        if (careTeam.hasPeriod()) {
            period = formatPeriod(careTeam.getPeriod());
        }

        if (careTeam.hasParticipant()) {
            for (CareTeam.CareTeamParticipantComponent participant : careTeam.getParticipant()) {
                if (participant.hasMember() && participant.getMember().hasDisplay()) {
                    String name = participant.getMember().getDisplay();;
                    String role = null;
                    if (participant.hasRole()) {
                        role = getConceptNameFromCodeableConcept(participant.getRoleFirstRep());
                    }
                    String period = null;
                    if (participant.hasPeriod()) {
                        period = formatPeriod(participant.getPeriod());
                    }
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

    public String getDescription() {
        return StringUtils.isNotBlank(name) ?
                name :
                "(No description)";
    }

    public String getPeriod() {
        return period;
    }

    public List<ParticipantInfo> getParticipants() {
        return participants;
    }

    private static final class ParticipantInfo {
        private String name;
        private String role;
        private String period;

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
