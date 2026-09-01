package edu.ohsu.cmp.ecareplan.model.dataset;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.hl7.fhir.r4.model.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public abstract class BaseDataSetModel<T extends IDomainResource> {
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MMM d, yyyy");

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

    public abstract T toResourceForSDSExport();

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

    protected Set<String> getDistinctConceptNamesFromCodeableConcept(List<CodeableConcept> ccList) {
        Set<String> set = null;
        if (ccList != null) {
            for (CodeableConcept cc : ccList) {
                String name = getConceptNameFromCodeableConcept(cc);
                if (name != null) {
                    if (set == null) set = new LinkedHashSet<>();
                    set.add(name);
                }
            }
        }
        return set;
    }

    protected String getConceptNameFromCodeableConcept(CodeableConcept cc) {
        if (cc != null) {
            if (cc.hasText()) {
                return cc.getText();
            } else if (cc.hasCoding()) {
                for (Coding c : cc.getCoding()) {
                    if (c.hasDisplay()) {
                        return c.getDisplay();
                    }
                }
            }
        }
        return null;
    }

    protected Set<String> getDistinctDisplayValuesFromReferences(List<Reference> references) {
        Set<String> set = null;
        if (references != null) {
            for (Reference r : references) {
                if (r.hasDisplay()) {
                    if (set == null) set = new LinkedHashSet<>();
                    set.add(r.getDisplay());
                }
            }
        }
        return set;
    }

    protected String getPreferredName(List<HumanName> names) {
        if (names != null && ! names.isEmpty()) {
            HumanName n = getName(names, HumanName.NameUse.OFFICIAL);
            if (n != null) return n.getNameAsSingleString();

            n = getName(names, HumanName.NameUse.USUAL);
            if (n != null) return n.getNameAsSingleString();

            n = names.getFirst();
            if (n != null) return n.getNameAsSingleString();
        }
        return null;
    }

    private HumanName getName(List<HumanName> list, HumanName.NameUse use) {
        if (list != null) {
            for (HumanName n : list) {
                if (n.getUse() == use) return n;
            }
        }
        return null;
    }

    protected List<String> buildNotes(List<Annotation> notes) {
        if (notes == null) return null;
        List<String> list = new ArrayList<>();
        for (Annotation note : notes) {
            if (note.hasText()) {
                list.add(note.getText());
            }
        }
        return list;
    }

    protected String formatPeriod(Period period) {
        if (period == null) return null;

        if (period.hasStart() && period.hasEnd()) {
            return DATE_FORMAT.format(period.getStart()) + " - " + DATE_FORMAT.format(period.getEnd());

        } else if (period.hasStart()) {
            return "Began " + DATE_FORMAT.format(period.getStart());

        } else if (period.hasEnd()) {
            return "Ended " + DATE_FORMAT.format(period.getEnd());
        }

        return null;
    }
}
