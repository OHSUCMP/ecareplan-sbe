package edu.ohsu.cmp.ecareplan.model.dataset;

import java.util.HashMap;
import java.util.Map;

public enum DataSetName {
    ASSESSMENTS("assessments"),
    CARE_PLAN("carePlan"),
    CARE_TEAM("careTeam"),
    CLINICAL_NOTES("clinicalNotes"),
    CONCERNS("concerns"), // conditions
    DIAGNOSTIC_REPORTS("diagnosticReports"),
    GOALS("goals"),
    IMMUNIZATIONS("immunizations"),
    INTERACTIONS("interactions"), // encounters
    MEDICATIONS("medications"), // all active plus the last 10 inactive
    PROCEDURES("procedures"),
    SERVICE_REQUESTS("serviceRequests"),
    SOCIAL_HISTORY("socialHistory"),
    SURVEY_OBSERVATIONS("surveyObservations"),
    TESTS("tests"), // standard lab results plus EGFR
    VITALS("vitals");

    private static final Map<String, DataSetName> TAG_MAP = new HashMap<String, DataSetName>();
    static {
        for (DataSetName dsn : values()) {
            TAG_MAP.put(dsn.tag, dsn);
        }
    }

    public static DataSetName fromTag(String tag) {
        return TAG_MAP.get(tag);
    }

    private final String tag;

    DataSetName(String tag) {
        this.tag = tag;
    }

    public String getTag() {
        return tag;
    }
}
