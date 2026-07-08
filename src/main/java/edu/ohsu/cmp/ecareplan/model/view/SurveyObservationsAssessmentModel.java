package edu.ohsu.cmp.ecareplan.model.view;

import edu.ohsu.cmp.ecareplan.model.dataset.SurveyObservationModel;

import java.util.List;

public class SurveyObservationsAssessmentModel extends AssessmentModel {
    private List<SurveyObservationModel> sourceModels;

    public SurveyObservationsAssessmentModel(List<SurveyObservationModel> sourceModels) {
        this.sourceModels = sourceModels;
    }

    public List<SurveyObservationModel> getSourceModels() {
        return sourceModels;
    }
}
