package edu.ohsu.cmp.ecareplan.task;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.exceptions.FhirClientConnectionException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.model.AuditSeverity;
import edu.ohsu.cmp.ecareplan.model.ProgressStatus;
import edu.ohsu.cmp.ecareplan.model.dataset.BaseDataSetModel;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSet;
import edu.ohsu.cmp.ecareplan.model.progress.ShareProgressModel;
import edu.ohsu.cmp.ecareplan.service.AuditService;
import edu.ohsu.cmp.ecareplan.util.FhirUtil;
import org.hl7.fhir.instance.model.api.IDomainResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Callable;

public class ShareTask implements ITask<Void> {
    private static final Logger logger = LoggerFactory.getLogger(ShareTask.class);

    public static final String AUDIT_EVENT_SHARE = "share to SDS";

    private static final String PARTITION_HEADER = "X-Partition-Name";

    // deliberately short: backoff is applied per resource, so a dataset where every resource
    // fails pays this on all of them.  enough to stop hammering the SDS and to give interruption
    // a gap to land in, without turning one bad dataset into hours of sleeping
    private static final long BACKOFF_BASE_MILLIS = 100L;
    private static final long BACKOFF_MAX_MILLIS = 400L;

    private final String sessionId;
    private final DataSet<?> dataSet;
    private final Endpoint endpoint;
    private final IGenericClient client;
    private final List<? extends BaseDataSetModel<?>> resources;
    private final ShareProgressModel progress;
    private final AuditService auditService;

    public ShareTask(String sessionId, DataSet<?> dataSet, Endpoint endpoint, IGenericClient client,
                     List<? extends BaseDataSetModel<?>> resources, ShareProgressModel progress,
                     AuditService auditService) {
        this.sessionId = sessionId;
        this.dataSet = dataSet;
        this.endpoint = endpoint;
        this.client = client;
        this.resources = List.copyOf(resources);
        this.progress = progress;
        this.auditService = auditService;
    }

    @Override
    public String getDescription() {
        return "ShareTask(sessionId=" + sessionId +
                ", dataSet=" + dataSet.getName() +
                ", endpoint=" + endpoint.getName() +
                ", resourcesCount=" + resources.size() + ")";
    }

