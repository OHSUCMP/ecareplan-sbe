package edu.ohsu.cmp.ecareplan.entity;

import edu.ohsu.cmp.ecareplan.model.fhir.FHIRStrategy;
import jakarta.persistence.*;

@Entity
@Table(name = "default_query")
public class DefaultQuery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String dataSetName;
    private String query;

    @Enumerated(EnumType.STRING)
    private FHIRStrategy strategy;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDataSetName() {
        return dataSetName;
    }

    public void setDataSetName(String dataSetName) {
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
}
