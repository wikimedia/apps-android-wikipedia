package org.wikimedia.metrics_platform;

import javax.annotation.concurrent.ThreadSafe;
import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.wikimedia.metrics_platform.config.sampling.SampleConfig;
import org.wikimedia.metrics_platform.config.StreamConfig;
import org.wikimedia.metrics_platform.context.ClientData;

/**
 * SamplingController: computes various sampling functions on the client
 *
 * Sampling is based on associative identifiers, each of which have a
 * well-defined scope, and sampling config, which each stream provides as
 * part of its configuration.
 */
@ThreadSafe
@ParametersAreNonnullByDefault
public class SamplingController {

    private final ClientData clientData;
    private final SessionController sessionController;

    SamplingController(ClientData clientData, SessionController sessionController) {
        this.clientData = clientData;
        this.sessionController = sessionController;
    }

    /**
     * @param streamConfig stream config
     * @return true if in sample or false otherwise
     */
    boolean isInSample(StreamConfig streamConfig) {
        if (!streamConfig.hasSampleConfig()) {
            return true;
        }
        SampleConfig sampleConfig = streamConfig.getSampleConfig();
        if (sampleConfig.getRate() == 1.0) {
            return true;
        }
        if (sampleConfig.getRate() == 0.0) {
            return false;
        }
        return getSamplingValue(sampleConfig.getIdentifier()) < sampleConfig.getRate();
    }

    /**
     * @param identifier identifier type from sampling config
     * @return a floating point value between 0.0 and 1.0 (inclusive)
     */
    double getSamplingValue(SampleConfig.Identifier identifier) {
        String token = getSamplingId(identifier).substring(0, 8);
        return (double) Long.parseLong(token, 16) / (double) 0xFFFFFFFFL;
    }

    /**
     * Returns the ID string to be used when evaluating presence in sample.
     * The ID used is configured in stream config.
     *
     * @param identifier Identifier enum value
     * @return the requested ID string
     */
    @Nonnull
    String getSamplingId(SampleConfig.Identifier identifier) {
        switch (identifier) {
            case SESSION:
                return sessionController.getSessionId();
            case DEVICE:
                return clientData.getAgentData().getAppInstallId();
            case PAGEVIEW:
                return clientData.getPerformerData().getPageviewId();
            default:
                throw new IllegalArgumentException("Bad identifier type: " + identifier);
        }
    }

}
