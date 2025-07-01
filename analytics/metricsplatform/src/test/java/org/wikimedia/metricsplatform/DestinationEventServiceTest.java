package org.wikimedia.metricsplatform;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.Test;
import org.wikimedia.metricsplatform.config.DestinationEventService;

class DestinationEventServiceTest {

    @Test void testDestinationEventServiceAnalytics() throws MalformedURLException {
        DestinationEventService loggingService = DestinationEventService.ANALYTICS;
        assertThat(loggingService.getBaseUri()).isEqualTo(new URL("https://intake-analytics.wikimedia.org/v1/events?hasty=true"));
    }

    @Test void testDestinationEventServiceLogging() throws MalformedURLException {
        DestinationEventService loggingService = DestinationEventService.ERROR_LOGGING;
        assertThat(loggingService.getBaseUri()).isEqualTo(new URL("https://intake-logging.wikimedia.org/v1/events?hasty=true"));
    }

    @Test void testDestinationEventServiceLocal() throws MalformedURLException {
        DestinationEventService loggingService = DestinationEventService.LOCAL;
        assertThat(loggingService.getBaseUri()).isEqualTo(new URL("http://localhost:8192/v1/events?hasty=true"));
    }

    @Test void testDestinationEventServiceAnalyticsDev() throws MalformedURLException {
        DestinationEventService loggingService = DestinationEventService.ANALYTICS;
        assertThat(loggingService.getBaseUri(true)).isEqualTo(new URL("https://intake-analytics.wikimedia.org/v1/events"));
    }

    @Test void testDestinationEventServiceLoggingDev() throws MalformedURLException {
        DestinationEventService loggingService = DestinationEventService.ERROR_LOGGING;
        assertThat(loggingService.getBaseUri(true)).isEqualTo(new URL("https://intake-logging.wikimedia.org/v1/events"));
    }

    @Test void testDestinationEventServiceLocalDev() throws MalformedURLException {
        DestinationEventService loggingService = DestinationEventService.LOCAL;
        assertThat(loggingService.getBaseUri(true)).isEqualTo(new URL("http://localhost:8192/v1/events"));
    }
}
