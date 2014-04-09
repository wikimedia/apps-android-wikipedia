package org.wikipedia.concurrency;

import java.util.*;
import java.util.concurrent.*;

/**
 * Service registry for providing Executors for various purposes.
 *
 * TODO: Make number of threads per executor configurable.
 */
public class ExecutorService {
    private static final ExecutorService SINGLETON = new ExecutorService();

    public static ExecutorService getSingleton() {
        return SINGLETON;
    }

    private final HashMap<Class, Executor> executors;

    public ExecutorService() {
        executors = new HashMap<Class, Executor>();
    }

    public Executor getExecutor(Class cls, int threads) {
        if (!executors.containsKey(cls)) {
            executors.put(cls, new ScheduledThreadPoolExecutor(threads));
        }
        return executors.get(cls);
    }
}
