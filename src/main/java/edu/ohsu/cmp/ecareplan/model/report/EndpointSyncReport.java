package edu.ohsu.cmp.ecareplan.model.report;

import edu.ohsu.cmp.ecareplan.entity.AuditData;
import edu.ohsu.cmp.ecareplan.entity.UserEndpoint;
import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSet;
import edu.ohsu.cmp.ecareplan.model.dataset.PatientModel;
import edu.ohsu.cmp.ecareplan.task.DataSetPopulationTask;
import edu.ohsu.cmp.ecareplan.task.ShareTask;
import jakarta.annotation.Nullable;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EndpointSyncReport implements IReport {
    private static final Pattern COUNT_DATASET_FROM_GOT_DETAILS_PATTERN = Pattern.compile("got ([0-9]+) resource\\(s\\) for dataSet=([A-Z_]+)\\s.+");
    private static final Pattern RESOURCE_FROM_CREATED_DETAILS_PATTERN = Pattern.compile("created ([A-Za-z]+)/.+");
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    private final UserEndpoint userEndpoint;
    private final PatientModel patientModel;
    private final List<AuditData> auditDataList;
    private final Map<String, Integer> dataSetReadCountMap;
    private final Map<String, Integer> resourceCreationCountMap;

    private Integer readCount;
    private Integer creationCount;
    private Integer errorCount;

    public EndpointSyncReport(UserEndpoint userEndpoint, @Nullable PatientModel patientModel, List<AuditData> auditDataList) {
        this.userEndpoint = userEndpoint;
        this.patientModel = patientModel;
        this.auditDataList = auditDataList;

        dataSetReadCountMap = new TreeMap<>();
        resourceCreationCountMap = new TreeMap<>();

        readCount = 0;
        creationCount = 0;
        errorCount = 0;
        for (AuditData ad : auditDataList) {
            if (ad.getEvent().equals(DataSetPopulationTask.AUDIT_EVENT_CACHE_POPULATION) && ad.getSeverity().equals(AuditSeverity.INFO) &&
                    ad.getDetails().startsWith(DataSetPopulationTask.AUDIT_DETAILS_GOT_PREFIX)) {
                Matcher matcher = COUNT_DATASET_FROM_GOT_DETAILS_PATTERN.matcher(ad.getDetails());
                if (matcher.find()) {
                    int count = Integer.parseInt(matcher.group(1));
                    DataSet<?> dataSet = DataSet.getDataSet(matcher.group(2));
                    dataSetReadCountMap.put(dataSet.getDisplay(), count);
                    readCount += count;
                }
            }
            if (ad.getEvent().equals(ShareTask.AUDIT_EVENT_SHARE) && ad.getSeverity().equals(AuditSeverity.INFO) &&
                    ad.getDetails().startsWith(ShareTask.AUDIT_DETAILS_CREATED_PREFIX)) {
                creationCount++;
                Matcher matcher = RESOURCE_FROM_CREATED_DETAILS_PATTERN.matcher(ad.getDetails());
                if (matcher.find()) {
                    String resourceType = matcher.group(1);
                    resourceCreationCountMap.put(resourceType, resourceCreationCountMap.getOrDefault(resourceType, 0) + 1);
                }
            }
            if (ad.getSeverity().equals(AuditSeverity.ERROR)) {
                errorCount++;
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

        sb.append("<h1 style='text-decoration: underline;'>Endpoint Sync Report</h1>");
        sb.append("<p style='font-style:italic;'>Generated ").append(DATE_FORMAT.format(new Date())).append("</p>");

        sb.append("<h2 style='text-decoration: underline;'>Summary</h2>");
        sb.append("<p>");
        sb.append("User: <span style='font-weight: bold;'>").append(userEndpoint.getUser().getId()).append("</span><br/>");
        if (patientModel != null) {
            sb.append("Name: <span style='font-weight: bold;'>").append(patientModel.getName()).append("</span><br/>");
        }
        sb.append("Source: <span style='font-weight: bold;'>").append(userEndpoint.getEndpoint().getName()).append("</span><br/>");
        sb.append("# of resources read: <span style='font-weight: bold;'>").append(readCount).append("</span><br/>");
        sb.append("# of resources created in the SDS: <span style='font-weight: bold;'>").append(creationCount).append("</span><br/>");

        String errorCountDisplay = errorCount > 0 ?
                "<span style='color: red;'>" + errorCount + "</span>" :
                String.valueOf(errorCount);
        sb.append("# of errors encountered: <span style='font-weight: bold;'>").append(errorCountDisplay).append("</span><br/>");
        sb.append("</p>");

        if (readCount > 0) {
            sb.append("<h2 style='text-decoration: underline;'># Resources Read (by DataSet)</h2><ul>");
            for (Map.Entry<String, Integer> entry : dataSetReadCountMap.entrySet()) {
                sb.append("<li>").append(entry.getKey()).append(": <span style='font-weight: bold;'>").append(entry.getValue()).append("</span></li>");
            }
            sb.append("</ul>");
        }

        if (creationCount > 0) {
            sb.append("<h2 style='text-decoration: underline;'># Resources Created in the SDS</h2><ul>");
            for (Map.Entry<String, Integer> entry : resourceCreationCountMap.entrySet()) {
                sb.append("<li>").append(entry.getKey()).append(": <span style='font-weight: bold;'>").append(entry.getValue()).append("</span></li>");
            }
            sb.append("</ul>");
        }

        if (errorCount > 0) {
            sb.append("<h2 style='text-decoration: underline;'>Errors</h2><ul>");
            for (AuditData ad : auditDataList) {
                if (ad.getSeverity().equals(AuditSeverity.ERROR)) {
                    sb.append("<li><span style='color: red;'>").append(ad.getDetails()).append("</span></li>");
                }
            }
            sb.append("</ul>");
        }

        return sb.toString();
    }
}
