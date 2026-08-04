package edu.ohsu.cmp.ecareplan.model.view;

import edu.ohsu.cmp.ecareplan.entity.Assessment;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;

import java.util.List;

public class AssessmentModel {
    private String name;
    private String label;
    private String resourceId;
    private String url;
    private String learnMoreUrl;
    private Boolean scored;
    private String codeSystem;
    private String code;
    private Questionnaire questionnaire;
    private List<QuestionnaireResponse> questionnaireResponseList;

    public AssessmentModel(Assessment assessment, List<QuestionnaireResponse> questionnaireResponseList) {
        this.name = assessment.getName();
        this.label = assessment.getLabel();
        this.resourceId = assessment.getResourceId();
        this.url = assessment.getUrl();
        this.learnMoreUrl = assessment.getLearnMoreUrl();
        this.scored = assessment.isScored();
        this.codeSystem = assessment.getCodeSystem();
        this.code = assessment.getCode();
        this.questionnaire = assessment.getQuestionnaire();
        this.questionnaireResponseList = questionnaireResponseList;
    }

    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getUrl() {
        return url;
    }

    public String getLearnMoreUrl() {
        return learnMoreUrl;
    }

    public Boolean getScored() {
        return scored;
    }

    public String getCodeSystem() {
        return codeSystem;
    }

    public String getCode() {
        return code;
    }

    public Questionnaire getQuestionnaire() {
        return questionnaire;
    }

    public List<QuestionnaireResponse> getQuestionnaireResponseList() {
        return questionnaireResponseList;
    }
}
