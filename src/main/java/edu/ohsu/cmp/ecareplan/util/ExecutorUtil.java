package edu.ohsu.cmp.ecareplan.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ExecutorUtil {
    private static final Logger logger = LoggerFactory.getLogger(ExecutorUtil.class);

    public static void shutdownNowAndAwaitTermination(ExecutorService pool, long timeout) {
        if (pool == null) return;

        pool.shutdownNow();

        try {
            if ( ! pool.awaitTermination(timeout, TimeUnit.SECONDS) )
                logger.error("ExecutorService pool did not terminate");

        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
