package edu.ohsu.cmp.ecareplan.model.dataset;

import org.hl7.fhir.r4.model.Condition;

public class ConcernModel extends BaseDataSetModel<Condition> {
    public ConcernModel(Condition condition) {
        super(condition);
    }
}
