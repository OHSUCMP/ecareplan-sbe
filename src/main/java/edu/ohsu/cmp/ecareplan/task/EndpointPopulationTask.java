package edu.ohsu.cmp.ecareplan.task;

import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSet;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSetBuilderRequestConfiguration;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentials;
import edu.ohsu.cmp.ecareplan.model.progress.EndpointReadProgressModel;
import edu.ohsu.cmp.ecareplan.service.AuditService;
import edu.ohsu.cmp.ecareplan.service.BackgroundTaskService;
import edu.ohsu.cmp.ecareplan.service.EndpointService;
import edu.ohsu.cmp.ecareplan.service.SDSService;
import edu.ohsu.cmp.ecareplan.workspace.UserWorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class EndpointPopulationTask implements ITask<Void> {
    private static final Logger logger = LoggerFactory.getLogger(EndpointPopulationTask.class);

    private final String sessionId;
    private final boolean loadFromEndpoint;
    private final boolean doShareOperations;
    private final DataSetBuilderRequestConfiguration cfg;
    private final FHIRCredentials launchCredentials;
    private final EndpointReadProgressModel progress;
    private final UserWorkspaceService userWorkspaceService;
    private final EndpointService endpointService;
    private final SDSService sdsService;
    private final BackgroundTaskService backgroundTaskService;
    private final AuditService auditService;

    public EndpointPopulationTask(String sessionId, boolean loadFromEndpoint, boolean doShareOperations,
                                  DataSetBuilderRequestConfiguration cfg,
                                  FHIRCredentials launchCredentials, EndpointReadProgressModel progress,
                                  UserWorkspaceService userWorkspaceService, EndpointService endpointService, SDSService sdsService,
                                  BackgroundTaskService backgroundTaskService, AuditService auditService) {

        this.sessionId = sessionId;
        this.loadFromEndpoint = loadFromEndpoint;
        this.doShareOperations = doShareOperations;
        this.cfg = cfg;
        this.launchCredentials = launchCredentials;
        this.progress = progress;
        this.userWorkspaceService = userWorkspaceService;
        this.endpointService = endpointService;
        this.sdsService = sdsService;
        this.backgroundTaskService = backgroundTaskService;
        this.auditService = auditService;
    }

    @Override
    public String getDescription() {
        return "EndpointPopulationTask(sessionId=" + sessionId +
                ", userId=" + cfg.userEndpoint().getUser().getId() +
                ", endpoint=" + cfg.userEndpoint().getEndpoint().getName() +
                ", loadFromEndpoint=" + loadFromEndpoint + "," +
                ", doShareOperations=" + doShareOperations + ")";
    }

    @Override
    public Callable<Void> getCallable() {
        return () -> {
            final long start = System.currentTimeMillis();
            final Endpoint endpoint = cfg.userEndpoint().getEndpoint();
            final List<Future<Void>> dataSetFutures = new ArrayList<>();

            logger.info("BEGIN populating {} for session={}", endpoint.getName(), sessionId);

            notifyEndpointPopulationStarted(endpoint);

            try {
                for (DataSet<?> dataSet : DataSet.ALL_DATASETS_BY_PRIORITY) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException("Endpoint population interrupted");
                    }

                    DataSetPopulationTask task = new DataSetPopulationTask(sessionId,
                            loadFromEndpoint, doShareOperations, dataSet, cfg, launchCredentials, progress,
                            userWorkspaceService, endpointService, sdsService, auditService);
                    Future<Void> future = backgroundTaskService.submit(task);
                    dataSetFutures.add(future);
                }

                Exception failure = null;
                for (Future<Void> dataSetFuture : dataSetFutures) {
                    try {
                        dataSetFuture.get();
                    } catch (ExecutionException | CancellationException e) {
                        // A failed child must not leave other children running after completion.
                        logger.error("Failed while populating dataSet", e);
                        if (failure == null) {
                            failure = e;
                        }
                    }
                }
                if (failure != null) {
                    throw failure;
                }

                if (loadFromEndpoint && progress.getErrors().isEmpty()) {
                    logger.info("Successfully shared all data from {} to SDS", endpoint.getName());
                    endpointService.updateUserEndpointLastSyncCompleted(cfg.userEndpoint());
                }

            } catch (InterruptedException e) {
                logger.info("Interrupted while populating {} for session={} - {}", endpoint.getName(), sessionId, e.getMessage());
                Thread.currentThread().interrupt();
                throw e;

            } finally {
                for (Future<Void> future : dataSetFutures) {
                    if (!future.isDone()) {
                        future.cancel(true);
                    }
                }

                long runtime = System.currentTimeMillis() - start;
                logger.info("DONE populating {} for session={} (took {} ms)", endpoint.getName(), sessionId, runtime);

                notifyEndpointPopulationComplete(endpoint);
                notifyIfAllComplete();

                if ( ! userWorkspaceService.exists(sessionId) ) {
                    // user logged out during population, flush any accumulated progress data from the SDS service
                    sdsService.clearAllCompletedProgress(sessionId);
                }
            }

            return null;
        };
    }

    private void notifyEndpointPopulationStarted(Endpoint endpoint) {
        var workspace = userWorkspaceService.getIfPresent(sessionId);
        if (workspace != null) {
            workspace.notifyEndpointPopulationStarted(endpoint);
        }
    }

    private void notifyEndpointPopulationComplete(Endpoint endpoint) {
        var workspace = userWorkspaceService.getIfPresent(sessionId);
        if (workspace != null) {
            workspace.notifyEndpointPopulationComplete(endpoint);
        }
    }

    private void notifyIfAllComplete() {
        var workspace = userWorkspaceService.getIfPresent(sessionId);
        if (workspace != null) {
            workspace.notifyIfAllComplete();
        }
    }
}
