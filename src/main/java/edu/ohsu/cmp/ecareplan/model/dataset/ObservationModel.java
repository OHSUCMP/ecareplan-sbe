package edu.ohsu.cmp.ecareplan.model.dataset;

import edu.ohsu.cmp.ecareplan.util.FhirUtil;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Quantity;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public abstract class ObservationModel extends BaseDataSetModel<Observation> {
    private static final List<Coding> BP_CODINGS = List.of(
            new Coding("http://loinc.org", "85354-9", "Blood pressure panel with all children optional"),
            new Coding("http://loinc.org", "72076-3", "Blood pressure home reading"),
            new Coding("http://loinc.org", "55284-4", "Blood pressure systolic and diastolic")
    );

    private static final Coding SYSTOLIC_CODING = new Coding("http://loinc.org", "8480-6", "Systolic blood pressure");
    private static final Coding DIASTOLIC_CODING = new Coding("http://loinc.org", "8462-4", "Diastolic blood pressure");
    private static final String BP_UNIT = "mmHg";

    private String conceptName;
    private Date effectiveDate;
    private String resultText;
    private ResultValue resultValue;
    private String resultUnits;
    private String referenceRange;
    private String interpretation;      // complex; skip for now // todo : populate this
    private Boolean flag;
    private List<String> performers;
    private List<String> notes;
    private String learnMore;           // complex; skip for now // todo : populate this

    public ObservationModel(Observation observation) {
        super(observation);

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

        if (observation.hasComponent() && FhirUtil.hasCoding(observation.getCode(), BP_CODINGS) && observation.hasComponent()) {
            Quantity systolic = getQuantityFromComponent(SYSTOLIC_CODING, observation.getComponent());
            Quantity diastolic = getQuantityFromComponent(DIASTOLIC_CODING, observation.getComponent());
            if (systolic != null && diastolic != null) {
                String unit = systolic.hasUnit() ? // it should always be "mmHg" but if it's phrased differently for some obscure reason, use that
                        systolic.getUnit() :
                        BP_UNIT;
                resultText = systolic.getValue().toString() + "/" + diastolic.getValue().toString() + " " + unit;

                ResultValue.Component systolicComponent = new ResultValue.Component("Systolic", systolic.getValue());
                ResultValue.Component diastolicComponent = new ResultValue.Component("Diastolic", diastolic.getValue());
                resultValue = new ResultValue(List.of(systolicComponent, diastolicComponent));

                resultUnits = unit;
            }
        } else if (observation.hasValueQuantity()) {
            Quantity q = observation.getValueQuantity();
            resultValue = new ResultValue(conceptName, q.getValue());
            if (q.hasUnit()) {
                resultUnits = q.getUnit();
                resultText = q.getValue().toString() + " " + q.getUnit();
            } else {
                resultText = q.getValue().toString();
            }
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
        if (resultValue != null && resultValue.isComparable() && referenceRangeLow != null) {
            if (resultValue.getValueForCompare().compareTo(referenceRangeLow) < 0) {
                flag = true;
            } else if (resultValue.getValueForCompare().compareTo(referenceRangeHigh) > 0) {
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

    public String getConceptName() {
        return conceptName;
    }

    public String getDescription() {
        return conceptName != null ?
                conceptName :
                "(No description)";
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public String getResultText() {
        return resultText;
    }

    public ResultValue getResultValue() {
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

    protected Quantity getQuantityFromComponent(Coding coding, List<Observation.ObservationComponentComponent> components) {
        for (Observation.ObservationComponentComponent component : components) {
            if (FhirUtil.hasCoding(component.getCode(), coding)) {
                if (component.hasValueQuantity()) {
                    return component.getValueQuantity();
                }
            }
        }
        return null;
    }
}
