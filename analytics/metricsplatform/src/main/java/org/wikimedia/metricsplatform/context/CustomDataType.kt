package org.wikimedia.metricsplatform.context

import kotlinx.serialization.SerialName

enum class CustomDataType {
    @SerialName("number") NUMBER,
    @SerialName("string") STRING,
    @SerialName("boolean") BOOLEAN,
    @SerialName("null") NULL
}
