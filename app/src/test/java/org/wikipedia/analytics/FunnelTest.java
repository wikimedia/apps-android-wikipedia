package org.wikipedia.analytics;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class FunnelTest {

    @Test
    public void testUserWithinSamplingGroup() throws Exception {
        assertThat(Funnel.isUserInSamplingGroup("25a609da-35e9-4902-94c6-c343f7eb3784", Funnel.SAMPLE_LOG_ALL), is(true));
        assertThat(Funnel.isUserInSamplingGroup("15f3f81b-d30a-4485-8dca-37d07aea146e", Funnel.SAMPLE_LOG_10), is(true));
        assertThat(Funnel.isUserInSamplingGroup("10ccfe00-2d6a-4e22-8e4c-09215201db88", Funnel.SAMPLE_LOG_100), is(true));
        assertThat(Funnel.isUserInSamplingGroup("2bee6109-5be2-42b8-ad61-53c35ca89470", Funnel.SAMPLE_LOG_1K), is(true));
    }

    @Test
    public void testUserOutsideSamplingGroup() throws Exception {
        // Not divisible by 10
        assertThat(Funnel.isUserInSamplingGroup("15f3f81b-d30a-4485-8dca-37d07aea146f", Funnel.SAMPLE_LOG_10), is(false));
        // Divisible by 10, but not 100
        assertThat(Funnel.isUserInSamplingGroup("15f3f81b-d30a-4485-8dca-37d07aea146e", Funnel.SAMPLE_LOG_100), is(false));
        // = 1 mod 100
        assertThat(Funnel.isUserInSamplingGroup("10ccfe00-2d6a-4e22-8e4c-09215201db89", Funnel.SAMPLE_LOG_100), is(false));
        // Divisible by 10, but not 1000
        assertThat(Funnel.isUserInSamplingGroup("15f3f81b-d30a-4485-8dca-37d07aea146e", Funnel.SAMPLE_LOG_1K), is(false));
        // Divisible by 100, but not 1000
        assertThat(Funnel.isUserInSamplingGroup("10ccfe00-2d6a-4e22-8e4c-09215201db88", Funnel.SAMPLE_LOG_1K), is(false));
        // = 1 mod 1000
        assertThat(Funnel.isUserInSamplingGroup("2bee6109-5be2-42b8-ad61-53c35ca89471", Funnel.SAMPLE_LOG_1K), is(false));
    }

    @Test
    public void testMalformedSamplingGroup() throws Exception {
        assertThat(Funnel.isUserInSamplingGroup("foo", Funnel.SAMPLE_LOG_ALL), is(false));
        assertThat(Funnel.isUserInSamplingGroup("", Funnel.SAMPLE_LOG_ALL), is(false));
        assertThat(Funnel.isUserInSamplingGroup(null, Funnel.SAMPLE_LOG_ALL), is(false));
    }
}
