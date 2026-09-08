package edu.ohsu.cmp.ecareplan.task;

import java.util.concurrent.Callable;

public interface ITask<T> {
    String getDescription();

    /**
     * Work executed on its own virtual thread by BackgroundTaskService.
     * Implementations must protect shared mutable state and honor interruption.
     */
    Callable<T> getCallable();
}
