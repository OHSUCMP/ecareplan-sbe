package edu.ohsu.cmp.ecareplan.service;

import edu.ohsu.cmp.ecareplan.entity.Assessment;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSet;
import edu.ohsu.cmp.ecareplan.model.dataset.QuestionnaireResponseModel;
import edu.ohsu.cmp.ecareplan.model.dataset.SurveyObservationModel;
import edu.ohsu.cmp.ecareplan.model.view.AssessmentModel;
import edu.ohsu.cmp.ecareplan.repository.AssessmentRepository;
import edu.ohsu.cmp.ecareplan.util.FhirUtil;
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
            for (Map.Entry<String, String> sourceEndpoint : sourceEndpointMap.entrySet()) {
                List<QuestionnaireResponse> responseList = new ArrayList<>();

                if (questionnaireResponseMap.containsKey(sourceEndpoint.getKey())) {
                    for (QuestionnaireResponseModel qrm : questionnaireResponseMap.get(sourceEndpoint.getKey())) {
                        if (qrm.getSourceResource().hasQuestionnaire() &&
                                Strings.CS.equals(qrm.getSourceResource().getQuestionnaire(), assessment.getQuestionnaire().getUrl())) {
                            responseList.add(qrm.getSourceResource());
                        }
                    }
                }

                if (observationResponseMap.containsKey(sourceEndpoint.getKey())) {
                    List<QuestionnaireResponse> observationResponseList = convertObservations(assessment, observationResponseMap.get(sourceEndpoint.getKey()));
                    if (observationResponseList != null && ! observationResponseList.isEmpty()) {
                        responseList.addAll(observationResponseList);
                    }
                }

                if ( ! responseList.isEmpty() ) {
                    list.add(new AssessmentModel(assessment, responseList, sourceEndpoint.getKey(), sourceEndpoint.getValue()));
                }
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
}
