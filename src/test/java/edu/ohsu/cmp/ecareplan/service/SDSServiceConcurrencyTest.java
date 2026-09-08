package edu.ohsu.cmp.ecareplan.service;

import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.model.ProgressStatus;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSet;
import edu.ohsu.cmp.ecareplan.model.dataset.PatientModel;
import edu.ohsu.cmp.ecareplan.model.fhir.FHIRCredentials;
import edu.ohsu.cmp.ecareplan.model.progress.ShareProgressModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Timeout(20)
class SDSServiceConcurrencyTest {
    private final SDSService service = new SDSService();
    private final BackgroundTaskService background = mock(BackgroundTaskService.class);
    private final Endpoint endpoint = new Endpoint();
    private final FHIRCredentials credentials = mock(FHIRCredentials.class);
    private final List<PatientModel> resources = List.of(mock(PatientModel.class));

    @BeforeEach
    void setup() {
        endpoint.setId(1L);
        endpoint.setName("test endpoint");
        endpoint.setIss("https://example.test/fhir");
        when(credentials.getBearerToken()).thenReturn("test-token");
        ReflectionTestUtils.setField(service, "backgroundTaskService", background);
        ReflectionTestUtils.setField(service, "sdsFhirEndpointUrl", "https://sds.example.test/fhir");

        // the FhirContext is built by @PostConstruct, which nothing runs when the service is
        // constructed directly, so stand in for Spring here
        ReflectionTestUtils.setField(service, "socketTimeout", 1000);
        ReflectionTestUtils.setField(service, "poolMaxTotal", BaseDataSetBuilderService.DEFAULT_POOL_MAX_TOTAL);
        ReflectionTestUtils.setField(service, "poolMaxPerRoute", BaseDataSetBuilderService.DEFAULT_POOL_MAX_PER_ROUTE);
        ReflectionTestUtils.setField(service, "connectionRequestTimeout", BaseDataSetBuilderService.DEFAULT_CONNECTION_REQUEST_TIMEOUT);
        ReflectionTestUtils.invokeMethod(service, "initFhirContext");

        when(background.submit(any())).thenAnswer(invocation -> new CompletableFuture<Void>());
    }

    @Test
    void concurrentDuplicateSharesReturnTheSamePublishedFuture() throws Exception {
        try (var callers = Executors.newVirtualThreadPerTaskExecutor()) {
            var start = new CountDownLatch(1);
            var results = new ArrayList<Future<Future<Void>>>();
            for (int i = 0; i < 32; i++) {
                results.add(callers.submit(() -> {
                    start.await();
                    return share(DataSet.PATIENT);
                }));
            }
            start.countDown();
            Future<Void> expected = results.getFirst().get(10, TimeUnit.SECONDS);
            assertNotNull(expected);
            for (Future<Future<Void>> result : results) {
                assertSame(expected, result.get(10, TimeUnit.SECONDS));
            }
            verify(background, times(1)).submit(any());
            assertEquals(1, service.getCurrentProgress("session", DataSet.PATIENT).size());
        }
    }

    @Test
    void differentDatasetsRemainRegisteredDuringConcurrentInitialization() throws Exception {
        try (var callers = Executors.newVirtualThreadPerTaskExecutor()) {
            var start = new CountDownLatch(1);
            Future<?> first = callers.submit(() -> { start.await(); return share(DataSet.PATIENT); });
            Future<?> second = callers.submit(() -> { start.await(); return share(DataSet.CARE_PLANS); });
            start.countDown();
            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);
            assertEquals(2, service.getCurrentProgress("session", endpoint).size());
            verify(background, times(2)).submit(any());
        }
    }

    @Test
    void completedStatusDoesNotPermitDuplicateUntilCallableExits() {
        Future<Void> first = share(DataSet.PATIENT);
        ShareProgressModel progress = (ShareProgressModel) service.getCurrentProgress("session", DataSet.PATIENT).getFirst();
        progress.setStatus(ProgressStatus.COMPLETED);
        service.clearAllCompletedProgress("session");
        assertSame(first, share(DataSet.PATIENT));
        verify(background, times(1)).submit(any());
        ((CompletableFuture<Void>) first).complete(null);
        assertNotSame(first, share(DataSet.PATIENT));
        verify(background, times(2)).submit(any());
    }

    @Test
    void rejectedSubmissionDoesNotLeaveAnUnstartedShareRegistered() {
        when(background.submit(any())).thenThrow(new java.util.concurrent.RejectedExecutionException());
        assertThrows(java.util.concurrent.RejectedExecutionException.class, () -> share(DataSet.PATIENT));
        assertNull(service.getCurrentProgress("session", DataSet.PATIENT));
    }

    @Test
    void emptyShareCompletesWithoutSubmittingWork() {
        assertNull(service.shareToSDS("session", DataSet.PATIENT, endpoint, credentials, List.of()));
        assertEquals(ProgressStatus.COMPLETED, service.getCurrentProgress("session", DataSet.PATIENT).getFirst().getStatus());
        verifyNoInteractions(background);
    }

    private Future<Void> share(DataSet<?> dataSet) {
        return service.shareToSDS("session", dataSet, endpoint, credentials, resources);
    }
}
