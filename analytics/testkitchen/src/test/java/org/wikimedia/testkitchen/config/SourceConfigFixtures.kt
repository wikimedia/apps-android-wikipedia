package org.wikimedia.testkitchen.config

object SourceConfigFixtures {
    val testSourceConfigMin
        get() = SourceConfig(StreamConfigFixtures.streamConfigMap(StreamConfigFixtures.provideValuesMinimum()))

    val testSourceConfigMax
        get() = SourceConfig(StreamConfigFixtures.streamConfigMap(StreamConfigFixtures.provideValuesExtended()))
}
