package edu.ohsu.cmp.ecareplan.model.dataset;

import org.hl7.fhir.r4.model.Immunization;

import java.util.Date;
import java.util.List;

public class ImmunizationModel extends BaseDataSetModel<Immunization> {
    private String description;
    private Date effectiveDate;
    private String location;
    private List<String> notes;

    public ImmunizationModel(Immunization immunization) {
        super(immunization);

        if (immunization.hasVaccineCode()) {
            description = getConceptNameFromCodeableConcept(immunization.getVaccineCode());
        }

        if (immunization.hasOccurrenceDateTimeType()) {
            effectiveDate = immunization.getOccurrenceDateTimeType().getValue();
        }

        if (immunization.hasLocation() && immunization.getLocation().hasDisplay()) {
            location = immunization.getLocation().getDisplay();
        }

        if (immunization.hasNote()) {
            notes = buildNotes(immunization.getNote());
        }
    }

    @Override
    public Immunization toResourceForSDSExport() {
        return sourceResource;
    }

    public String getDescription() {
        return description;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public String getLocation() {
        return location;
    }

    public List<String> getNotes() {
        return notes;
    }
}
