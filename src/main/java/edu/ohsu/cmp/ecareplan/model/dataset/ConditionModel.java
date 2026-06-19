package edu.ohsu.cmp.ecareplan.model.dataset;

import org.hl7.fhir.r4.model.Condition;

public class ConditionModel extends BaseDataSetModel<Condition> {
    public ConditionModel(Condition condition) {
        super(condition);
    }
}
