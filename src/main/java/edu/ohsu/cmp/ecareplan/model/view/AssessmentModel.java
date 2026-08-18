package edu.ohsu.cmp.ecareplan.model.view;

import edu.ohsu.cmp.ecareplan.entity.Assessment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

public class AssessmentModel {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final String name;
    private final String label;
    private final String resourceId;
    private final String url;
    private final String learnMoreUrl;
    private final Boolean scored;
    private final String codeSystem;
    private final String code;
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

        if (responseSummaryList != null) {
            for (ResponseSummary responseSummary : responseSummaryList) {
                // verify that the response summary is for the same questionnaire as the assessment
                if ( ! responseSummary.getQuestionnaire().equals(assessment.getQuestionnaire().getUrl()) ) {
                    throw new IllegalArgumentException("Response summary questionnaire does not match assessment questionnaire");
                }
            }
        }

        this.responseSummaryList = responseSummaryList;
        this.responseSummaryList.sort((o1, o2) -> o2.getAuthored().compareTo(o1.getAuthored())); // inverse sort order, most recent first
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

    public List<ResponseSummary> getResponseSummaryList() {
        return responseSummaryList;
    }

    public static final class ResponseSummary {
        private final String questionnaire;
        private final Date authored;
        private final Number score;
        private final String interpretation;
        private final String sourceEndpointIss;
        private final String sourceEndpointName;


        public ResponseSummary(String questionnaire, Date authored, Number score, String interpretation, String sourceEndpointIss, String sourceEndpointName) {
            this.questionnaire = questionnaire;
            this.authored = authored;
            this.score = score;
            this.interpretation = interpretation;
            this.sourceEndpointIss = sourceEndpointIss;
            this.sourceEndpointName = sourceEndpointName;
        }

        public String getQuestionnaire() {
            return questionnaire;
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
