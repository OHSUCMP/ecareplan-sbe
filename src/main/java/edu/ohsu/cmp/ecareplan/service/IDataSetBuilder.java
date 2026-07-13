package edu.ohsu.cmp.ecareplan.service;

import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.exception.ConfigurationException;
import edu.ohsu.cmp.ecareplan.exception.DataException;
import edu.ohsu.cmp.ecareplan.model.dataset.*;

import java.io.IOException;
import java.util.List;

public interface IDataSetBuilder {
    List<PatientModel> buildPatients(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException;
    List<CarePlanModel> buildCarePlans(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException;
    List<CareTeamModel> buildCareTeams(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException;
    List<ClinicalNoteModel> buildClinicalNotes(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException;
    List<ConditionModel> buildConditions(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException;
    List<DiagnosticReportModel> buildDiagnosticReports(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException;
    List<EncounterModel> buildEncounters(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException;
    List<GoalModel> buildGoals(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException;
    List<ImmunizationModel> buildImmunizations(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException;
    List<LabResultModel> buildLabResults(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException;
    List<MedicationModel> buildMedications(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException;
    List<ProcedureModel> buildProcedures(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException;
    List<QuestionnaireResponseModel> buildQuestionnaireResponses(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException;
    List<ServiceRequestModel> buildServiceRequests(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException;
    List<SocialHistoryModel> buildSocialHistories(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException;
    List<SurveyObservationModel> buildSurveyObservations(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException;
    List<VitalsModel> buildVitals(String sessionId, Endpoint e) throws DataException, ConfigurationException, IOException;
}
