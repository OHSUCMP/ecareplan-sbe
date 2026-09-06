package edu.ohsu.cmp.ecareplan.model.view;

import edu.ohsu.cmp.ecareplan.model.dataset.Consolidatable;
import edu.ohsu.cmp.ecareplan.model.dataset.ResultValue;

import java.util.Date;
import java.util.List;
import java.util.Set;

public interface IVitalsModel extends Consolidatable {
    String getId();
    String getConceptName();
    String getDescription();
    Date getEffectiveDate();
    String getResultText();
    ResultValue getResultValue();
    String getResultUnits();
    String getReferenceRange();
    String getInterpretation();
    Boolean getFlag();
    Set<String> getPerformers();
    List<String> getNotes();
    String getLearnMore();
    String getSourceEndpointName();
    String getSourceEndpointIss();
}
