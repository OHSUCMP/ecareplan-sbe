package edu.ohsu.cmp.ecareplan.task;

import ca.uhn.fhir.rest.server.exceptions.AuthenticationException;
import ca.uhn.fhir.rest.server.exceptions.ForbiddenOperationException;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.entity.User;
import edu.ohsu.cmp.ecareplan.entity.UserEndpoint;
import edu.ohsu.cmp.ecareplan.exception.CaseNotHandledException;
import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import edu.ohsu.cmp.ecareplan.model.ProgressStatus;
import edu.ohsu.cmp.ecareplan.model.dataset.BaseDataSetModel;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSet;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSetBuilderRequestConfiguration;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentials;
import edu.ohsu.cmp.ecareplan.model.progress.EndpointReadProgressModel;
import edu.ohsu.cmp.ecareplan.service.AuditService;
import edu.ohsu.cmp.ecareplan.service.EndpointService;
import edu.ohsu.cmp.ecareplan.service.IDataSetBuilder;
import edu.ohsu.cmp.ecareplan.service.SDSService;
import edu.ohsu.cmp.ecareplan.workspace.UserWorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.*;

public class DataSetPopulationTask implements ITask<Void> {
    private static final Logger logger = LoggerFactory.getLogger(DataSetPopulationTask.class);

    private final String sessionId;
    private final boolean loadFromEndpoint;
    private final DataSet<?> dataSet;
    private final DataSetBuilderRequestConfiguration cfg;
    private final FHIRCredentials launchCredentials;
    private final EndpointReadProgressModel progress;
    private final UserWorkspaceService userWorkspaceService;
    private final EndpointService endpointService;
    private final SDSService sdsService;
    private final AuditService auditService;

    public DataSetPopulationTask(String sessionId, boolean loadFromEndpoint, DataSet<?> dataSet,
                                 DataSetBuilderRequestConfiguration cfg,
                                 FHIRCredentials launchCredentials, EndpointReadProgressModel progress,
                                 UserWorkspaceService userWorkspaceService, EndpointService endpointService, SDSService sdsService, AuditService auditService) {

        this.sessionId = sessionId;
        this.loadFromEndpoint = loadFromEndpoint;
        this.dataSet = dataSet;
        this.cfg = cfg;
        this.launchCredentials = launchCredentials;
        this.progress = progress;
        this.userWorkspaceService = userWorkspaceService;
        this.endpointService = endpointService;
        this.sdsService = sdsService;
        this.auditService = auditService;
    }

    @Override
    public String getDescription() {
        return "DataSetPopulationTask(sessionId=" + sessionId +
                ", userId=" + cfg.userEndpoint().getUser().getId() +
                ", dataSet=" + dataSet.getName() +
                ", endpoint=" + cfg.userEndpoint().getEndpoint().getName() +
                ", loadFromEndpoint=" + loadFromEndpoint + ")";
    }

