package edu.ohsu.cmp.ecareplan.model.dataset;

import org.hl7.fhir.r4.model.ServiceRequest;

public class ServiceRequestModel extends BaseDataSetModel {
    public ServiceRequestModel(ServiceRequest serviceRequest) {
        super(serviceRequest);
    }
}
