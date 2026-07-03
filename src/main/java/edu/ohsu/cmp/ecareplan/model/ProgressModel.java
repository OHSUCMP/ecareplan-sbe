package edu.ohsu.cmp.ecareplan.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProgressModel {
    private final String id;
    private final String label;
    private ProgressStatus status;
    private String message;
    private Integer percentComplete;
    private final List<String> errors = new ArrayList<>();

    public ProgressModel(String label, ProgressStatus status, String message, Integer percentComplete) {
        id = UUID.randomUUID().toString();
        this.label = label;
        this.status = status;
        this.message = message;

        if (percentComplete == null || percentComplete < 0 || percentComplete > 100) {
            throw new IllegalArgumentException("Percent complete must be between 0 and 100");
        }
        this.percentComplete = percentComplete;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public ProgressStatus getStatus() {
        return status;
    }

    public void setStatus(ProgressStatus status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getPercentComplete() {
        return percentComplete;
    }

    public void setPercentComplete(Integer percentComplete) {
        if (percentComplete == null || percentComplete < 0 || percentComplete > 100) {
            throw new IllegalArgumentException("Percent complete must be between 0 and 100");
        }
        this.percentComplete = percentComplete;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void addError(String error) {
        errors.add(error);
    }
}
