package edu.ohsu.cmp.ecareplan.entity;

import edu.ohsu.cmp.ecareplan.model.DataSetName;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRStrategy;
import jakarta.persistence.*;

import java.util.Date;

@Entity
@Table(name = "endpoint_query")
public class EndpointQuery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long endpointId;

    @Enumerated(EnumType.STRING)
    private DataSetName dataSetName;

    private String query;

    @Enumerated(EnumType.STRING)
    private FHIRStrategy strategy;

    private Date created;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEndpointId() {
        return endpointId;
    }

    public void setEndpointId(Long endpointId) {
        this.endpointId = endpointId;
    }

    public DataSetName getDataSetName() {
        return dataSetName;
    }

    public void setDataSetName(DataSetName dataSetName) {
        this.dataSetName = dataSetName;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public FHIRStrategy getStrategy() {
        return strategy;
    }

    public void setStrategy(FHIRStrategy strategy) {
        this.strategy = strategy;
    }

    public Date getCreated() {
        return created;
    }

    public void setCreated(Date created) {
        this.created = created;
    }
}
