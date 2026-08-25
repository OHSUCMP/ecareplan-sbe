package edu.ohsu.cmp.ecareplan.model.view;

import edu.ohsu.cmp.ecareplan.model.dataset.Consolidatable;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public interface IVitalsModel extends Consolidatable<Date> {
    String getId();
    String getConceptName();
    String getDescription();
    Date getEffectiveDate();
    String getResultText();
    BigDecimal getResultValue();
    String getResultUnits();
    String getReferenceRange();
    String getInterpretation();
    Boolean getFlag();
    List<String> getPerformers();
    List<String> getNotes();
    String getLearnMore();
    String getSourceEndpointName();
    String getSourceEndpointIss();
}
