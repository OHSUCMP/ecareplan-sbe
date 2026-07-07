package edu.ohsu.cmp.ecareplan.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ProgressModel {
    private final String id;
    private final String label;
    private ProgressStatus status;
    private String message;
    private Integer current;
    private final Integer max;
    private final List<String> errors = new ArrayList<>();

    public ProgressModel(String label, ProgressStatus status, String message, Integer current, Integer max) {
        id = UUID.randomUUID().toString();
        this.label = label;
        this.status = status;
        this.message = message;
        if (current == null || max == null) {
            throw new IllegalArgumentException("Current and max cannot be null");
        }
        if (max < 0) {
            throw new IllegalArgumentException("Max cannot be negative");
        }
        if (current < 0 || current > max) {
            throw new IllegalArgumentException("Current cannot be null or greater than max");
        }
        this.current = current;
        this.max = max;
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
        if (max == 0) {
            return 100;
        } else {
            return max == 100 ?
                    current :
                    Math.round(current * 100 / (float) max);
        }
    }

    public void setCurrent(Integer current) {
        if (current == null) {
            throw new IllegalArgumentException("Current cannot be null");
        } else if (current < 0 || current > max) {
            throw new IllegalArgumentException("Current cannot be negative or greater than max");
        }
        this.current = current;
    }

    public Integer getCurrent() {
        return current;
    }

    public void incrementCurrent() {
        if (current >= max) {
            throw new IllegalArgumentException("Current cannot be incremented beyond max");
        }
        this.current++;
    }

    public Integer getMax() {
        return max;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void addError(String error) {
        errors.add(error);
    }
}