    @Override
    public Callable<Void> getCallable() {
        return () -> {
            final long start = System.currentTimeMillis();
            final User user = cfg.userEndpoint().getUser();
            final Endpoint endpoint = cfg.userEndpoint().getEndpoint();

            logger.info("BEGIN populating {} from {} for session={}, userId={}", dataSet.getName(),
                    endpoint.getName(), sessionId, user.getId());

            List<? extends BaseDataSetModel<?>> resources = List.of();
            Future<Void> sdsFuture = null;
            try {
                try {
                    checkInterrupted();
                    progress.setStatus(dataSet, ProgressStatus.RUNNING);
                    invalidateCache(dataSet, endpoint);

                    if (loadFromEndpoint) {
                        resources = getDataSetModelsForEndpoint(dataSet, cfg, endpointService);
                        checkInterrupted();
                        sdsFuture = sdsService.shareToSDS(sessionId, dataSet, endpoint, launchCredentials, resources);

                    } else {
                        resources = getDataSetModelsForEndpoint(dataSet, cfg, sdsService);
                    }

                } catch (Exception e) {
                    String endpointNameForLogging = ! loadFromEndpoint ?
                            "SDS for " + endpoint.getName() :
                            endpoint.getName();

                    logger.error("caught {} populating {} from {} for session={} - {}", e.getClass().getSimpleName(), dataSet.getName(),
                            endpointNameForLogging, sessionId, e.getMessage(), e);
                    auditService.doAudit(user, AuditSeverity.ERROR, "endpoint population",
                            "caught " + e.getClass().getSimpleName() + " populating " + dataSet.getName() + " from " +
                                    endpointNameForLogging + " - " + e.getMessage());
                    progress.addError(dataSet, e.getMessage());

                    if (e instanceof ForbiddenOperationException && ! loadFromEndpoint) {
                        // user can't access their SDS records that the app seems to think they have
                        // maybe the SDS was reset?
                        // in any case, it probably makes sense to just clear their lastSyncCompleted timestamp and abort this attempt
                        endpointService.clearUserEndpointLastSyncCompleted(cfg.userEndpoint());
                        auditService.doAudit(user, AuditSeverity.WARN, "endpoint population",
                                "cleared SDS lastSyncCompleted timestamp and aborting population for " + endpoint.getName());
                    }

                    throw e;

                } finally {
                    addToCache(dataSet, endpoint, resources);
                }

                long runtime = System.currentTimeMillis() - start;
                logger.info("DONE populating {} from {} for session={}, userId={} (took {} ms)", dataSet.getName(),
                        endpoint.getName(), sessionId, user.getId(), runtime);

                // wait for all child SDS tasks to complete
                if (sdsFuture != null) {
                    try {
                        sdsFuture.get();

                    } catch (InterruptedException e) {
                        sdsFuture.cancel(true);
                        logger.error("Interrupted while sharing data to SDS", e);
                        Thread.currentThread().interrupt();
                        throw e;

                    } catch (ExecutionException | CancellationException e) {
                        logger.error("Failed while sharing data to SDS", e);
                        progress.addError(dataSet, "Failed while sharing data to SDS: " + e.getMessage());
                        throw e;
                    }
                }

                return null;
            } finally {
                if (sdsFuture != null && !sdsFuture.isDone()) {
                    sdsFuture.cancel(true);
                }
            }
        };
    }

