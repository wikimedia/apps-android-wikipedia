package org.wikimedia.metrics_platform.context

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Agent context data fields.
 *
 * All fields are nullable, and boxed types are used in place of their equivalent primitive types to avoid
 * unexpected default values from being used where the true value is null.
 */
@Serializable
class AgentData(
    @SerialName("app_flavor") val appFlavor: String? = null,
    @SerialName("app_install_id") val appInstallId: String? = null,
    @SerialName("app_theme") val appTheme: String? = null,
    @SerialName("app_version") val appVersion: Int? = null,
    @SerialName("app_version_name") val appVersionName: String? = null,
    @SerialName("client_platform") val clientPlatform: String? = null,
    @SerialName("client_platform_family") val clientPlatformFamily: String? = null,
    @SerialName("device_family") val deviceFamily: String? = null,
    @SerialName("device_language") val deviceLanguage: String? = null,
    @SerialName("release_status") val releaseStatus: String? = null,
)
