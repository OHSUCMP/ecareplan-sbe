package edu.ohsu.cmp.ecareplan.task;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IUpdateExecutable;
import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.entity.User;
import edu.ohsu.cmp.ecareplan.entity.UserEndpoint;
import edu.ohsu.cmp.ecareplan.model.ProgressStatus;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSet;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSetBuilderRequestConfiguration;
import edu.ohsu.cmp.ecareplan.model.dataset.PatientModel;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentials;
import edu.ohsu.cmp.ecareplan.model.progress.EndpointReadProgressModel;
import edu.ohsu.cmp.ecareplan.model.progress.ShareProgressModel;
import edu.ohsu.cmp.ecareplan.service.*;
import edu.ohsu.cmp.ecareplan.workspace.UserWorkspace;
import edu.ohsu.cmp.ecareplan.workspace.UserWorkspaceService;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Timeout(15)
class PopulationTaskTest {
    private final Endpoint endpoint = new Endpoint();
    private final UserEndpoint userEndpoint = new UserEndpoint();
    private final FHIRCredentials credentials = mock(FHIRCredentials.class);
    private final EndpointService endpoints = mock(EndpointService.class);
    private final SDSService sds = mock(SDSService.class);
    private final UserWorkspaceService workspaces = mock(UserWorkspaceService.class);
    private final AuditService audit = mock(AuditService.class);
    private final ReportService report = mock(ReportService.class);
    private DataSetBuilderRequestConfiguration cfg;
    private EndpointReadProgressModel progress;

    @BeforeEach
    void setup() {
        endpoint.setId(1L);
        endpoint.setName("test endpoint");
        endpoint.setIss("https://example.test/fhir");
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        userEndpoint.setUser(user);
        userEndpoint.setEndpoint(endpoint);
        cfg = new DataSetBuilderRequestConfiguration(userEndpoint, credentials, "123");
        progress = new EndpointReadProgressModel(endpoint, false);
    }

    @Test
    void actualEndpointDatasetAndShareCallablesRunOnVirtualThreads() throws Exception {
        var service = new BackgroundTaskService();
        try {
            PatientModel model = mock(PatientModel.class);
            Patient patient = new Patient();
            patient.setId("Patient/123");
            when(model.toResourceForSDSExport()).thenAnswer(invocation -> {
                assertTrue(Thread.currentThread().isVirtual());
                return patient;
            });
            when(endpoints.buildPatients(cfg)).thenAnswer(invocation -> {
                assertTrue(Thread.currentThread().isVirtual());
                return List.of(model);
            });
            IGenericClient client = mock(IGenericClient.class, RETURNS_DEEP_STUBS);
            MethodOutcome outcome = new MethodOutcome();
            outcome.setResponseStatusCode(200);
            IUpdateExecutable update = mock(IUpdateExecutable.class);
            when(client.update().resource(patient).withId("Patient/123")).thenReturn(update);
            when(update.withAdditionalHeader("X-Partition-Name", endpoint.getIss())).thenReturn(update);
            when(update.execute()).thenReturn(outcome);
            when(sds.shareToSDS(eq("session"), any(), eq(endpoint), eq(credentials), anyList()))
                    .thenAnswer(invocation -> {
                        List<PatientModel> resources = invocation.getArgument(4);
                        if (resources.isEmpty()) return null;
                        ShareProgressModel shareProgress = new ShareProgressModel(DataSet.PATIENT, endpoint,
                                ProgressStatus.WAITING_TO_START, 0, 1);
                        return service.submit(new ShareTask("session", DataSet.PATIENT, endpoint, client,
                                resources, shareProgress, audit));
                    });
            doAnswer(invocation -> {
                assertTrue(Thread.currentThread().isVirtual());
                return null;
            }).when(endpoints).updateUserEndpointLastSyncCompleted(userEndpoint);

            service.submit(endpointTask(service)).get(10, TimeUnit.SECONDS);
            verify(model).toResourceForSDSExport();
            verify(endpoints).updateUserEndpointLastSyncCompleted(userEndpoint);
        } finally {
            service.shutdown();
        }
    }

