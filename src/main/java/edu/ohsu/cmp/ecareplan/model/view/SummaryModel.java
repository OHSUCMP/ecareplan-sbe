package edu.ohsu.cmp.ecareplan.model.view;

import edu.ohsu.cmp.ecareplan.model.dataset.PatientModel;

import java.util.Date;

public class SummaryModel {
    private final PatientModel patientModel;
    private final Date sdsLastSyncCompleted;

    public SummaryModel(PatientModel patientModel, Date sdsLastSyncCompleted) {
        this.patientModel = patientModel;
        this.sdsLastSyncCompleted = sdsLastSyncCompleted;
    }

    public PatientModel getPatientModel() {
        return patientModel;
    }

    public String getSourceEndpointName() {
        return patientModel.getSourceEndpointName();
    }

    public Boolean isSourcedFromSDS() {
        return patientModel.isSourcedFromSDS();
    }

    public Date getSdsLastSyncCompleted() {
        return sdsLastSyncCompleted;
    }
}
