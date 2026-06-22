package edu.ohsu.cmp.ecareplan.model.dataset;

import org.hl7.fhir.r4.model.QuestionnaireResponse;

public class AssessmentModel extends BaseDataSetModel<QuestionnaireResponse> {
    public AssessmentModel(QuestionnaireResponse questionnaireResponse) {
        super(questionnaireResponse);
    }

    @Override
    public QuestionnaireResponse toResourceForSDSExport() {
        return sourceResource;
    }
}
