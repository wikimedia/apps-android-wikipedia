package org.wikimedia.metrics_platform.config;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.logging.Level.WARNING;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

public class ConfigFetcherRunnable implements Runnable {

    private final Duration streamConfigFetchInterval;
    private final StreamConfigFetcher streamConfigFetcher;
    private final List<Consumer<SourceConfig>> consumers;
    private final ScheduledExecutorService executorService;
    private final Duration retryDelay;

    public ConfigFetcherRunnable(
            Duration streamConfigFetchInterval,
            StreamConfigFetcher streamConfigFetcher,
            List<Consumer<SourceConfig>> consumers,
            ScheduledExecutorService executorService,
            Duration retryDelay
    ) {
        this.streamConfigFetchInterval = streamConfigFetchInterval;
        this.streamConfigFetcher = streamConfigFetcher;
        this.consumers = consumers;
        this.executorService = executorService;
        this.retryDelay = retryDelay;
    }

    public void run() {
        Duration nextFetch = streamConfigFetchInterval;
        try {
            SourceConfig sourceConfig = streamConfigFetcher.fetchStreamConfigs();

            for (Consumer<SourceConfig> consumer : consumers) {
                consumer.accept(sourceConfig);
            }
        } catch (Exception e) {
            log.log(WARNING, "Could not fetch configuration. Will retry sooner.", e);
            nextFetch = retryDelay;
        } finally {
            executorService.schedule(this, nextFetch.toMillis(), MILLISECONDS);
        }
    }
}
