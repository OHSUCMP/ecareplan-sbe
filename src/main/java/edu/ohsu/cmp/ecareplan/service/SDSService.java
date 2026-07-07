package edu.ohsu.cmp.ecareplan.service;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.model.ProgressModel;
import edu.ohsu.cmp.ecareplan.model.ProgressStatus;
import edu.ohsu.cmp.ecareplan.model.dataset.BaseDataSetModel;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSet;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentialsWithClient;
import edu.ohsu.cmp.ecareplan.util.ExecutorUtil;
import edu.ohsu.cmp.ecareplan.util.FhirUtil;
import edu.ohsu.cmp.ecareplan.workspace.UserWorkspace;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class SDSService extends BaseService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final int POOL_SIZE = 5;

    @Value("${socket.timeout:300000}")
    private Integer socketTimeout;

    @Value("${sds.fhirEndpointUrl}")
    private String sdsFhirEndpointUrl;

    @Autowired
    private EndpointService endpointService;

    private final ExecutorService executorService;
    private final Map<String, Map<String, ProgressModel>> sessionIdProgressMap;

    public SDSService() {
        executorService = Executors.newFixedThreadPool(POOL_SIZE);
        sessionIdProgressMap = Collections.synchronizedMap(new HashMap<>());
    }

    public void shutdown() {
        ExecutorUtil.shutdownAndAwaitTermination(executorService, 60);
        sessionIdProgressMap.clear();
    }

    public List<ProgressModel> getCurrentProgress(String sessionId) {
        return sessionIdProgressMap.containsKey(sessionId) ?
                new ArrayList<>(sessionIdProgressMap.get(sessionId).values()) :
                null;
    }

    public void clearCompletedProgress(String sessionId) {
        if (sessionIdProgressMap.containsKey(sessionId)) {
            Iterator<ProgressModel> iter = sessionIdProgressMap.get(sessionId).values().iterator();
            while (iter.hasNext()) {
                ProgressModel pm = iter.next();
                if (pm.getStatus() == ProgressStatus.COMPLETED) {
                    iter.remove();
                }
            }
            if (sessionIdProgressMap.get(sessionId).isEmpty()) {
                sessionIdProgressMap.remove(sessionId);
            }
        }
    }

    public void shareToSDS(String sessionId, DataSet<?> dataSet, Endpoint endpoint) {
        if ( ! sessionIdProgressMap.containsKey(sessionId) ) {
            sessionIdProgressMap.put(sessionId, Collections.synchronizedMap(new LinkedHashMap<>()));
        }

        if (sessionIdProgressMap.get(sessionId).containsKey(dataSet.getName())) {
            ProgressModel progress = sessionIdProgressMap.get(sessionId).get(dataSet.getName());
            switch (progress.getStatus()) {
                case INITIALIZING:
                case RUNNING:
                    logger.warn("Sharing of {} for session={} is already in progress", dataSet.getName(), sessionId);
                    return;
                case COMPLETED:
                    sessionIdProgressMap.get(sessionId).remove(dataSet.getName());
            }
        }

        Endpoint patientLaunchEndpoint = endpointService.getPatientLaunchEndpoint();
        UserWorkspace workspace = userWorkspaceService.get(sessionId);
        FHIRCredentialsWithClient fcc = workspace.getCredentialsWithClientForEndpoint(patientLaunchEndpoint);
        IGenericClient client = FhirUtil.buildClient(sdsFhirEndpointUrl, fcc.getCredentials().getBearerToken(), socketTimeout);

        List<? extends BaseDataSetModel<?>> list = workspace.getDataSetModelsForEndpoint(dataSet, endpoint);

        ProgressStatus status;
        String message;
        if (list.isEmpty()) {
            status = ProgressStatus.COMPLETED;
            message = "No data to share.";
        } else {
            status = ProgressStatus.INITIALIZING;
            message = "Initializing...";
        }

        final ProgressModel progress = new ProgressModel("Sharing " + dataSet.getName() + " resources to SDS", status, message, 0, list.size());;

        sessionIdProgressMap.get(sessionId).put(dataSet.getName(), progress);

        if (ProgressStatus.COMPLETED.equals(progress.getStatus())) {
            return;
        }

        Runnable shareRunnable = new Runnable() {
            @Override
            public void run() {
                if (ProgressStatus.INITIALIZING.equals(progress.getStatus())) {
                    progress.setStatus(ProgressStatus.RUNNING);
                    progress.setMessage("Processing...");
                }

                final int maxAttempts = 10;

                for (BaseDataSetModel<?> item : list) {
                    try {
                        final IDomainResource resource = item.toResourceForSDSExport();
                        String id = FhirUtil.toRelativeReference(resource.getId());

                        int attempt = 0;
                        boolean success = false;
                        while ( ! success && attempt++ < maxAttempts ) {
                            if (attempt > 1) {
                                logger.info("Re-attempting share of {} with id={} from {} for session {} ({}/{})",
                                        resource.getClass().getSimpleName(), id, endpoint.getName(), sessionId, attempt, maxAttempts);
                            }

                            try {
                                MethodOutcome outcome = client.update()
                                        .resource(resource)
                                        .withId(id)
                                        .withAdditionalHeader("X-Partition-Name", endpoint.getIss())
                                        .execute();

                                success = outcome.getResponseStatusCode() >= 200 && outcome.getResponseStatusCode() < 300;
                                if (success) {
                                    logger.info("Successfully shared {} with id={} from {} for session={}",
                                            resource.getClass().getSimpleName(), id, endpoint.getName(), sessionId);

                                } else {
                                    logger.debug("Failed sharing {} with id={} from {} with status code {} ({}/{})",
                                            resource.getClass().getSimpleName(), id, endpoint.getName(),
                                            outcome.getResponseStatusCode(), attempt, maxAttempts);
                                }

                            } catch (Exception e) {
                                logger.debug("caught {} sharing {} with id={} from {} for session={} - {}", e.getClass().getSimpleName(),
                                        resource.getClass().getSimpleName(), id, endpoint.getName(), sessionId, e.getMessage());
                            }
                        }

                        if ( ! success ) {
                            progress.addError("Failed to share " + resource.getClass().getSimpleName() + " with id=" + id);
                        }

                    } catch (Exception e) {
                        logger.error("caught {} sharing {} with id={} from {} for session={} - {}", e.getClass().getSimpleName(),
                                dataSet.getName(), item.getId(), endpoint.getName(), sessionId, e.getMessage());
                        logger.debug(e.getMessage(), e);

                    } finally {
                        if (progress.getCurrent() < progress.getMax()) {
                            progress.incrementCurrent();
                        }

                        if (progress.getCurrent().equals(progress.getMax())) {
                            progress.setStatus(ProgressStatus.COMPLETED);
                            progress.setMessage("Sharing complete.");
                        }
                    }
                }

                if ( ! progress.getCurrent().equals(progress.getMax()) ) {
                    logger.warn("somehow got through all list items for " + dataSet.getName() +
                            " from " + endpoint.getName() + ", but progress current != max?  that's weird.  investigate?");
                    progress.setStatus(ProgressStatus.COMPLETED);
                    progress.setMessage("Sharing complete.");
                }
            }
        };

        executorService.submit(shareRunnable);
    }
}
