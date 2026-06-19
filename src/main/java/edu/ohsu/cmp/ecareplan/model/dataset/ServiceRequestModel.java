package edu.ohsu.cmp.ecareplan.model.dataset;

import org.hl7.fhir.r4.model.ServiceRequest;

import java.util.Date;
import java.util.List;

public class ServiceRequestModel extends BaseDataSetModel<ServiceRequest> {
    private String code;
    private String requester;
    private Date authoredOn;
    private List<String> reasons;
    private List<String> notes;

    public ServiceRequestModel(ServiceRequest serviceRequest) {
        super(serviceRequest);

        if (serviceRequest.hasCode()) {
            code = getConceptNameFromCodeableConcept(serviceRequest.getCode());
        }

        if (serviceRequest.hasRequester() && serviceRequest.getRequester().hasDisplay()) {
            requester = serviceRequest.getRequester().getDisplay();
        }

        if (serviceRequest.hasAuthoredOn()) {
            authoredOn = serviceRequest.getAuthoredOn();
        }

        if (serviceRequest.hasReasonCode()) {
            reasons = getConceptNamesFromCodeableConcept(serviceRequest.getReasonCode());

        } else if (serviceRequest.hasReasonReference()) {
            reasons = getDisplayValuesFromReferences(serviceRequest.getReasonReference());
        }

        if (serviceRequest.hasNote()) {
            notes = buildNotes(serviceRequest.getNote());
        }
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return code != null ?
                code :
                "(No description)";
    }

    public String getRequester() {
        return requester;
    }

    public Date getAuthoredOn() {
        return authoredOn;
    }

    public List<String> getReasons() {
        return reasons;
    }

    public List<String> getNotes() {
        return notes;
    }
}