    private static void checkInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Dataset population interrupted");
        }
    }

    private void addToCache(DataSet<?> dataSet, Endpoint endpoint, List<? extends BaseDataSetModel<?>> resources) {
        var workspace = userWorkspaceService.getIfPresent(sessionId);
        if (workspace != null) {
            workspace.addToCache(dataSet, endpoint, resources);
        }
    }

    private void invalidateCache(DataSet<?> dataSet, Endpoint endpoint) {
        var workspace = userWorkspaceService.getIfPresent(sessionId);
        if (workspace != null) {
            workspace.invalidateCache(dataSet, endpoint);
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends BaseDataSetModel<?>> List<T> getDataSetModelsForEndpoint(DataSet<?> dataSet, DataSetBuilderRequestConfiguration cfg, IDataSetBuilder dataSetBuilder) {
        final UserEndpoint userEndpoint = cfg.userEndpoint();

        List<? extends BaseDataSetModel<?>> list = null;
        try {
            final long start = System.currentTimeMillis();

            if (DataSet.PATIENT.equals(dataSet)) {
                list = dataSetBuilder.buildPatients(cfg);
            } else if (DataSet.CARE_PLANS.equals(dataSet)) {
                list = dataSetBuilder.buildCarePlans(cfg);
            } else if (DataSet.CARE_TEAMS.equals(dataSet)) {
                list = dataSetBuilder.buildCareTeams(cfg);
            } else if (DataSet.CLINICAL_NOTES.equals(dataSet)) {
                list = dataSetBuilder.buildClinicalNotes(cfg);
            } else if (DataSet.CONDITIONS.equals(dataSet)) {
                list = dataSetBuilder.buildConditions(cfg);
            } else if (DataSet.DIAGNOSTIC_REPORTS.equals(dataSet)) {
                list = dataSetBuilder.buildDiagnosticReports(cfg);
            } else if (DataSet.ENCOUNTERS.equals(dataSet)) {
                list = dataSetBuilder.buildEncounters(cfg);
            } else if (DataSet.GOALS.equals(dataSet)) {
                list = dataSetBuilder.buildGoals(cfg);
            } else if (DataSet.IMMUNIZATIONS.equals(dataSet)) {
                list = dataSetBuilder.buildImmunizations(cfg);
            } else if (DataSet.LAB_RESULTS.equals(dataSet)) {
                list = dataSetBuilder.buildLabResults(cfg);
            } else if (DataSet.MEDICATIONS.equals(dataSet)) {
                list = dataSetBuilder.buildMedications(cfg);
            } else if (DataSet.PROCEDURES.equals(dataSet)) {
                list = dataSetBuilder.buildProcedures(cfg);
            } else if (DataSet.QUESTIONNAIRE_RESPONSES.equals(dataSet)) {
                list = dataSetBuilder.buildQuestionnaireResponses(cfg);
            } else if (DataSet.SERVICE_REQUESTS.equals(dataSet)) {
                list = dataSetBuilder.buildServiceRequests(cfg);
            } else if (DataSet.SOCIAL_HISTORIES.equals(dataSet)) {
                list = dataSetBuilder.buildSocialHistories(cfg);
            } else if (DataSet.SURVEY_OBSERVATIONS.equals(dataSet)) {
                list = dataSetBuilder.buildSurveyObservations(cfg);
            } else if (DataSet.VITALS.equals(dataSet)) {
                list = dataSetBuilder.buildVitals(cfg);
            } else {
                throw new CaseNotHandledException("Case not handled for data set: " + dataSet.getName());
            }

            if (dataSetBuilder instanceof EndpointService) {
                auditService.doAudit(userEndpoint.getUser(), AuditSeverity.INFO, "cache population", "got " + list.size() +
                        " resource(s) for dataSet=" + dataSet.getName() + " from " + userEndpoint.getEndpoint().getName() +
                        " (took " + (System.currentTimeMillis() - start) + "ms)");
            }

        } catch (Exception e) {
            final String endpointNameForLogging = dataSetBuilder instanceof SDSService ?
                    "SDS for " + userEndpoint.getEndpoint().getName() :
                    userEndpoint.getEndpoint().getName();

            if (e instanceof ForbiddenOperationException foe) {
                logger.error("attempt to retrieve {} from {} was forbidden - {}",
                        dataSet.getName(), endpointNameForLogging, foe.getMessage());

                if (DataSet.PATIENT.equals(dataSet)) {
                    logger.error("Patient is required for system operation; aborting -");
                    throw foe;

                } else {
                    auditService.doAudit(userEndpoint.getUser(), AuditSeverity.ERROR, "cache population", "retrieving " + dataSet.getName() +
                            " from " + endpointNameForLogging + " was forbidden");
                    progress.addError(dataSet, foe.getMessage());
                }

            } else if (e instanceof InvalidRequestException ire) {
                logger.error("attempt to retrieve {} from {} triggered an InvalidRequestException - {}",
                        dataSet.getName(), endpointNameForLogging, ire.getMessage());

                if (DataSet.PATIENT.equals(dataSet)) {
                    logger.error("Patient is required for system operation; aborting -");
                    throw ire;

                } else {
                    auditService.doAudit(userEndpoint.getUser(), AuditSeverity.ERROR, "cache population", "invalid request retrieving " +
                            dataSet.getName() + " from " + endpointNameForLogging);
                    progress.addError(dataSet, e.getMessage());
                }

            } else if (e instanceof AuthenticationException ae) {
                // access token expired
                // handle gracefully if possible, otherwise abort
                throw ae;

            } else if (e instanceof RuntimeException re) {
                throw re;

            } else {
                throw new RuntimeException(e);
            }
        }

        return list != null ?
                (List<T>) list :
                List.of();
    }
}
