package org.wikimedia.metricsplatform.context

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
    @SerialName("app_flavor") var appFlavor: String? = null,
    @SerialName("app_install_id") var appInstallId: String? = null,
    @SerialName("app_theme") var appTheme: String? = null,
    @SerialName("app_version") var appVersion: Int? = null,
    @SerialName("app_version_name") var appVersionName: String? = null,
    @SerialName("client_platform") var clientPlatform: String? = null,
    @SerialName("client_platform_family") var clientPlatformFamily: String? = null,
    @SerialName("device_family") var deviceFamily: String? = null,
    @SerialName("device_language") var deviceLanguage: String? = null,
    @SerialName("release_status") var releaseStatus: String? = null,
)
