package edu.ohsu.cmp.ecareplan.model.dataset;

import org.hl7.fhir.r4.model.Observation;

public class LabResultsModel extends BaseDataSetModel<Observation> {
    public LabResultsModel(Observation observation) {
        super(observation);
    }
}
