package edu.ohsu.cmp.ecareplan.model.dataset;

import org.hl7.fhir.r4.model.Observation;

public class VitalsModel extends BaseDataSetModel {
    public VitalsModel(Observation observation) {
        super(observation);
    }
}
