package edu.ohsu.cmp.ecareplan.model.dataset;

import org.hl7.fhir.r4.model.Observation;

public class LabResultModel extends ObservationModel implements Consolidatable {
    private final String commonName;

    public LabResultModel(Observation observation, String commonName) {
        super(observation);
        this.commonName = commonName;
    }

    @Override
    public Observation toResourceForSDSExport() {
        return sourceResource;
    }

    @Override
    public String getConsolidationKey() {
        return getDescription();
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
