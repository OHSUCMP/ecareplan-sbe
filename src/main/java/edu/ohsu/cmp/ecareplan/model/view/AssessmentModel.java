package edu.ohsu.cmp.ecareplan.model.view;

import com.fasterxml.jackson.annotation.JsonIgnore;
import edu.ohsu.cmp.ecareplan.entity.Assessment;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;

import java.util.Date;
import java.util.List;

public class AssessmentModel {
    private final String name;
    private final String label;
    private final String resourceId;
    private final String url;
    private final String learnMoreUrl;
    private final Boolean scored;
    private final String codeSystem;
    private final String code;
    private final Date effectiveDate;

    @JsonIgnore
    private final Questionnaire questionnaire;

    private final List<ResponseSummary> responseSummaryList;

    public AssessmentModel(Assessment assessment, List<ResponseSummary> responseSummaryList) {
        this.name = assessment.getName();
        this.label = assessment.getLabel();
        this.resourceId = assessment.getResourceId();
        this.url = assessment.getUrl();
        this.learnMoreUrl = assessment.getLearnMoreUrl();
        this.scored = assessment.isScored();
        this.codeSystem = assessment.getCodeSystem();
        this.code = assessment.getCode();
        this.questionnaire = assessment.getQuestionnaire();

        if (responseSummaryList == null || responseSummaryList.isEmpty()) {
            throw new IllegalArgumentException("Response summary list is null or empty");
        }

        for (ResponseSummary responseSummary : responseSummaryList) {
            // verify that the response summary is for the same questionnaire as the assessment
            if ( ! responseSummary.getQuestionnaireResponse().getQuestionnaire().equals(assessment.getQuestionnaire().getUrl()) ) {
                throw new IllegalArgumentException("Response summary questionnaire does not match assessment questionnaire");
            }
        }

        this.responseSummaryList = responseSummaryList;
        this.responseSummaryList.sort((o1, o2) -> o2.getAuthored().compareTo(o1.getAuthored())); // inverse sort order, most recent first

        this.effectiveDate = responseSummaryList.getFirst().getAuthored();
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

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public Questionnaire getQuestionnaire() {
        return questionnaire;
    }

    public List<ResponseSummary> getResponseSummaryList() {
        return responseSummaryList;
    }

    public static final class ResponseSummary {
        @JsonIgnore
        private final QuestionnaireResponse questionnaireResponse;

        private final Date authored;
        private final Number score;
        private final String interpretation;
        private final String sourceEndpointIss;
        private final String sourceEndpointName;


        public ResponseSummary(QuestionnaireResponse questionnaireResponse, Number score, String interpretation, String sourceEndpointIss, String sourceEndpointName) {
            this.questionnaireResponse = questionnaireResponse;
            this.authored = questionnaireResponse.getAuthored();
            this.score = score;
            this.interpretation = interpretation;
            this.sourceEndpointIss = sourceEndpointIss;
            this.sourceEndpointName = sourceEndpointName;
        }

        public QuestionnaireResponse getQuestionnaireResponse() {
            return questionnaireResponse;
        }

        public Date getAuthored() {
            return authored;
        }

        public Number getScore() {
            return score;
        }

        public String getInterpretation() {
            return interpretation;
        }

        public String getSourceEndpointIss() {
            return sourceEndpointIss;
        }

        public String getSourceEndpointName() {
            return sourceEndpointName;
        }
    }
}
