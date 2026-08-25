package edu.ohsu.cmp.ecareplan.model.view;

import edu.ohsu.cmp.ecareplan.model.dataset.VitalsModel;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.*;

public class CompositeBloodPressureModel implements IVitalsModel {
    private static final String COMMON_NAME = "Blood Pressure";

    private final VitalsModel systolicModel;
    private final VitalsModel diastolicModel;

    public CompositeBloodPressureModel(VitalsModel systolicModel, VitalsModel diastolicModel) {
        if (systolicModel == null || diastolicModel == null) {
            throw new IllegalArgumentException("systolicModel and diastolicModel must not be null");
        }

        if ( ! systolicModel.getEffectiveDate().equals(diastolicModel.getEffectiveDate()) ) {
            throw new IllegalArgumentException("systolicModel and diastolicModel must have the same effective date");
        }

        if ( ! systolicModel.getSourceEndpointIss().equals(diastolicModel.getSourceEndpointIss()) ) {
            throw new IllegalArgumentException("systolicModel and diastolicModel must have the same source endpoint");
        }

        this.systolicModel = systolicModel;
        this.diastolicModel = diastolicModel;
    }

    @Override
    public String getId() {
        return systolicModel.getId() + "," + diastolicModel.getId();
    }

    @Override
    public String getConceptName() {
        return COMMON_NAME;
    }

    @Override
    public String getDescription() {
        return COMMON_NAME;
    }

    @Override
    public Date getEffectiveDate() {
        return systolicModel.getEffectiveDate();
    }

    @Override
    public String getResultText() {
        return systolicModel.getResultValue().toString() + "/" + diastolicModel.getResultValue().toString() + systolicModel.getResultUnits();
    }

    @Override
    public BigDecimal getResultValue() {
        return null;
    }

    @Override
    public String getResultUnits() {
        return systolicModel.getResultUnits();
    }

    @Override
    public String getReferenceRange() {
        List<String> referenceRanges = new ArrayList<>();
        if (StringUtils.isNotBlank(systolicModel.getReferenceRange())) {
            referenceRanges.add("Systolic: " + systolicModel.getReferenceRange());
        }
        if (StringUtils.isNotBlank(diastolicModel.getReferenceRange())) {
            referenceRanges.add("Diastolic: " + diastolicModel.getReferenceRange());
        }
        return String.join("; ", referenceRanges);
    }

    @Override
    public String getInterpretation() {
        return null;
    }

    @Override
    public Boolean getFlag() {
        return systolicModel.getFlag() || diastolicModel.getFlag();
    }

    @Override
    public List<String> getPerformers() {
        Set<String> performers = new LinkedHashSet<>();
        if (systolicModel.getPerformers() != null) {
            performers.addAll(systolicModel.getPerformers());
        }
        if (diastolicModel.getPerformers() != null) {
            performers.addAll(diastolicModel.getPerformers());
        }
        return new ArrayList<>(performers);
    }

    @Override
    public List<String> getNotes() {
        Set<String> notes = new LinkedHashSet<>();
        if (systolicModel.getNotes() != null) {
            notes.addAll(systolicModel.getNotes());
        }
        if (diastolicModel.getNotes() != null) {
            notes.addAll(diastolicModel.getNotes());
        }
        return new ArrayList<>(notes);
    }

    @Override
    public String getLearnMore() {
        return systolicModel.getLearnMore();
    }

    @Override
    public String getSourceEndpointName() {
        return systolicModel.getSourceEndpointName();
    }

    @Override
    public String getSourceEndpointIss() {
        return systolicModel.getSourceEndpointIss();
    }

    @Override
    public String getConsolidationGroupBy() {
        return getDescription();
    }

    @Override
    public Date getConsolidationSortBy() {
        return getEffectiveDate();
    }
}
