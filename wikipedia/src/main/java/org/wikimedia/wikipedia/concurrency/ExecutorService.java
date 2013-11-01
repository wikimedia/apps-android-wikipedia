package org.wikimedia.wikipedia.concurrency;

import java.util.HashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * Service registry for providing Executors for various purposes.
 *
 * TODO: Make number of threads per executor configurable.
 */
public class ExecutorService {
    private static final ExecutorService singleton = new ExecutorService();

    public static ExecutorService getSingleton() {
        return singleton;
    }

    private final HashMap<Class, Executor> executors;

    public ExecutorService() {
        executors = new HashMap<Class, Executor>();
    }

    public Executor getExecutor(Class cls) {
        if (!executors.containsKey(cls)) {
            // TODO: Custom number of threads for services
            executors.put(cls, new ScheduledThreadPoolExecutor(1));
        }
        return executors.get(cls);
    }
}
