package org.wikimedia.metrics_platform.context

import kotlinx.serialization.SerialName

enum class CustomDataType {
    @SerialName("number") NUMBER,
    @SerialName("string") STRING,
    @SerialName("boolean") BOOLEAN,
    @SerialName("null") NULL
}
