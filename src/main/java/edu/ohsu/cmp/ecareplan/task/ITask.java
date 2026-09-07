package edu.ohsu.cmp.ecareplan.task;

import java.util.concurrent.Callable;

public interface ITask<T> {
    String getDescription();
    Callable<T> getCallable();
}
