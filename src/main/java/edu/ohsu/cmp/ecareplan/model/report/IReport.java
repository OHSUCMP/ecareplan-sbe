package edu.ohsu.cmp.ecareplan.model.report;

public interface IReport {
    String getTitle(boolean redact);
    String getBody(boolean redact);
}
