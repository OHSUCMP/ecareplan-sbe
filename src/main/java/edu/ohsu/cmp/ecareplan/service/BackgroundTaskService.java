package edu.ohsu.cmp.ecareplan.service;

import edu.ohsu.cmp.ecareplan.task.ITask;
import edu.ohsu.cmp.ecareplan.util.ExecutorUtil;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class BackgroundTaskService {
    private static final Logger logger = LoggerFactory.getLogger(BackgroundTaskService.class);

    private final ExecutorService executorService;
    private final AtomicBoolean shutdown = new AtomicBoolean(false);

    public BackgroundTaskService() {
        // Parent tasks wait for children submitted to this same executor. A thread per
        // task avoids pool starvation, and virtual threads release carriers while waiting.
        executorService = Executors.newThreadPerTaskExecutor(
                Thread.ofVirtual().name("background-task-", 0).factory());
    }

    @PreDestroy
    public void shutdown() {
        if ( ! shutdown.compareAndSet(false, true) ) {
            logger.debug("BackgroundTaskService is already shut down");
            return;
        }

        ExecutorUtil.shutdownAndAwaitTermination(executorService, 10);
    }


    public <T> Future<T> submit(ITask<T> task) {
        logger.info("Submitting task : {}", task.getDescription());
        return executorService.submit(task.getCallable());
    }
}
