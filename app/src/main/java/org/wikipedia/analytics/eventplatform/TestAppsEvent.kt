package org.wikipedia.analytics.eventplatform

import kotlinx.serialization.Serializable

@Serializable
class TestAppsEvent : AppsEvent {
    constructor(_stream: String) : super(_stream)
}
