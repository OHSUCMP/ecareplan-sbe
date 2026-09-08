package edu.ohsu.cmp.ecareplan.task;

import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.gclient.IUpdateExecutable;
import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.model.ProgressStatus;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSet;
import edu.ohsu.cmp.ecareplan.model.dataset.PatientModel;
import edu.ohsu.cmp.ecareplan.model.progress.ShareProgressModel;
import edu.ohsu.cmp.ecareplan.service.AuditService;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShareTaskTest {
    private final Endpoint endpoint = new Endpoint();
    private final PatientModel model = mock(PatientModel.class);
    private final IGenericClient client = mock(IGenericClient.class, RETURNS_DEEP_STUBS);
    private final AuditService audit = mock(AuditService.class);
    private final Patient patient = new Patient();
    private ShareProgressModel progress;

    @BeforeEach
    void setup() {
        endpoint.setName("test endpoint");
        endpoint.setIss("https://example.test/fhir");
        patient.setId("Patient/123");
        when(model.getId()).thenReturn("Patient/123");
        when(model.toResourceForSDSExport()).thenReturn(patient);
        progress = new ShareProgressModel(DataSet.PATIENT, endpoint, ProgressStatus.WAITING_TO_START, 0, 1);
    }

    @Test
    void exhaustedRetriesFailTheCallable() {
        MethodOutcome outcome = new MethodOutcome();
        outcome.setResponseStatusCode(500);
        var update = stubUpdate();
        when(update.execute()).thenReturn(outcome);
        assertThrows(IllegalStateException.class, () -> task(List.of(model)).getCallable().call());
        verify(update, times(10)).execute();
        assertEquals(1, progress.getErrors().size());
    }

    @Test
    void interruptionStopsBeforeExportOrHttpRequests() {
        Thread.currentThread().interrupt();
        try {
            assertThrows(InterruptedException.class, () -> task(List.of(model)).getCallable().call());
            assertTrue(Thread.currentThread().isInterrupted());
            assertEquals(0, progress.getCurrent());
            assertFalse(progress.getErrors().isEmpty());
            verify(model, never()).toResourceForSDSExport();
            verifyNoInteractions(client);
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void taskKeepsItsOwnResourceList() throws Exception {
        MethodOutcome outcome = new MethodOutcome();
        outcome.setResponseStatusCode(200);
        when(stubUpdate().execute()).thenReturn(outcome);
        var resources = new ArrayList<>(List.of(model));
        ShareTask task = task(resources);
        resources.clear();
        task.getCallable().call();
        verify(model).toResourceForSDSExport();
        assertEquals(1, progress.getCurrent());
        assertEquals(ProgressStatus.COMPLETED, progress.getStatus());
    }

    private ShareTask task(List<PatientModel> resources) {
        return new ShareTask("session", DataSet.PATIENT, endpoint, client, resources, progress, audit);
    }

    private IUpdateExecutable stubUpdate() {
        IUpdateExecutable update = mock(IUpdateExecutable.class);
        when(client.update().resource(patient).withId("Patient/123")).thenReturn(update);
        when(update.withAdditionalHeader("X-Partition-Name", endpoint.getIss())).thenReturn(update);
        return update;
    }
}
