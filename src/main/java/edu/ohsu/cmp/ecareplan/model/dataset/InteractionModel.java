package edu.ohsu.cmp.ecareplan.model.dataset;

import org.hl7.fhir.r4.model.Encounter;

public class InteractionModel extends BaseDataSetModel<Encounter> {
    public InteractionModel(Encounter encounter) {
        super(encounter);
    }
}
