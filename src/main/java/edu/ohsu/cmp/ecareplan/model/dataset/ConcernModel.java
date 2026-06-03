package edu.ohsu.cmp.ecareplan.model.dataset;

import org.hl7.fhir.r4.model.Condition;

public class ConcernModel extends BaseModel {
    public ConcernModel(Condition condition) {
        super(condition);
    }
}
