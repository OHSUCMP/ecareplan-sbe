package edu.ohsu.cmp.ecareplan.model.dataset;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.Provenance;

public abstract class BaseDataSetModel {
    @JsonIgnore
    private IDomainResource sourceResource;

    @JsonIgnore
    private Provenance provenance;

    private String id;

    protected BaseDataSetModel(IDomainResource resource) {
        this.sourceResource = resource;
        this.id = resource.getId();
    }

    public String getId() {
        return id;
    }

    public IDomainResource getSourceResource() {
        return sourceResource;
    }

    public Provenance getProvenance() {
        return provenance;
    }

    public void setProvenance(Provenance provenance) {
        this.provenance = provenance;
    }
}
