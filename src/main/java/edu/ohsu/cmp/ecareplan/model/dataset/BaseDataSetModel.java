package edu.ohsu.cmp.ecareplan.model.dataset;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.Provenance;

public abstract class BaseDataSetModel {
    @JsonIgnore
    private final IDomainResource sourceResource;

    protected final String id;

    @JsonIgnore
    private Provenance provenance;

    protected BaseDataSetModel(IDomainResource resource) {
        this.sourceResource = resource;
        this.id = resource.getId();
    }

    public IDomainResource getSourceResource() {
        return sourceResource;
    }

    public String getId() {
        return id;
    }

    public Provenance getProvenance() {
        return provenance;
    }

    public void setProvenance(Provenance provenance) {
        this.provenance = provenance;
    }
}
