package edu.ohsu.cmp.ecareplan.model.dataset;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Provenance;

public abstract class BaseDataSetModel<T extends IDomainResource> {
    @JsonIgnore
    protected final T sourceResource;

    protected final String id;

    private String sourceEndpointName;
    private String sourceEndpointIss;

    @JsonIgnore
    private Provenance provenance;

    protected BaseDataSetModel(T resource) {
        this.sourceResource = resource;
        this.id = resource.getId();
    }

    public T getSourceResource() {
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

    public String getProvenanceTransmitter() {
        return getProvenanceAgentWhoByCode("transmitter");
    }

    public String getProvenanceAuthor() {
        return getProvenanceAgentWhoByCode("author");
    }

    private String getProvenanceAgentWhoByCode(String code) {
        if (provenance != null && provenance.hasAgent()) {
            for (Provenance.ProvenanceAgentComponent agent : provenance.getAgent()) {
                if (agent.hasType() && agent.getType().hasCoding()) {
                    for (Coding c : agent.getType().getCoding()) {
                        if (c.hasCode() && c.getCode().equals(code)) {
                            if (agent.hasWho() && agent.getWho().hasDisplay()) {
                                return agent.getWho().getDisplay();
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public String getSourceEndpointName() {
        return sourceEndpointName;
    }

    public void setSourceEndpointName(String sourceEndpointName) {
        this.sourceEndpointName = sourceEndpointName;
    }

    public String getSourceEndpointIss() {
        return sourceEndpointIss;
    }

    public void setSourceEndpointIss(String sourceEndpointIss) {
        this.sourceEndpointIss = sourceEndpointIss;
    }
}
