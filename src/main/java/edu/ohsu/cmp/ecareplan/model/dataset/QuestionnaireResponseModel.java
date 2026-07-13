package edu.ohsu.cmp.ecareplan.model.dataset;

import org.hl7.fhir.r4.model.QuestionnaireResponse;

public class QuestionnaireResponseModel extends BaseDataSetModel<QuestionnaireResponse> {
    public QuestionnaireResponseModel(QuestionnaireResponse questionnaireResponse) {
        super(questionnaireResponse);
    }

    @Override
    public QuestionnaireResponse toResourceForSDSExport() {
        return sourceResource;
    }
}
