package org.wikimedia.metricsplatform.config;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URL;

import org.junit.jupiter.api.Test;
import org.wikimedia.metricsplatform.json.GsonHelper;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.google.common.io.Resources;

import okhttp3.OkHttpClient;

@WireMockTest
class StreamConfigIT {

    @Test void canLoadConfigOverHTTP(WireMockRuntimeInfo wmRuntimeInfo) throws IOException {
        stubFor(get("/streamConfig").willReturn(
                aResponse()
                        .withBody(loadConfigStream())
                )
        );

        StreamConfigFetcher streamConfigFetcher = new StreamConfigFetcher(
                new URL(wmRuntimeInfo.getHttpBaseUrl() + "/streamConfig"),
                new OkHttpClient(),
                GsonHelper.getGson()
        );

        SourceConfig sourceConfig = streamConfigFetcher.fetchStreamConfigs();

        assertThat(sourceConfig).isNotNull();

    }

    private byte[] loadConfigStream() throws IOException {
        return Resources.asByteSource(
                Resources.getResource("org/wikimedia/metrics_platform/config/streamconfigs.json")
        ).read();
    }

}