    @Test
    void endpointWaitsForRemainingChildrenAfterOneFails() throws Exception {
        BackgroundTaskService service = mock(BackgroundTaskService.class);
        Future<Void> remaining = mockFuture();
        when(remaining.isDone()).thenReturn(true);
        AtomicInteger submissions = new AtomicInteger();
        when(service.submit(any())).thenAnswer(invocation -> submissions.getAndIncrement() == 0
                ? CompletableFuture.failedFuture(new IllegalStateException("failed dataset")) : remaining);

        assertThrows(ExecutionException.class, () -> endpointTask(service).getCallable().call());
        verify(remaining, times(DataSet.ALL_DATASETS_BY_PRIORITY.size() - 1)).get();
        verify(endpoints, never()).updateUserEndpointLastSyncCompleted(any());
    }

    @Test
    void interruptedEndpointCancelsAllChildrenAndPreservesInterrupt() throws Exception {
        BackgroundTaskService service = mock(BackgroundTaskService.class);
        Future<Void> child = mockFuture();
        when(child.get()).thenThrow(new InterruptedException("cancelled"));
        when(service.submit(any())).thenAnswer(invocation -> child);
        try {
            assertThrows(InterruptedException.class, () -> endpointTask(service).getCallable().call());
            assertTrue(Thread.currentThread().isInterrupted());
            verify(child, times(DataSet.ALL_DATASETS_BY_PRIORITY.size())).cancel(true);
            verify(endpoints, never()).updateUserEndpointLastSyncCompleted(any());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void submissionFailureCancelsAlreadySubmittedChildren() {
        BackgroundTaskService service = mock(BackgroundTaskService.class);
        Future<Void> child = mockFuture();
        AtomicInteger submissions = new AtomicInteger();
        when(service.submit(any())).thenAnswer(invocation -> {
            if (submissions.getAndIncrement() == 0) return child;
            throw new RejectedExecutionException();
        });
        assertThrows(RejectedExecutionException.class, () -> endpointTask(service).getCallable().call());
        verify(child).cancel(true);
        verify(endpoints, never()).updateUserEndpointLastSyncCompleted(any());
    }

    @Test
    void interruptedDatasetCancelsShareAndPreservesInterrupt() throws Exception {
        Future<Void> share = mockFuture();
        when(share.get()).thenThrow(new InterruptedException("cancelled"));
        when(share.cancel(true)).thenAnswer(invocation -> {
            when(share.isDone()).thenReturn(true);
            return true;
        });
        when(sds.shareToSDS(anyString(), any(), any(), any(), anyList())).thenReturn(share);
        try {
            assertThrows(InterruptedException.class, () -> datasetTask().getCallable().call());
            assertTrue(Thread.currentThread().isInterrupted());
            verify(share).cancel(true);
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void datasetPropagatesShareFailure() {
        when(sds.shareToSDS(anyString(), any(), any(), any(), anyList()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("SDS failed")));
        assertThrows(ExecutionException.class, () -> datasetTask().getCallable().call());
        assertFalse(progress.getErrors().isEmpty());
    }

    @Test
    void failedReadCachesEmptyListAndPropagatesOriginalFailure() throws Exception {
        UserWorkspace workspace = mock(UserWorkspace.class);
        when(workspaces.getIfPresent("session")).thenReturn(workspace);
        var failure = new IllegalStateException("read failed");
        when(endpoints.buildPatients(cfg)).thenThrow(failure);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> datasetTask().getCallable().call()));
        verify(workspace).addToCache(DataSet.PATIENT, endpoint, List.of());
    }

    private EndpointPopulationTask endpointTask(BackgroundTaskService service) {
        return new EndpointPopulationTask("session", true, true, cfg, credentials, progress,
                workspaces, endpoints, sds, service, audit, report);
    }

    private DataSetPopulationTask datasetTask() {
        return new DataSetPopulationTask("session", true, true, DataSet.PATIENT, cfg, credentials,
                progress, workspaces, endpoints, sds, audit);
    }

    @SuppressWarnings("unchecked")
    private static Future<Void> mockFuture() {
        return mock(Future.class);
    }
}
