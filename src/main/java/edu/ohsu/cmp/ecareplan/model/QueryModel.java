package edu.ohsu.cmp.ecareplan.model;

import edu.ohsu.cmp.ecareplan.entity.DefaultQuery;
import edu.ohsu.cmp.ecareplan.entity.EndpointQuery;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSet;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRStrategy;

public class QueryModel {
    private DataSet<?> dataSet;
    private String query;
    private FHIRStrategy strategy;

    public QueryModel(DefaultQuery defaultQuery) {
        this.dataSet = DataSet.getDataSet(defaultQuery.getDataSetName());
        this.query = defaultQuery.getQuery();
        this.strategy = defaultQuery.getStrategy();
    }

    public QueryModel(EndpointQuery endpointQuery) {
        this.dataSet = DataSet.getDataSet(endpointQuery.getDataSetName());
        this.query = endpointQuery.getQuery();
        this.strategy = endpointQuery.getStrategy();
    }

    public DataSet<?> getDataSet() {
        return dataSet;
    }

    public String getQuery() {
        return query;
    }

    public FHIRStrategy getStrategy() {
        return strategy;
    }
}
