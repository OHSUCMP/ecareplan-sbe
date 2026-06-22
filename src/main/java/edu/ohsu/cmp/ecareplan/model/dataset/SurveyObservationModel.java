package edu.ohsu.cmp.ecareplan.model.dataset;

import org.hl7.fhir.r4.model.Observation;

public class SurveyObservationModel extends ObservationModel {
    public SurveyObservationModel(Observation observation) {
        super(observation);
    }

    @Override
    public Observation toResourceForSDSExport() {
        return sourceResource;
    }
}
