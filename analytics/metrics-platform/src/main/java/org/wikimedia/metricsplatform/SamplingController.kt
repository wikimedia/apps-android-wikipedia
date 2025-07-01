package org.wikimedia.metricsplatform

import org.wikimedia.metricsplatform.config.StreamConfig
import org.wikimedia.metricsplatform.config.sampling.SampleConfig
import org.wikimedia.metricsplatform.context.ClientData
import java.util.UUID

/**
 * SamplingController: computes various sampling functions on the client
 *
 * Sampling is based on associative identifiers, each of which have a
 * well-defined scope, and sampling config, which each stream provides as
 * part of its configuration.
 */
class SamplingController internal constructor(
    private val clientData: ClientData,
    private val sessionController: SessionController
) {
    /**
     * @param streamConfig stream config
     * @return true if in sample or false otherwise
     */
    fun isInSample(streamConfig: StreamConfig): Boolean {
        if (!streamConfig.hasSampleConfig()) {
            return true
        }
        val sampleConfig = streamConfig.sampleConfig!!
        if (sampleConfig.rate == 1.0) {
            return true
        }
        if (sampleConfig.rate == 0.0) {
            return false
        }
        return getSamplingValue(sampleConfig.unit) < sampleConfig.rate
    }

    fun getSamplingValue(unit: String): Double {
        val token = getSamplingId(unit).substring(0, 8)
        return token.toLong(16).toDouble() / 0xFFFFFFFFL.toDouble()
    }

    /**
     * Returns the ID string to be used when evaluating presence in sample.
     * The ID used is configured in stream config.
     *
     * @param unit Identifier enum value
     * @return the requested ID string
     */
    fun getSamplingId(unit: String): String {
        return when (unit) {
            SampleConfig.UNIT_SESSION -> return sessionController.sessionId
            SampleConfig.UNIT_DEVICE -> return clientData.agentData.appInstallId.orEmpty()
            SampleConfig.UNIT_PAGEVIEW -> return clientData.performerData.pageviewId.orEmpty()
            else -> UUID.randomUUID().toString()
        }
    }
}
