package edu.ohsu.cmp.ecareplan.model.dataset;

import org.hl7.fhir.r4.model.Binary;
import org.hl7.fhir.r4.model.DocumentReference;

public class ClinicalNoteModel extends BaseDataSetModel {
    private Binary binary;

    public ClinicalNoteModel(DocumentReference documentReference) {
        super(documentReference);
    }

    public Binary getBinary() {
        return binary;
    }

    public void setBinary(Binary binary) {
        this.binary = binary;
    }
}
