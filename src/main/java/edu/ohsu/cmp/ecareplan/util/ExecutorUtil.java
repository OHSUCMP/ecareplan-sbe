package edu.ohsu.cmp.ecareplan.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ExecutorUtil {
    private static final Logger logger = LoggerFactory.getLogger(ExecutorUtil.class);

    // adapted from https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/util/concurrent/ExecutorService.html
    public static void shutdownAndAwaitTermination(ExecutorService pool, long timeout) {
        if (pool == null) return;

        pool.shutdown();

        try {
            if ( ! pool.awaitTermination(timeout, TimeUnit.SECONDS) ) {
                pool.shutdownNow();
                if ( ! pool.awaitTermination(timeout, TimeUnit.SECONDS) )
                    logger.error("ExecutorService pool did not terminate");
            }

        } catch (InterruptedException ex) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
