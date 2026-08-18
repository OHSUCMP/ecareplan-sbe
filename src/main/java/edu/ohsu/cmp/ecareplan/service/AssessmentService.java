package edu.ohsu.cmp.ecareplan.service;

import edu.ohsu.cmp.ecareplan.entity.Assessment;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSet;
import edu.ohsu.cmp.ecareplan.model.dataset.QuestionnaireResponseModel;
import edu.ohsu.cmp.ecareplan.model.dataset.SurveyObservationModel;
import edu.ohsu.cmp.ecareplan.model.view.AssessmentModel;
import edu.ohsu.cmp.ecareplan.repository.AssessmentRepository;
import edu.ohsu.cmp.ecareplan.util.FhirUtil;
import edu.ohsu.cmp.ecareplan.util.NumberUtil;
import edu.ohsu.cmp.ecareplan.workspace.UserWorkspace;
import org.apache.commons.lang3.Strings;
import org.hl7.fhir.r4.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AssessmentService extends BaseService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private AssessmentRepository assessmentRepository;

    public List<AssessmentModel> getAssessmentModels(String sessionId) {
        UserWorkspace workspace = userWorkspaceService.get(sessionId);

        // prepare - split everything out by their respective endpoints - we don't want to inadvertently combine resources from different endpoints
        Map<String, String> sourceEndpointMap = new LinkedHashMap<>();
        Map<String, List<QuestionnaireResponseModel>> questionnaireResponseMap = new LinkedHashMap<>();
        Map<String, List<SurveyObservationModel>> observationResponseMap = new LinkedHashMap<>();

        for (QuestionnaireResponseModel qrm : workspace.getAllDataSetModels(DataSet.QUESTIONNAIRE_RESPONSES)) {
            if ( ! sourceEndpointMap.containsKey(qrm.getSourceEndpointIss()) ) {
                sourceEndpointMap.put(qrm.getSourceEndpointIss(), qrm.getSourceEndpointName());
            }
            if ( ! questionnaireResponseMap.containsKey(qrm.getSourceEndpointIss()) ) {
                questionnaireResponseMap.put(qrm.getSourceEndpointIss(), new ArrayList<>());
            }
            questionnaireResponseMap.get(qrm.getSourceEndpointIss()).add(qrm);
        }

        for (SurveyObservationModel som : workspace.getAllDataSetModels(DataSet.SURVEY_OBSERVATIONS)) {
            if ( ! sourceEndpointMap.containsKey(som.getSourceEndpointIss()) ) {
                sourceEndpointMap.put(som.getSourceEndpointIss(), som.getSourceEndpointName());
            }
            if ( ! observationResponseMap.containsKey(som.getSourceEndpointIss()) ) {
                observationResponseMap.put(som.getSourceEndpointIss(), new ArrayList<>());
            }
            observationResponseMap.get(som.getSourceEndpointIss()).add(som);
        }

        List<AssessmentModel> list = new ArrayList<>();


        for (Assessment assessment : assessmentRepository.findByActiveTrue()) {
            List<AssessmentModel.ResponseSummary> responseList = new ArrayList<>();

            for (Map.Entry<String, String> sourceEndpoint : sourceEndpointMap.entrySet()) {
                List<QuestionnaireResponse> questionnaireResponseList = new ArrayList<>();

                if (questionnaireResponseMap.containsKey(sourceEndpoint.getKey())) {
                    for (QuestionnaireResponseModel qrm : questionnaireResponseMap.get(sourceEndpoint.getKey())) {
                        if (qrm.getSourceResource().hasQuestionnaire() &&
                                Strings.CS.equals(qrm.getSourceResource().getQuestionnaire(), assessment.getQuestionnaire().getUrl())) {
                            questionnaireResponseList.add(qrm.getSourceResource());
                        }
                    }
                }

                if (observationResponseMap.containsKey(sourceEndpoint.getKey())) {
                    List<QuestionnaireResponse> observationResponseList = convertObservations(assessment, observationResponseMap.get(sourceEndpoint.getKey()));
                    if (observationResponseList != null && ! observationResponseList.isEmpty()) {
                        questionnaireResponseList.addAll(observationResponseList);
                    }
                }

                responseList.addAll(buildResponseSummaryList(assessment.getQuestionnaire(), questionnaireResponseList, assessment.isScored(), sourceEndpoint.getKey(), sourceEndpoint.getValue()));
            }

            if ( ! responseList.isEmpty() ) {
                list.add(new AssessmentModel(assessment, responseList));
            }
        }

        return list;
    }

    private List<QuestionnaireResponse> convertObservations(Assessment assessment, List<SurveyObservationModel> surveyObservationModelList) {
        if (surveyObservationModelList == null) return null;

        List<SurveyObservationModel> topLevelModels = surveyObservationModelList.stream()
                .filter(o -> FhirUtil.hasCoding(o.getSourceResource().getCode(), assessment.getCoding()))
                .toList();

        if (topLevelModels.isEmpty()) return null;

        Map<String, SurveyObservationModel> map = new HashMap<>(); // facilitate lookups and matching
        for (SurveyObservationModel som : surveyObservationModelList) {
            map.put(FhirUtil.toRelativeReference(som.getSourceResource().getId()), som);
        }

        List<QuestionnaireResponse> list = new ArrayList<>();

        for (SurveyObservationModel topLevelModel : topLevelModels) {
            if (topLevelModel.getSourceResource().hasHasMember()) {
                List<SurveyObservationModel> members = new ArrayList<>();

                for (Reference ref : topLevelModel.getSourceResource().getHasMember()) {
                    String reference = FhirUtil.toRelativeReference(ref.getReference());
                    if (map.containsKey(reference)) {
                        members.add(map.get(reference));
                    }
                }

                QuestionnaireResponse qr = new QuestionnaireResponse();
                qr.setStatus(QuestionnaireResponse.QuestionnaireResponseStatus.COMPLETED);
                qr.setQuestionnaire(assessment.getQuestionnaire().getUrl());
                qr.setAuthored(topLevelModel.getEffectiveDate());

                List<QuestionnaireResponse.QuestionnaireResponseItemComponent> responseItems = new ArrayList<>();
                collectMatchingItems(assessment.getQuestionnaire().getItem(), members, responseItems);
                qr.setItem(responseItems);

                list.add(qr);
            }
        }

        return list;
    }

    private void collectMatchingItems(List<Questionnaire.QuestionnaireItemComponent> questionnaireItems,
                                      List<SurveyObservationModel> members,
                                      List<QuestionnaireResponse.QuestionnaireResponseItemComponent> responseItems) {

        for (Questionnaire.QuestionnaireItemComponent item : questionnaireItems) {
            if ( ! item.hasCode() ) continue;

            Observation observation = null;
            for (SurveyObservationModel member : members) {
                Observation o = member.getSourceResource();
                if (o.hasCode() && FhirUtil.hasCoding(o.getCode(), item.getCodeFirstRep())) {
                    observation = o;
                    break;
                }
            }

            if (observation != null) {
                QuestionnaireResponse.QuestionnaireResponseItemComponent responseItem = new QuestionnaireResponse.QuestionnaireResponseItemComponent();
                responseItem.setLinkId(item.getLinkId());
                responseItem.setText(item.getText());
                responseItem.setAnswer(new ArrayList<>());

                if (observation.hasValueCodeableConcept()) {
                    for (Coding c : observation.getValueCodeableConcept().getCoding()) {
                        responseItem.getAnswer().add(new QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().setValue(c));
                    }
                } else if (observation.hasValueQuantity()) {
                    responseItem.getAnswer().add(new QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().setValue(observation.getValueQuantity()));
                } else if (observation.hasValueIntegerType()) {
                    responseItem.getAnswer().add(new QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent().setValue(observation.getValueIntegerType()));
                }

                responseItems.add(responseItem);
            }

            if (item.hasItem()) {
                collectMatchingItems(item.getItem(), members, responseItems);
            }
        }
    }

    private List<AssessmentModel.ResponseSummary> buildResponseSummaryList(Questionnaire questionnaire, List<QuestionnaireResponse> questionnaireResponseList,
                                                                           boolean isScored,
                                                                           String sourceEndpointIss, String sourceEndpointName) {
        if (questionnaireResponseList == null) return null;

        List<AssessmentModel.ResponseSummary> list = new ArrayList<>();

        for (QuestionnaireResponse qr : questionnaireResponseList) {
            try {
                if (qr == null || !qr.hasAuthored()) continue;
                Number score = extractResponseScore(questionnaire, qr, isScored);
                list.add(new AssessmentModel.ResponseSummary(qr.getQuestionnaire(), qr.getAuthored(), score,
                        interpretScore(questionnaire, score),
                        sourceEndpointIss, sourceEndpointName));
            } catch (Exception e) {
                logger.error("caught {} building response summary for QuestionnaireResponse {} - {}", e.getClass().getSimpleName(), qr.getId(), e.getMessage(), e);
            }
        }

        return list;
    }

    private static final String EXTENSION_RANGE_SCORE_INTERPRETATION_URL = "range-score-interpretation";
    private static final String EXTENSION_RANGE_URL = "range";
    private static final String EXTENSION_INTERPRETATION_URL = "interpretation";

    private String interpretScore(Questionnaire questionnaire, Number score) {
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

    private Number extractResponseScore(Questionnaire questionnaire, QuestionnaireResponse qr, boolean isScored) {
        if (isScored) {
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
}
