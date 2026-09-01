package edu.ohsu.cmp.ecareplan.model.dataset;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CareTeam;
import org.jspecify.annotations.NonNull;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

public class CareTeamModel extends BaseDataSetModel<CareTeam> {
    private String name;
    private String category;
    private String period;
    private Set<ParticipantInfo> participants;

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
                    if (participants == null) participants = new LinkedHashSet<>();
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

    public Set<ParticipantInfo> getParticipants() {
        return participants;
    }

    public record ParticipantInfo(String name, String role, String period) implements Comparable<ParticipantInfo> {
        private static final Comparator<String> STRING_COMPARATOR =
                Comparator.nullsLast(
                        String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder())
                );

        private static final Comparator<ParticipantInfo> COMPARATOR =
                Comparator.comparing(ParticipantInfo::name, STRING_COMPARATOR)
                        .thenComparing(ParticipantInfo::role, STRING_COMPARATOR)
                        .thenComparing(ParticipantInfo::period, STRING_COMPARATOR);

        @Override
        public int compareTo(@NonNull ParticipantInfo other) {
            return COMPARATOR.compare(this, other);
        }
    }
}
