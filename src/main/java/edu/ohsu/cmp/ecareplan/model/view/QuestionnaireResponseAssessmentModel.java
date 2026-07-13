package edu.ohsu.cmp.ecareplan.model.view;

import edu.ohsu.cmp.ecareplan.model.dataset.QuestionnaireResponseModel;

public class QuestionnaireResponseAssessmentModel extends AssessmentModel {
    private QuestionnaireResponseModel sourceModel;

    public QuestionnaireResponseAssessmentModel(QuestionnaireResponseModel sourceModel) {
        this.sourceModel = sourceModel;
    }

    public QuestionnaireResponseModel getSourceModel() {
        return sourceModel;
    }
}
