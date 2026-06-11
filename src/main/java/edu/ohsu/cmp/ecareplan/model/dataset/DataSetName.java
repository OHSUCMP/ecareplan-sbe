package edu.ohsu.cmp.ecareplan.model.dataset;

public enum DataSetName {
    PATIENT,
    ASSESSMENTS,
    CARE_PLANS,
    CARE_TEAMS,
    CLINICAL_NOTES,
    CONCERNS, // conditions
    DIAGNOSTIC_REPORTS,
    GOALS,
    IMMUNIZATIONS,
    INTERACTIONS, // encounters
    MEDICATIONS, // all active plus the last 10 inactive
    PROCEDURES,
    SERVICE_REQUESTS,
    SOCIAL_HISTORIES,
    SURVEY_OBSERVATIONS,
    TESTS, // standard lab results plus EGFR
    VITALS;
}
