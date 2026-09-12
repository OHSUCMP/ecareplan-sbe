package edu.ohsu.cmp.ecareplan.model.report;

import edu.ohsu.cmp.ecareplan.entity.AuditData;
import edu.ohsu.cmp.ecareplan.entity.UserEndpoint;
import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import edu.ohsu.cmp.ecareplan.model.dataset.PatientModel;
import edu.ohsu.cmp.ecareplan.task.ShareTask;
import jakarta.annotation.Nullable;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EndpointSyncReport implements IReport {
    private static final Pattern RESOURCE_FROM_CREATED_DETAILS_PATTERN = Pattern.compile(ShareTask.AUDIT_DETAILS_CREATED_PREFIX + "\\s+([A-Za-z]+)/.+");
    private static final DateFormat DATE_FORMAT = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);


    private final UserEndpoint userEndpoint;
    private final PatientModel patientModel;
    private final List<AuditData> auditDataList;
    private final Map<String, Integer> resourceCreationCountMap;

    private Integer errorCount;
    private Integer creationCount;

    public EndpointSyncReport(UserEndpoint userEndpoint, @Nullable PatientModel patientModel, List<AuditData> auditDataList) {
        this.userEndpoint = userEndpoint;
        this.patientModel = patientModel;
        this.auditDataList = auditDataList;

        resourceCreationCountMap = new TreeMap<>();

        errorCount = 0;
        creationCount = 0;
        for (AuditData ad : auditDataList) {
            if (ad.getSeverity().equals(AuditSeverity.ERROR)) {
                errorCount++;
            }
            if (ad.getEvent().equals(ShareTask.AUDIT_EVENT_SHARE) && ad.getDetails().startsWith(ShareTask.AUDIT_DETAILS_CREATED_PREFIX)) {
                creationCount++;
                Matcher matcher = RESOURCE_FROM_CREATED_DETAILS_PATTERN.matcher(ad.getDetails());
                if (matcher.find()) {
                    String resourceType = matcher.group(1);
                    resourceCreationCountMap.put(resourceType, resourceCreationCountMap.getOrDefault(resourceType, 0) + 1);
                }
            }
        }
    }

    @Override
    public String getTitle() {
        String identifier = patientModel != null ?
                patientModel.getName() + " (User #" + userEndpoint.getUser().getId() + ")" :
                "User #" + userEndpoint.getUser().getId();

        return "Endpoint Sync Report for " + identifier + " from " + userEndpoint.getEndpoint().getName();
    }


    @Override
    public String getBody() {
        StringBuilder sb = new StringBuilder();

        // todo : ideally this would be in HTML, with a plaintext fallback if the appropriate MIME type isn't supported

        sb.append("<h1>Endpoint Sync Report (generated ").append(DATE_FORMAT.format(new Date())).append(")</h1>\n");

        sb.append("<p>User: <strong>").append(userEndpoint.getUser().getId()).append("</strong></p>\n");
        if (patientModel != null) {
            sb.append("<p>Name: <strong>").append(patientModel.getName()).append("</strong></p>\n");
        }
        sb.append("<p>Source: <strong>").append(userEndpoint.getEndpoint().getName()).append("</strong></p>\n");
        sb.append("<p># of resources created in the SDS: <strong>").append(creationCount).append("</strong></p>\n");
        sb.append("<p># of errors encountered: <strong>").append(errorCount).append("</strong></p>\n");

        if (creationCount > 0) {
            sb.append("<br/>\n<p>Resources created:\n<ul>");
            for (Map.Entry<String, Integer> entry : resourceCreationCountMap.entrySet()) {
                sb.append("<li>").append(entry.getKey()).append(": ").append(entry.getValue()).append("</li>\n");
            }
            sb.append("</ul></p>\n");
        }

        if (errorCount > 0) {
            sb.append("<br/><p>Errors encountered:\n<ul>");
            for (AuditData ad : auditDataList) {
                if (ad.getSeverity().equals(AuditSeverity.ERROR)) {
                    sb.append("<li>[").append(ad.getId()).append("] ").append(ad.getSeverity()).append(": ").append(ad.getEvent()).append(": ")
                            .append(ad.getDetails()).append(" (").append(DATE_FORMAT.format(ad.getCreated())).append(")</li>\n");
                }
            }
            sb.append("</ul></p>\n");
        }

        return sb.toString();
    }
}
