package edu.ohsu.cmp.ecareplan.service;

import edu.ohsu.cmp.ecareplan.exception.ConfigurationException;
import edu.ohsu.cmp.ecareplan.exception.DataException;
import edu.ohsu.cmp.ecareplan.model.dataset.*;

import java.io.IOException;
import java.util.List;

public interface IDataSetBuilder {
    List<PatientModel> buildPatients(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException;
    List<CarePlanModel> buildCarePlans(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException;
    List<CareTeamModel> buildCareTeams(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException;
    List<ClinicalNoteModel> buildClinicalNotes(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException;
    List<ConditionModel> buildConditions(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException;
    List<DiagnosticReportModel> buildDiagnosticReports(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException;
    List<EncounterModel> buildEncounters(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException;
    List<GoalModel> buildGoals(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException;
    List<ImmunizationModel> buildImmunizations(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException;
    List<LabResultModel> buildLabResults(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException;
    List<MedicationModel> buildMedications(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException;
    List<ProcedureModel> buildProcedures(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException;
    List<QuestionnaireResponseModel> buildQuestionnaireResponses(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException;
    List<ServiceRequestModel> buildServiceRequests(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException;
    List<SocialHistoryModel> buildSocialHistories(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException;
    List<SurveyObservationModel> buildSurveyObservations(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException;
    List<VitalsModel> buildVitals(DataSetBuilderRequestConfiguration cfg) throws DataException, ConfigurationException, IOException;
}
