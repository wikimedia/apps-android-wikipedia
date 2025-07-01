package org.wikimedia.metricsplatform.config;

public final class SourceConfigFixtures {

    private SourceConfigFixtures() {
        // Utility class, should never be instantiated
    }

    /**
     * Convenience method for getting source config with minimum provided values.
     */
    public static SourceConfig getTestSourceConfigMin() {
        return new SourceConfig(StreamConfigFixtures.streamConfigMap(StreamConfigFixtures.provideValuesMinimum()));
    }

    /**
     * Convenience method for getting source config with extended provided values.
     */
    public static SourceConfig getTestSourceConfigMax() {
        return new SourceConfig(StreamConfigFixtures.streamConfigMap(StreamConfigFixtures.provideValuesExtended()));
    }
}
