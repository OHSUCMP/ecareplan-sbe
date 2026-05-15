package edu.ohsu.cmp.ecareplan.model;

import java.util.HashMap;
import java.util.Map;

public enum DataSetName {
    ENCOUNTERS("encounters"),
    CLINICAL_NOTES("clinicalNotes"),
    CONDITIONS("conditions"),
    VITALS("vitals"),
    CARE_TEAM("careTeam"),
    PROCEDURES_TIME("proceduresTime"),
    PROCEDURES_COUNT("proceduresCount"),
    MEDICATION_REQUEST_ACTIVE("medicationRequestActive"),
    MEDICATION_REQUEST_INACTIVE("medicationRequestInactive"),
    SERVICE_REQUEST("serviceRequest"),
    CARE_PLAN("carePlan"),
    GOAL("goal"),
    DIAGNOSTIC_REPORT("diagnosticReport"),
    IMMUNIZATION("immunization"),
    LAB_RESULTS("labResults"),
    EGFR_EXTRA_LAB_RESULTS("egfrExtraLabResults"),
    SOCIAL_HISTORY("socialHistory"),
    QUESTIONNAIRE_RESPONSE("questionnaireResponse"),
    SURVEY_OBSERVATIONS("surveyObservations");

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
