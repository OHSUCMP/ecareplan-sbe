package edu.ohsu.cmp.ecareplan.model.dataset;

import org.hl7.fhir.r4.model.Observation;

public class SocialHistoryModel extends ObservationModel {
    public SocialHistoryModel(Observation observation) {
        super(observation);
    }

    @Override
    public Observation toResourceForSDSExport() {
        return sourceResource;
    }
}
