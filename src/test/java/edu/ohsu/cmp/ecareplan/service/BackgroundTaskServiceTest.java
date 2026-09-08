package edu.ohsu.cmp.ecareplan.service;

import edu.ohsu.cmp.ecareplan.task.ITask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@Timeout(15)
class BackgroundTaskServiceTest {
    private final BackgroundTaskService service = new BackgroundTaskService();

    @AfterEach
    void shutdown() {
        service.shutdown();
    }

    @Test
    void usesADistinctVirtualThreadForEachCallable() throws Exception {
        Thread first = service.submit(task(Thread::currentThread)).get(5, TimeUnit.SECONDS);
        Thread second = service.submit(task(Thread::currentThread)).get(5, TimeUnit.SECONDS);
        assertTrue(first.isVirtual());
        assertTrue(second.isVirtual());
        assertNotSame(first, second);
        assertTrue(first.getName().startsWith("background-task-"));
    }

    @Test
    void nestedTasksCanAllStartWhileTheirParentsWait() throws Exception {
        int count = 64;
        CountDownLatch childrenStarted = new CountDownLatch(count);
        var parents = new ArrayList<Future<Boolean>>();
        for (int i = 0; i < count; i++) {
            parents.add(service.submit(task(() -> service.submit(task(() -> {
                childrenStarted.countDown();
                return childrenStarted.await(5, TimeUnit.SECONDS) && Thread.currentThread().isVirtual();
            })).get(10, TimeUnit.SECONDS))));
        }
        for (Future<Boolean> parent : parents) {
            assertTrue(parent.get(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void cancellationInterruptsRunningCallable() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        Future<Void> future = service.submit(task(() -> {
            started.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException e) {
                interrupted.countDown();
                throw e;
            }
            return null;
        }));
        assertTrue(started.await(5, TimeUnit.SECONDS));
        assertTrue(future.cancel(true));
        assertTrue(interrupted.await(5, TimeUnit.SECONDS));
    }

    @Test
    void shutdownInterruptsRunningTasksInsteadOfWaitingOutAGracePeriod() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        service.submit(task(() -> {
            started.countDown();
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException e) {
                interrupted.countDown();
                throw e;
            }
            return null;
        }));
        assertTrue(started.await(5, TimeUnit.SECONDS));

        long elapsedMillis = System.nanoTime();
        service.shutdown();
        elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - elapsedMillis);

        assertTrue(interrupted.await(5, TimeUnit.SECONDS));
        // a grace-period shutdown would have blocked for its full 10s before interrupting this task
        assertTrue(elapsedMillis < 5000, "shutdown took " + elapsedMillis + "ms; expected an immediate interrupt");
    }

    @Test
    void preservesExceptionsAndRejectsSubmissionsAfterShutdown() throws Exception {
        var failure = new IllegalStateException("test failure");
        Future<Void> future = service.submit(task(() -> { throw failure; }));
        assertSame(failure, assertThrows(ExecutionException.class,
                () -> future.get(5, TimeUnit.SECONDS)).getCause());
        service.shutdown();
        service.shutdown();
        assertThrows(RejectedExecutionException.class, () -> service.submit(task(() -> null)));
    }

    private static <T> ITask<T> task(Callable<T> callable) {
        return new ITask<>() {
            public String getDescription() { return "test task"; }
            public Callable<T> getCallable() { return callable; }
        };
    }
}
