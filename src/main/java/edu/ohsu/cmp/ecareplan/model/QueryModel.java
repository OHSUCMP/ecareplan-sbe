package edu.ohsu.cmp.ecareplan.model;

import edu.ohsu.cmp.ecareplan.entity.DefaultQuery;
import edu.ohsu.cmp.ecareplan.entity.EndpointQuery;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRStrategy;

public class QueryModel {
    private String dataSetName;
    private String query;
    private FHIRStrategy strategy;

    public QueryModel(DefaultQuery defaultQuery) {
        this.dataSetName = defaultQuery.getDataSet().getName();
        this.query = defaultQuery.getQuery();
        this.strategy = defaultQuery.getStrategy();
    }

    public QueryModel(EndpointQuery endpointQuery) {
        this.dataSetName = endpointQuery.getDataSet().getName();
        this.query = endpointQuery.getQuery();
        this.strategy = endpointQuery.getStrategy();
    }

    public String getDataSetName() {
        return dataSetName;
    }

    public String getQuery() {
        return query;
    }

    public FHIRStrategy getStrategy() {
        return strategy;
    }
}
