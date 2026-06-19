package edu.ohsu.cmp.ecareplan.model.dataset;

import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Condition;
import org.hl7.fhir.r4.model.Period;

import java.util.Date;
import java.util.List;

public class ConditionModel extends BaseDataSetModel<Condition> {
    private String category;
    private String commonName;
    private String conceptName;
    private Date recordedDate;
    private Date assertedDate;          // todo: populate this from extension
                                        // http://hl7.org/fhir/StructureDefinition/condition-assertedDate (dateTime type)
    private Date onsetDate;
    private String recorder;
    private String asserter;
    private List<String> notes;
    private List<GoalModel> goals;      // todo : these need to be linked after object creation
    private String learnMore;           // complex; skip for now // todo : populate this

    public ConditionModel(Condition condition, String category, String commonName) {
        super(condition);
        this.category = category;
        this.commonName = commonName;

        if (condition.hasCode()) {
            conceptName = getConceptNameFromCodeableConcept(condition.getCode());
        }

        if (condition.hasRecordedDate()) {
            recordedDate = condition.getRecordedDate();
        }

        if (condition.hasOnsetDateTimeType()) {
            onsetDate = condition.getOnsetDateTimeType().getValue();
        } else if (condition.hasOnsetPeriod()) {
            Period p = condition.getOnsetPeriod();
            if (p.hasEnd()) {
                onsetDate = p.getEnd();
            } else if (p.hasStart()) {
                onsetDate = p.getStart();
            }
        }

        if (condition.hasRecorder() && condition.getRecorder().hasDisplay()) {
            recorder = condition.getRecorder().getDisplay();
        }

        if (condition.hasAsserter() && condition.getAsserter().hasDisplay()) {
            asserter = condition.getAsserter().getDisplay();
        }

        if (condition.hasNote()) {
            notes = buildNotes(condition.getNote());
        }
    }

    public String getCategory() {
        return category;
    }

    public String getCommonName() {
        return commonName;
    }

    public String getConceptName() {
        return conceptName;
    }

    public String getName() {
        if (StringUtils.isNotBlank(commonName)) {
            return commonName;
        } else if (StringUtils.isNotBlank(conceptName)) {
            return conceptName;
        } else {
            return "Missing Condition Name";
        }
    }

    public Date getRecordedDate() {
        return recordedDate;
    }

    public Date getAssertedDate() {
        return assertedDate;
    }

    public Date getOnsetDate() {
        return onsetDate;
    }

    public String getRecorder() {
        return recorder;
    }

    public String getAsserter() {
        return asserter;
    }

    public String getAuthor() {
        if (StringUtils.isNotBlank(recorder)) {
            return recorder;
        } else if (StringUtils.isNotBlank(asserter)) {
            return asserter;
        } else {
            return "Unknown";
        }
    }

    public List<String> getNotes() {
        return notes;
    }

    public List<GoalModel> getGoals() {
        return goals;
    }

    public void setGoals(List<GoalModel> goals) {
        this.goals = goals;
    }

    public String getLearnMore() {
        return learnMore;
    }
}
