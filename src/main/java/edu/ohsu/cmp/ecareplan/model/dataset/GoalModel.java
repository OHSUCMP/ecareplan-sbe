package edu.ohsu.cmp.ecareplan.model.dataset;

import edu.ohsu.cmp.ecareplan.util.DateUtil;
import org.hl7.fhir.r4.model.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GoalModel extends BaseDataSetModel {
    private String category;            // concept text from goal.category[0]
    private String description;         // concept text from goal.description
    private String expressedBy;         // goal.expressedBy.display.value
    private Date startDate;             // goal.start
    private String target;              // complex; skip for now // todo : populate this
    private String addresses;           // complex; skip for now // todo : populate this
    private String lifecycleStatus;     // goal.lifecycleStatus.value
    private String achievementStatus;   // goal.achievementStatus.value
    private List<String> notes;         // consolidation of goal.note.text
    private String learnMore;           // complex; skip for now // todo : populate this
    private Boolean overdue;            // true if goal.target.due.value is before today

    public GoalModel(Goal goal) {
        super(goal);

        if (goal.hasCategory()) {
            CodeableConcept cc = goal.getCategoryFirstRep();
            if (cc.hasText()) {
                this.category = cc.getText();
            } else if (cc.hasCoding()) {
                Coding c = cc.getCodingFirstRep();
                if (c.hasDisplay()) {
                    this.category = c.getDisplay();
                }
            }
        }

        if (goal.hasDescription() && goal.getDescription().hasText()) {
            this.description = goal.getDescription().getText();
        }

        if (goal.hasExpressedBy() && goal.getExpressedBy().hasDisplay()) {
            this.expressedBy = goal.getExpressedBy().getDisplay();
        }

        if (goal.hasStartDateType() && goal.getStartDateType().hasValue()) {
            this.startDate = goal.getStartDateType().getValue();
        }

        if (goal.hasLifecycleStatus()) {
            this.lifecycleStatus = goal.getLifecycleStatus().getDisplay();
        }

        if (goal.hasAchievementStatus()) {
            if (goal.getAchievementStatus().hasText()) {
                this.achievementStatus = goal.getAchievementStatus().getText();
            } else if (goal.getAchievementStatus().hasCoding()) {
                Coding c = goal.getAchievementStatus().getCodingFirstRep();
                if (c.hasDisplay()) {
                    this.achievementStatus = c.getDisplay();
                } else if (c.hasCode()) {
                    this.achievementStatus = c.getCode();
                }
            }
        }

        if (goal.hasNote()) {
            this.notes = new ArrayList<>();
            for (Annotation note : goal.getNote()) {
                if (note.hasText()) {
                    this.notes.add(note.getText());
                }
            }
        }

        if (goal.hasTarget()) {
            for (Goal.GoalTargetComponent target : goal.getTarget()) {
                if (target.hasDue() && target.getDue() instanceof DateType) {
                    this.overdue = target.getDueDateType().getValue().before(DateUtil.startOfToday());
                }
            }
        }
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getExpressedBy() {
        return expressedBy;
    }

    public Date getStartDate() {
        return startDate;
    }

    public String getTarget() {
        return target;
    }

    public String getAddresses() {
        return addresses;
    }

    public String getLifecycleStatus() {
        return lifecycleStatus;
    }

    public String getAchievementStatus() {
        return achievementStatus;
    }

    public String getConsolidatedStatus() {
        if (lifecycleStatus != null && achievementStatus != null) {
            return lifecycleStatus + " - " + achievementStatus;
        } else if (lifecycleStatus != null) {
            return lifecycleStatus;
        } else if (achievementStatus != null) {
            return achievementStatus;
        } else {
            return null;
        }
    }

    public List<String> getNotes() {
        return notes;
    }

    public String getFirstNote() {
        return notes != null && ! notes.isEmpty() ?
                notes.getFirst() :
                null;
    }

    public String getLearnMore() {
        return learnMore;
    }

    public Boolean getOverdue() {
        return overdue;
    }
}
