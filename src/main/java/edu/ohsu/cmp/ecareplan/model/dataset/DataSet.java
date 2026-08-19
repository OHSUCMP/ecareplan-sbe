package edu.ohsu.cmp.ecareplan.model.dataset;

import java.util.*;

public final class DataSet<T extends BaseDataSetModel<?>> {
    public static final DataSet<PatientModel> PATIENT = new DataSet<>("PATIENT", PatientModel.class, 1);
    public static final DataSet<CarePlanModel> CARE_PLANS = new DataSet<>("CARE_PLANS", CarePlanModel.class, 3);
    public static final DataSet<CareTeamModel> CARE_TEAMS = new DataSet<>("CARE_TEAMS", CareTeamModel.class, 2);
    public static final DataSet<ClinicalNoteModel> CLINICAL_NOTES = new DataSet<>("CLINICAL_NOTES", ClinicalNoteModel.class, 4);
    public static final DataSet<ConditionModel> CONDITIONS = new DataSet<>("CONDITIONS", ConditionModel.class, 2);
    public static final DataSet<DiagnosticReportModel> DIAGNOSTIC_REPORTS = new DataSet<>("DIAGNOSTIC_REPORTS", DiagnosticReportModel.class, 3);
    public static final DataSet<EncounterModel> ENCOUNTERS = new DataSet<>("ENCOUNTERS", EncounterModel.class, 2);
    public static final DataSet<GoalModel> GOALS = new DataSet<>("GOALS", GoalModel.class, 2);
    public static final DataSet<ImmunizationModel> IMMUNIZATIONS = new DataSet<>("IMMUNIZATIONS", ImmunizationModel.class, 2);
    public static final DataSet<LabResultModel> LAB_RESULTS = new DataSet<>("LAB_RESULTS", LabResultModel.class, 2);
    public static final DataSet<MedicationModel> MEDICATIONS = new DataSet<>("MEDICATIONS", MedicationModel.class, 2);
    public static final DataSet<ProcedureModel> PROCEDURES = new DataSet<>("PROCEDURES", ProcedureModel.class, 3);
    public static final DataSet<QuestionnaireResponseModel> QUESTIONNAIRE_RESPONSES = new DataSet<>("QUESTIONNAIRE_RESPONSES", QuestionnaireResponseModel.class, 2);
    public static final DataSet<ServiceRequestModel> SERVICE_REQUESTS = new DataSet<>("SERVICE_REQUESTS", ServiceRequestModel.class, 2);
    public static final DataSet<SocialHistoryModel> SOCIAL_HISTORIES = new DataSet<>("SOCIAL_HISTORIES", SocialHistoryModel.class, 3);
    public static final DataSet<SurveyObservationModel> SURVEY_OBSERVATIONS = new DataSet<>("SURVEY_OBSERVATIONS", SurveyObservationModel.class, 3);
    public static final DataSet<VitalsModel> VITALS = new DataSet<>("VITALS", VitalsModel.class, 2);

    public static final Map<String, DataSet<?>> DATASET_MAP = new LinkedHashMap<>();
    static {
        DATASET_MAP.put(PATIENT.getName(), PATIENT);
        DATASET_MAP.put(CARE_PLANS.getName(), CARE_PLANS);
        DATASET_MAP.put(CARE_TEAMS.getName(), CARE_TEAMS);
        DATASET_MAP.put(CLINICAL_NOTES.getName(), CLINICAL_NOTES);
        DATASET_MAP.put(CONDITIONS.getName(), CONDITIONS);
        DATASET_MAP.put(DIAGNOSTIC_REPORTS.getName(), DIAGNOSTIC_REPORTS);
        DATASET_MAP.put(ENCOUNTERS.getName(), ENCOUNTERS);
        DATASET_MAP.put(GOALS.getName(), GOALS);
        DATASET_MAP.put(IMMUNIZATIONS.getName(), IMMUNIZATIONS);
        DATASET_MAP.put(LAB_RESULTS.getName(), LAB_RESULTS);
        DATASET_MAP.put(MEDICATIONS.getName(), MEDICATIONS);
        DATASET_MAP.put(PROCEDURES.getName(), PROCEDURES);
        DATASET_MAP.put(QUESTIONNAIRE_RESPONSES.getName(), QUESTIONNAIRE_RESPONSES);
        DATASET_MAP.put(SERVICE_REQUESTS.getName(), SERVICE_REQUESTS);
        DATASET_MAP.put(SOCIAL_HISTORIES.getName(), SOCIAL_HISTORIES);
        DATASET_MAP.put(SURVEY_OBSERVATIONS.getName(), SURVEY_OBSERVATIONS);
        DATASET_MAP.put(VITALS.getName(), VITALS);
    }

    public static final List<DataSet<?>> ALL_DATASETS = new ArrayList<>(DATASET_MAP.values());

    public static DataSet<?> getDataSet(String name) {
        if (DATASET_MAP.containsKey(name)) {
            return DATASET_MAP.get(name);
        } else {
            throw new IllegalArgumentException("Unknown dataset: " + name);
        }
    }

    public static final List<DataSet<?>> ALL_DATASETS_BY_PRIORITY;
    static {
        ALL_DATASETS_BY_PRIORITY = new ArrayList<>(ALL_DATASETS);
        ALL_DATASETS_BY_PRIORITY.sort(Comparator.comparingInt(DataSet::getPriority));
    }

    private final String name;
    private final Class<T> modelClass;
    private final int priority;

    private DataSet(String name, Class<T> modelClass, int priority) {
        this.name = name;
        this.modelClass = modelClass;
        this.priority = priority;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }

    public Class<T> getModelClass() {
        return modelClass;
    }

    public int getPriority() {
        return priority;
    }
}
