package edu.ohsu.cmp.ecareplan.model.dataset;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.Coding;
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
}
