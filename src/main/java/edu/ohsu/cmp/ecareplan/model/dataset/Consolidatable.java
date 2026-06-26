package edu.ohsu.cmp.ecareplan.model.dataset;

public interface Consolidatable<S extends Comparable<S>> {
    String getConsolidationKey();
    S getConsolidationSortBy();
}
