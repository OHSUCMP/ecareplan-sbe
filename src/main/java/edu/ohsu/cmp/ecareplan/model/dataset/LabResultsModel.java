package edu.ohsu.cmp.ecareplan.model.dataset;

import edu.ohsu.cmp.ecareplan.util.FhirUtil;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class LabResultsModel extends ObservationModel {
    private static final Coding BP_PANEL_CODING = new Coding("http://loinc.org", "85354-9", "Blood pressure systolic and diastolic");
    private static final Coding SYSTOLIC_CODING = new Coding("http://loinc.org", "8480-6", "Systolic blood pressure");
    private static final Coding DIASTOLIC_CODING = new Coding("http://loinc.org", "8462-4", "Diastolic blood pressure");

    private String commonName;
    private String conceptName;
    private Date effectiveDate;
    private String resultText;
    private BigDecimal resultValue;
    private String resultUnits;
    private String referenceRange;
    private String interpretation;      // complex; skip for now // todo : populate this
    private Boolean flag;
    private List<String> performers;
    private List<String> notes;
    private String learnMore;           // complex; skip for now // todo : populate this

    public LabResultsModel(Observation observation, String commonName) {
        super(observation);

        this.commonName = commonName;

        if (observation.hasCode()) {
            conceptName = getConceptNameFromCodeableConcept(observation.getCode());
        }

        if (observation.hasEffectiveDateTimeType()) {
            effectiveDate = observation.getEffectiveDateTimeType().getValue();
        } else if (observation.hasEffectiveInstantType()) {
            effectiveDate = observation.getEffectiveInstantType().getValue();
        } else if (observation.hasEffectivePeriod() && observation.getEffectivePeriod().hasEnd()) {
            effectiveDate = observation.getEffectivePeriod().getEnd();
        } else if (observation.hasEffectivePeriod() && observation.getEffectivePeriod().hasStart()) {
            effectiveDate = observation.getEffectivePeriod().getStart();
        } else if (observation.hasIssued()) {
            effectiveDate = observation.getIssued();
        }

        if (FhirUtil.hasCoding(observation.getCode(), BP_PANEL_CODING) && observation.hasComponent()) {
            Quantity systolic = getQuantityFromComponent(SYSTOLIC_CODING, observation.getComponent());
            Quantity diastolic = getQuantityFromComponent(DIASTOLIC_CODING, observation.getComponent());
            if (systolic != null && diastolic != null) {
                resultText = systolic.getValue() + "/" + diastolic.getValue() + " " + systolic.getUnit();
            }
        } else if (observation.hasValueQuantity()) {
            resultText = observation.getValueQuantity().getValue().toString() + " " + observation.getValueQuantity().getUnit();
            resultValue = observation.getValueQuantity().getValue();
            resultUnits = observation.getValueQuantity().getUnit();
        } else if (observation.hasValueCodeableConcept()) {
            resultText = getConceptNameFromCodeableConcept(observation.getValueCodeableConcept());
        } else if (observation.hasValueStringType()) {
            resultText = observation.getValueStringType().getValue();
        }

        BigDecimal referenceRangeLow = null;
        BigDecimal referenceRangeHigh = null;
        if (observation.hasReferenceRange()) {
            Observation.ObservationReferenceRangeComponent range = observation.getReferenceRangeFirstRep();
            if (range.hasText()) {
                referenceRange = range.getText();
            } else if (range.hasLow() && range.hasHigh()) {
                referenceRangeLow = range.getLow().getValue();
                referenceRangeHigh = range.getHigh().getValue();
                referenceRange = range.getLow().getValue() + " - " + range.getHigh().getValue();
            }
        }

        flag = false;
        if (resultValue != null && referenceRangeLow != null) {
            if (resultValue.compareTo(referenceRangeLow) < 0) {
                flag = true;
            } else if (resultValue.compareTo(referenceRangeHigh) > 0) {
                flag = true;
            }
        }

        if (observation.hasPerformer()) {
            performers = getDisplayValuesFromReferences(observation.getPerformer());
        }

        if (observation.hasNote()) {
            notes = buildNotes(observation.getNote());
        }
    }

    public String getCommonName() {
        return commonName;
    }

    public String getConceptName() {
        return conceptName;
    }

    public String getDescription() {
        if (commonName != null) {
            return commonName;
        } else if (conceptName != null) {
            return conceptName;
        }
        return "(No description)";
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public String getResultText() {
        return resultText;
    }

    public BigDecimal getResultValue() {
        return resultValue;
    }

    public String getResultUnits() {
        return resultUnits;
    }

    public String getReferenceRange() {
        return referenceRange;
    }

    public String getInterpretation() {
        return interpretation;
    }

    public Boolean getFlag() {
        return flag;
    }

    public List<String> getPerformers() {
        return performers;
    }

    public List<String> getNotes() {
        return notes;
    }

    public String getLearnMore() {
        return learnMore;
    }
}
