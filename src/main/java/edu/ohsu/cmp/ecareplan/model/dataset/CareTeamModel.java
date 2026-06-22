package edu.ohsu.cmp.ecareplan.model.dataset;

import org.hl7.fhir.r4.model.CareTeam;

public class CareTeamModel extends BaseDataSetModel<CareTeam> {
    public CareTeamModel(CareTeam careTeam) {
        super(careTeam);
    }

    @Override
    public CareTeam toResourceForSDSExport() {
        return sourceResource;
    }
}
