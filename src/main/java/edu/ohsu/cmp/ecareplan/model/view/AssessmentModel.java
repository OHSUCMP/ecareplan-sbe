package edu.ohsu.cmp.ecareplan.model.view;

import edu.ohsu.cmp.ecareplan.entity.Assessment;
import edu.ohsu.cmp.ecareplan.util.NumberUtil;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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
    private final Questionnaire questionnaire;
    private final List<ResponseSummary> responseSummaryList;
    private final String sourceEndpointIss;
    private final String sourceEndpointName;

    public AssessmentModel(Assessment assessment, List<QuestionnaireResponse> questionnaireResponseList, String sourceEndpointIss, String sourceEndpointName) {
        this.name = assessment.getName();
        this.label = assessment.getLabel();
        this.resourceId = assessment.getResourceId();
        this.url = assessment.getUrl();
        this.learnMoreUrl = assessment.getLearnMoreUrl();
        this.scored = assessment.isScored();
        this.codeSystem = assessment.getCodeSystem();
        this.code = assessment.getCode();
        this.questionnaire = assessment.getQuestionnaire();
        this.responseSummaryList = buildResponseSummaryList(questionnaireResponseList);
        this.sourceEndpointIss = sourceEndpointIss;
        this.sourceEndpointName = sourceEndpointName;
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

    public List<ResponseSummary> getResponseSummaryList() {
        return responseSummaryList;
    }

    public String getSourceEndpointIss() {
        return sourceEndpointIss;
    }

    public String getSourceEndpointName() {
        return sourceEndpointName;
    }


//////////////////////////////////////////////////////////////////
/// private stuff
///

    private List<ResponseSummary> buildResponseSummaryList(List<QuestionnaireResponse> questionnaireResponseList) {
        if (questionnaireResponseList == null) return null;

        List<ResponseSummary> list = new ArrayList<>();

        for (QuestionnaireResponse qr : questionnaireResponseList) {
            try {
                if (qr == null || !qr.hasAuthored()) continue;
                Number score = extractResponseScore(qr);
                list.add(new ResponseSummary(qr, score, interpretScore(score)));
            } catch (Exception e) {
                logger.error("caught {} building response summary for QuestionnaireResponse {} - {}", e.getClass().getSimpleName(), qr.getId(), e.getMessage(), e);
            }
        }

        return list;
    }

    private static final String EXTENSION_RANGE_SCORE_INTERPRETATION_URL = "range-score-interpretation";
    private static final String EXTENSION_RANGE_URL = "range";
    private static final String EXTENSION_INTERPRETATION_URL = "interpretation";

    private String interpretScore(Number score) {
        Questionnaire.QuestionnaireItemComponent scoreItem = findScoreItem(questionnaire.getItem());
        if (scoreItem != null) {
            for (Extension rangeInterpretation : scoreItem.getExtensionsByUrl(EXTENSION_RANGE_SCORE_INTERPRETATION_URL)) {
                if (rangeInterpretation.hasExtension(EXTENSION_RANGE_URL) && rangeInterpretation.hasExtension(EXTENSION_INTERPRETATION_URL)) {
                    Extension range = rangeInterpretation.getExtensionByUrl(EXTENSION_RANGE_URL);
                    if (range.hasValue() && range.getValue() instanceof Range r) {
                        if (r.hasLow() && r.hasHigh() &&
                                r.getLow().hasValue() && r.getLow().getValue().compareTo(NumberUtil.toBigDecimal(score)) <= 0 &&
                                r.getHigh().hasValue() && r.getHigh().getValue().compareTo(NumberUtil.toBigDecimal(score)) >= 0) {
                            Extension interpretation = rangeInterpretation.getExtensionByUrl(EXTENSION_INTERPRETATION_URL);
                            if (interpretation.hasValue() && interpretation.getValue() instanceof StringType st) {
                                return st.getValue();
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private Number extractResponseScore(QuestionnaireResponse qr) {
        if (scored) {
            Questionnaire.QuestionnaireItemComponent scoreItem = findScoreItem(questionnaire.getItem());
            if (scoreItem != null) {
                return findScoreValueByLinkId(qr.getItem(), scoreItem.getLinkId());
            }
        }

        return null;
    }

    private Number findScoreValueByLinkId(List<QuestionnaireResponse.QuestionnaireResponseItemComponent> items, String targetLinkId) {
        if (items == null) return null;

        for (QuestionnaireResponse.QuestionnaireResponseItemComponent item: items) {
            if (item.hasLinkId() && item.getLinkId().equals(targetLinkId) && item.hasAnswer() && ! item.getAnswer().isEmpty()) {
                QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent answer = item.getAnswerFirstRep();
                return extractAnswerValue(answer);
            }

            if (item.hasItem() && ! item.getItem().isEmpty()) {
                Number result = findScoreValueByLinkId(item.getItem(), targetLinkId);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    private Number extractAnswerValue(QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent answer) {
        if (answer.hasValueIntegerType()) {
            return answer.getValueIntegerType().getValue();
        } else if (answer.hasValueDecimalType()) {
            return answer.getValueDecimalType().getValue();
        } else if (answer.hasValueQuantity()) {
            return answer.getValueQuantity().getValue();
        }
        return null;
    }

    private Questionnaire.QuestionnaireItemComponent findScoreItem(List<Questionnaire.QuestionnaireItemComponent> itemList) {
        if (itemList == null) return null;

        for (Questionnaire.QuestionnaireItemComponent item : itemList) {
            if (isScoreQuestion(item)) {
                return item;
            }

            if (item.hasItem() && ! item.getItem().isEmpty()) {
                Questionnaire.QuestionnaireItemComponent found = findScoreItem(item.getItem());
                if (found != null) return found;
            }
        }

        return null;
    }

    private static final String EXTENSION_QUESTIONNAIRE_UNIT_URL = "http://hl7.org/fhir/StructureDefinition/questionnaire-unit";
    private static final String CODE_CARE_PLAN_SCORE = "care-plan-score";

    private boolean isScoreQuestion(Questionnaire.QuestionnaireItemComponent item) {
        if (item == null) return false;

        for (Extension extension : item.getExtensionsByUrl(EXTENSION_QUESTIONNAIRE_UNIT_URL)) {
            if (extension.hasValue()) {
                Type value = extension.getValue();
                if (value instanceof Coding c) {
                    return c.hasCode() && c.getCode().equals(CODE_CARE_PLAN_SCORE);
                }
            }
        }

        return false;
    }

    public static final class ResponseSummary {
        private final QuestionnaireResponse questionnaireResponse;
        private final Date authored;
        private final Number score;
        private final String interpretation;

        public ResponseSummary(QuestionnaireResponse questionnaireResponse, Number score, String interpretation) {
            this.questionnaireResponse = questionnaireResponse;
            this.authored = questionnaireResponse.getAuthored();
            this.score = score;
            this.interpretation = interpretation;
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
    }
}
