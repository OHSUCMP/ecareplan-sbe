package edu.ohsu.cmp.ecareplan.service;

import edu.ohsu.cmp.ecareplan.model.report.IReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

public class ReportService {
    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);


    @Value("#{new Boolean('${report.enabled}')}")
    private Boolean enabled;

    @Value("#{'${report.email-list-unredacted.csv}'.split(',')}")
    private List<String> unredactedEmailList;

    @Value("#{'${report.email-list-redacted.csv}'.split(',')}")
    private List<String> redactedEmailList;

    public Boolean isEnabled() {
        return enabled;
    }

    public void sendReport(IReport report) {
        if (enabled) {
            // todo : finish this
        }
    }
}
