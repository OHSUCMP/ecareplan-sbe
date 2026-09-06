package edu.ohsu.cmp.ecareplan.model.dataset;

import java.util.Date;

public interface Consolidatable {
    String getConsolidationGroupBy();
    Date getConsolidationSortBy();
}
