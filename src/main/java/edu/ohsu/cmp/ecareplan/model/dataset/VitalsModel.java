package edu.ohsu.cmp.ecareplan.model.dataset;

import org.hl7.fhir.r4.model.Observation;

import java.util.Date;

public class VitalsModel extends ObservationModel implements Consolidatable<Date> {
    private final String commonName;

    public VitalsModel(Observation observation, String commonName) {
        super(observation);
        this.commonName = commonName;
    }

    @Override
    public Observation toResourceForSDSExport() {
        return sourceResource;
    }

    @Override
    public String getConsolidationGroupBy() {
        return getDescription();
    }

    @Override
    public Date getConsolidationSortBy() {
        return getEffectiveDate();
    }

    public String getCommonName() {
        return commonName;
    }

    public String getDescription() {
        return commonName != null ?
                commonName :
                super.getDescription();
    }
}
