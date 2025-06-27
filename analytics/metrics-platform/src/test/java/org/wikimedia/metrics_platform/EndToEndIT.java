package org.wikimedia.metrics_platform;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.wikimedia.metrics_platform.ConsistencyITClientData.createConsistencyTestClientData;
import static org.wikimedia.metrics_platform.context.DataFixtures.getTestCustomData;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wikimedia.metrics_platform.context.ClientData;
import org.wikimedia.metrics_platform.context.DataFixtures;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.google.common.io.Resources;

@WireMockTest(httpPort = 8192)
public class EndToEndIT {
    private String expectedEventClick;
    private String expectedEventClickCustom;
    private String expectedEventInteraction;
    private String expectedEventView;
    private byte[] localConfig;
    private final ClientData testClientData = createConsistencyTestClientData();

    @BeforeEach
    void fetchStreamConfigs() throws IOException {
        // Stub fetching the stream config from api endpoint.
        stubStreamConfigFetch();
    }

    @Test void submitClickEvent(WireMockRuntimeInfo wireMockRuntimeInfo) throws IOException {
        // Create the metrics client.
        MetricsClient testMetricsClient = buildMetricsClient(wireMockRuntimeInfo);
        await().atMost(10, SECONDS).until(testMetricsClient::isFullyInitialized);

        // Stub response from posting event to local eventgate logging service.
        stubFor(post("/v1/events?hasty=true")
                .willReturn(aResponse()
                        .withBody(getExpectedEventClick())));

        testMetricsClient.submitClick(
                DataFixtures.getTestStream("click"),
                DataFixtures.getTestClientData(getExpectedEventClick()),
                DataFixtures.getTestInteractionData("TestClick")
        );

        await().atMost(10, SECONDS).until(testMetricsClient::isEventQueueEmpty);

        verify(postRequestedFor(urlEqualTo("/v1/events?hasty=true"))
                .withRequestBody(equalToJson(getExpectedEventClick(), true, true)));
    }

    @Test void submitClickEventWithCustomData(WireMockRuntimeInfo wireMockRuntimeInfo) throws IOException {
        // Create the metrics client.
        MetricsClient testMetricsClient = buildMetricsClient(wireMockRuntimeInfo);
        await().atMost(10, SECONDS).until(testMetricsClient::isFullyInitialized);

        // Stub response from posting event to local eventgate logging service.
        stubFor(post("/v1/events?hasty=true")
                .willReturn(aResponse()
                        .withBody(getExpectedEventClickCustom())));

        testMetricsClient.submitClick(
                DataFixtures.getTestStream("click_custom"),
                "/analytics/product_metrics/app/click_custom/1.0.0",
                "click.test_event_name_for_end_to_end_testing",
                DataFixtures.getTestClientData(getExpectedEventClickCustom()),
                getTestCustomData(),
                DataFixtures.getTestInteractionData("TestClickCustom")
        );

        await().atMost(10, SECONDS).until(testMetricsClient::isEventQueueEmpty);

        verify(postRequestedFor(urlEqualTo("/v1/events?hasty=true"))
                .withRequestBody(equalToJson(getExpectedEventClickCustom(), true, true)));
    }

    @Test void submitViewEvent(WireMockRuntimeInfo wireMockRuntimeInfo) throws IOException {
        // Create the metrics client.
        MetricsClient testMetricsClient = buildMetricsClient(wireMockRuntimeInfo);
        await().atMost(10, SECONDS).until(testMetricsClient::isFullyInitialized);

        // Stub response from posting event to local eventgate logging service.
        stubFor(post("/v1/events?hasty=true")
                .willReturn(aResponse()
                        .withBody(getExpectedEventView())));

        testMetricsClient.submitView(
                DataFixtures.getTestStream("view"),
                DataFixtures.getTestClientData(getExpectedEventView()),
                DataFixtures.getTestInteractionData("TestView")
        );

        await().atMost(10, SECONDS).until(testMetricsClient::isEventQueueEmpty);

        verify(postRequestedFor(urlEqualTo("/v1/events?hasty=true"))
                .withRequestBody(equalToJson(getExpectedEventView(), true, true)));
    }

    @Test void submitInteractionEvent(WireMockRuntimeInfo wireMockRuntimeInfo) throws IOException {
        // Create the metrics client.
        MetricsClient testMetricsClient = buildMetricsClient(wireMockRuntimeInfo);
        await().atMost(10, SECONDS).until(testMetricsClient::isFullyInitialized);

        // Stub response from posting event to local eventgate logging service.
        stubFor(post("/v1/events?hasty=true")
                .willReturn(aResponse()
                        .withBody(getExpectedEventInteraction())));

        testMetricsClient.submitInteraction(
                DataFixtures.getTestStream("interaction"),
                "interaction.test_event_name_for_end_to_end_testing",
                DataFixtures.getTestClientData(getExpectedEventInteraction()),
                DataFixtures.getTestInteractionData("TestInteraction")
        );

        await().atMost(15, SECONDS).until(testMetricsClient::isEventQueueEmpty);

        verify(postRequestedFor(urlEqualTo("/v1/events?hasty=true"))
                .withRequestBody(equalToJson(getExpectedEventInteraction(), true, true)));
    }

    private byte[] readConfig() throws IOException {
        if (this.localConfig == null) {
            this.localConfig = Resources.asByteSource(
                    Resources.getResource("org/wikimedia/metrics_platform/config/streamconfigs-local.json")
            ).read();
        }
        return this.localConfig;
    }

    private String getExpectedEventClick() throws IOException {
        if (this.expectedEventClick == null) {
            this.expectedEventClick = Resources.asCharSource(
                    Resources.getResource("org/wikimedia/metrics_platform/event/expected_event_click.json"),
                    UTF_8
            ).read();
        }
        return this.expectedEventClick;
    }

    private String getExpectedEventClickCustom() throws IOException {
        if (this.expectedEventClickCustom == null) {
            this.expectedEventClickCustom = Resources.asCharSource(
                    Resources.getResource("org/wikimedia/metrics_platform/event/expected_event_click_custom.json"),
                    UTF_8
            ).read();
        }
        return this.expectedEventClickCustom;
    }

    private String getExpectedEventView() throws IOException {
        if (this.expectedEventView == null) {
            this.expectedEventView = Resources.asCharSource(
                    Resources.getResource("org/wikimedia/metrics_platform/event/expected_event_view.json"),
                    UTF_8
            ).read();
        }
        return this.expectedEventView;
    }

    private String getExpectedEventInteraction() throws IOException {
        if (this.expectedEventInteraction == null) {
            this.expectedEventInteraction = Resources.asCharSource(
                    Resources.getResource("org/wikimedia/metrics_platform/event/expected_event_interaction.json"),
                    UTF_8
            ).read();
        }
        return this.expectedEventInteraction;
    }

    private void stubStreamConfigFetch() throws IOException {
        stubFor(get(urlEqualTo("/config"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(readConfig())));
    }

    private MetricsClient buildMetricsClient(WireMockRuntimeInfo wireMockRuntimeInfo) throws MalformedURLException {
        return MetricsClient.builder(testClientData)
                .streamConfigURL(new URL(wireMockRuntimeInfo.getHttpBaseUrl() + "/config"))
                .isDebug(false)
                .build();
    }
}
