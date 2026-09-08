package edu.ohsu.cmp.ecareplan.model.progress;

import edu.ohsu.cmp.ecareplan.entity.Endpoint;
import edu.ohsu.cmp.ecareplan.model.ProgressStatus;
import edu.ohsu.cmp.ecareplan.model.dataset.DataSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(15)
class ProgressConcurrencyTest {
    @Test
    void progressCanBeReadWhileAllDatasetsUpdateIt() throws Exception {
        Endpoint endpoint = new Endpoint();
        endpoint.setName("test endpoint");
        var progress = new EndpointReadProgressModel(endpoint, false);
        var share = new ShareProgressModel(DataSet.PATIENT, endpoint, ProgressStatus.RUNNING, 0, 1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var start = new CountDownLatch(1);
            var futures = new ArrayList<Future<?>>();
            for (DataSet<?> dataSet : DataSet.ALL_DATASETS_BY_PRIORITY) {
                futures.add(executor.submit(() -> {
                    start.await();
                    for (int i = 0; i < 100; i++) {
                        progress.setStatus(dataSet, ProgressStatus.RUNNING);
                        progress.addError(dataSet, "test error");
                        share.addError("test error");
                        progress.setStatus(dataSet, ProgressStatus.COMPLETED);
                    }
                    return null;
                }));
            }
            futures.add(executor.submit(() -> {
                start.await();
                for (int i = 0; i < 1000; i++) {
                    progress.getMessage();
                    progress.getErrors();
                    progress.getDataSetReadProgressModel(DataSet.PATIENT);
                    share.getErrors();
                    share.getMessage();
                }
                return null;
            }));
            start.countDown();
            for (Future<?> future : futures) future.get(10, TimeUnit.SECONDS);
        }
        assertEquals(ProgressStatus.COMPLETED, progress.getStatus());
        assertEquals(100 * DataSet.ALL_DATASETS_BY_PRIORITY.size(), progress.getErrors().size());
        assertEquals(progress.getErrors().size(), share.getErrors().size());
        var snapshot = progress.getErrors(DataSet.PATIENT);
        progress.addError(DataSet.PATIENT, "later error");
        assertEquals(100, snapshot.size());
        var shareSnapshot = share.getErrors();
        share.addError("later error");
        assertEquals(100 * DataSet.ALL_DATASETS_BY_PRIORITY.size(), shareSnapshot.size());
    }
}
