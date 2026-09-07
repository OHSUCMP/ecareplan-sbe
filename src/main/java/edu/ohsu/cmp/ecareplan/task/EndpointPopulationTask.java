package edu.ohsu.cmp.ecareplan.task;

import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.entity.User;
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
    private final DataSetBuilderRequestConfiguration cfg;
    private final FHIRCredentials launchCredentials;
    private final EndpointReadProgressModel progress;
    private final UserWorkspaceService userWorkspaceService;
    private final EndpointService endpointService;
    private final SDSService sdsService;
    private final BackgroundTaskService backgroundTaskService;
    private final AuditService auditService;

    public EndpointPopulationTask(String sessionId, boolean loadFromEndpoint, DataSetBuilderRequestConfiguration cfg,
                                  FHIRCredentials launchCredentials, EndpointReadProgressModel progress,
                                  UserWorkspaceService userWorkspaceService, EndpointService endpointService, SDSService sdsService,
                                  BackgroundTaskService backgroundTaskService, AuditService auditService) {

        this.sessionId = sessionId;
        this.loadFromEndpoint = loadFromEndpoint;
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
                ", loadFromEndpoint=" + loadFromEndpoint + ")";
    }

    @Override
    public Callable<Void> getCallable() {
        return new Callable<>() {
            @Override
            public Void call() {
                final long start = System.currentTimeMillis();
                final User user = cfg.userEndpoint().getUser();
                final Endpoint endpoint = cfg.userEndpoint().getEndpoint();
                final List<Future<Void>> dataSetFutures = new ArrayList<>();

                logger.info("BEGIN populating endpoint={} for session={}", endpoint.getName(), sessionId);

                notifyEndpointPopulationStarted(endpoint);

                try {
                    for (DataSet<?> dataSet : DataSet.ALL_DATASETS_BY_PRIORITY) {
                        DataSetPopulationTask task = new DataSetPopulationTask(sessionId, loadFromEndpoint, dataSet,
                                cfg, launchCredentials, progress,
                                userWorkspaceService, endpointService, sdsService, auditService);
                        Future<Void> future = backgroundTaskService.submit(task);
                        dataSetFutures.add(future);
                    }

                } finally {
                    try {
                        for (Future<Void> dataSetFuture : dataSetFutures) { // wait for all dataSet tasks to complete
                            dataSetFuture.get();
                        }

                        if (loadFromEndpoint) {
                            logger.info("Successfully shared all data from {} to SDS", endpoint.getName());
                            endpointService.updateUserEndpointLastSyncCompleted(cfg.userEndpoint());
                        }

                    } catch (InterruptedException e) {
                        logger.error("Interrupted while populating dataSet", e);
                        Thread.currentThread().interrupt();

                    } catch (ExecutionException | CancellationException e) {
                        logger.error("Failed while populating dataSet", e);
                    }
                }

                long runtime = System.currentTimeMillis() - start;
                logger.info("DONE populating endpoint={} for session={} (took {} ms)", endpoint.getName(), sessionId, runtime);

                notifyEndpointPopulationComplete(endpoint);
                notifyIfAllComplete();

                if ( ! userWorkspaceService.exists(sessionId) ) {
                    // user logged out during population, flush any accumulated progress data from the SDS service
                    sdsService.clearAllCompletedProgress(sessionId);
                }

                return null;
            }
        };
    }

    private void notifyEndpointPopulationStarted(Endpoint endpoint) {
        if (userWorkspaceService.exists(sessionId)) {
            userWorkspaceService.get(sessionId).notifyEndpointPopulationStarted(endpoint);
        }
    }

    private void notifyEndpointPopulationComplete(Endpoint endpoint) {
        if (userWorkspaceService.exists(sessionId)) {
            userWorkspaceService.get(sessionId).notifyEndpointPopulationComplete(endpoint);
        }
    }

    private void notifyIfAllComplete() {
        if (userWorkspaceService.exists(sessionId)) {
            userWorkspaceService.get(sessionId).notifyIfAllComplete();
        }
    }
}