    @Override
    public Callable<Void> getCallable() {
        return () -> {
            final long start = System.currentTimeMillis();

            logger.info("BEGIN sharing {} {} resources from {} to SDS for session={}", resources.size(),
                    dataSet.getName(), endpoint.getName(), sessionId);

            final int maxAttempts = 10;

            try {
                checkInterrupted();
                progress.setStatus(ProgressStatus.RUNNING);

                for (BaseDataSetModel<?> item : resources) {
                    checkInterrupted();
                    try {
                        final IDomainResource resource = item.toResourceForSDSExport();
                        final String id = FhirUtil.toRelativeReference(resource.getId());

                        int attempt = 0;
                        boolean success = false;
                        while ( ! success && attempt++ < maxAttempts ) {
                            checkInterrupted();
                            if (attempt > 1) {
                                // backoff introduced to mitigate retry storms and facilitate interruption
                                Thread.sleep(backoffMillis(attempt));

                                logger.info("Re-attempting share of {} from {} for session={} ({}/{})",
                                        id, endpoint.getName(), sessionId, attempt, maxAttempts);
                            }

                            try {
                                MethodOutcome outcome = client.update()
                                        .resource(resource)
                                        .withId(id)
                                        .withAdditionalHeader(PARTITION_HEADER, endpoint.getIss())
                                        .execute();

                                int code = outcome.getResponseStatusCode();
                                if (code == 200) {
                                    logger.debug("Successfully shared {} from {} for session={} (code={})", id, endpoint.getName(), sessionId, code);
                                    success = true;

                                } else if (code == 201) {
                                    logger.info("Successfully shared {} from {} for session={} (code={})", id, endpoint.getName(), sessionId, code);

                                    auditService.doAudit(sessionId, AuditSeverity.INFO, AUDIT_EVENT_SHARE, "created " + id + " from " + endpoint.getName());

                                    success = true;

                                } else if (code >= 400) {
                                    // initial failures at this point we only want to appear in debug logs
                                    logger.debug("Failed sharing {} from {} with status code {} ({}/{})",
                                            id, endpoint.getName(), outcome.getResponseStatusCode(), attempt, maxAttempts);

                                } else {
                                    logger.warn("Received unexpected response code {} sharing {} from {} for session={}", code, id, endpoint.getName(), sessionId);

                                    auditService.doAudit(sessionId, AuditSeverity.WARN, AUDIT_EVENT_SHARE, "received unexpected response code " + code +
                                            " sharing " + id + " from " + endpoint.getName());

                                    success = (code > 201 && code < 300);
                                }

                            } catch (FhirClientConnectionException fcce) {
                                // Connection refused
                                throw fcce;

                            } catch (ResourceNotFoundException rnfe) {
                                // HTTP 404 Not Found - generally thrown if the SDS can't introspect
                                throw rnfe;

                            } catch (Exception e) {
                                if (Thread.currentThread().isInterrupted()) {
                                    // interrupting a virtual thread blocked on socket I/O closes the
                                    // socket, so this is the interrupt arriving, not a share failure.
                                    // rethrow so shutdown doesn't log an error per in-flight resource
                                    throw e;
                                }

                                if (attempt < maxAttempts) {
                                    logger.warn("caught {} sharing {} from {} for session={} - {} (attempt {} of {}) - retrying -",
                                            e.getClass().getSimpleName(), id, endpoint.getName(), sessionId, e.getMessage(), attempt, maxAttempts);
                                } else {
                                    logger.error("caught {} sharing {} from {} for session={} - {}", e.getClass().getSimpleName(),
                                            id, endpoint.getName(), sessionId, e.getMessage());
                                }
                                logger.debug(e.getMessage(), e);
                            }
                        }

                        if ( ! success ) {
                            logger.error("failed to share {} from {} for session={} after {} attempts", id, endpoint.getName(), sessionId, maxAttempts);
                            auditService.doAudit(sessionId, AuditSeverity.ERROR, AUDIT_EVENT_SHARE, "failed to share " + id + " from " + endpoint.getName());
                            progress.addError("Failed to share " + id);
                        }

                    } catch (Exception e) {
                        if (e instanceof InterruptedException || Thread.currentThread().isInterrupted()) {
                            Thread.currentThread().interrupt();
                            throw new InterruptedException("Sharing data to SDS interrupted");
                        }
                        final String id = FhirUtil.toRelativeReference(item.getId());

                        logger.error("caught {} sharing {} from {} for session={} - {}", e.getClass().getSimpleName(),
                                id, endpoint.getName(), sessionId, e.getMessage());
                        logger.debug(e.getMessage(), e);

                        auditService.doAudit(sessionId, AuditSeverity.ERROR, AUDIT_EVENT_SHARE,
                                "caught " + e.getClass().getSimpleName() + " sharing " + id + " from " + endpoint.getName());

                        progress.addError("caught " + e.getClass().getSimpleName() + " sharing " + id + " from " + endpoint.getName());

                        if (e instanceof FhirClientConnectionException fcce) {
                            throw fcce;

                        } else if (e instanceof ResourceNotFoundException rnfe) {
                            throw rnfe;
                        }

                    } finally {
                        if (progress.getCurrent() < progress.getTotal()) {
                            progress.setCurrent(progress.getCurrent() + 1);
                        }
                    }
                }

                long runtime = System.currentTimeMillis() - start;
                logger.info("DONE sharing {} {} resources from {} to SDS for session={} (took {} ms)", resources.size(),
                        dataSet.getName(), endpoint.getName(), sessionId, runtime);

                if (!progress.getErrors().isEmpty()) {
                    throw new IllegalStateException("Failed to share all " + dataSet.getName() + " resources to SDS");
                }
                return null;
            } catch (InterruptedException e) {
                progress.addError("Sharing data to SDS interrupted");
                Thread.currentThread().interrupt();
                throw e;
            } finally {
                progress.setStatus(ProgressStatus.COMPLETED);
            }
        };
    }

    private static void checkInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Sharing data to SDS interrupted");
        }
    }

    // doubling per re-attempt, capped.  attempt is 1-based and this is only called when
    // attempt > 1, so the first re-attempt waits BACKOFF_BASE_MILLIS
    private static long backoffMillis(int attempt) {
        return Math.min(BACKOFF_BASE_MILLIS << Math.min(attempt - 2, 20), BACKOFF_MAX_MILLIS);
    }
}
