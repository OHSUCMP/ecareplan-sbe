package edu.ohsu.cmp.ecareplan.model;

import edu.ohsu.cmp.ecareplan.entity.DefaultQuery;
import edu.ohsu.cmp.ecareplan.entity.EndpointQuery;

public class QueryModel {
    private String dataSetName;
    private String query;

    public QueryModel(DefaultQuery defaultQuery) {
        this.dataSetName = defaultQuery.getDataSet().getName();
        this.query = query;
    }

    public QueryModel(EndpointQuery endpointQuery) {
        this.dataSetName = endpointQuery.getDataSet().getName();
        this.query = query;
    }

    public String getDataSetName() {
        return dataSetName;
    }

    public String getQuery() {
        return query;
    }
}
