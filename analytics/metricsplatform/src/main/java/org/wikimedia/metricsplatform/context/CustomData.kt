package org.wikimedia.metricsplatform.context

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class CustomData (
    @SerialName("data_type") val type: CustomDataType? = null,
    val value: String? = null
) {
    companion object {
        /**
         * Return custom data, based on a generic object.
         *
         * @return formatted custom data
         */
        fun of(value: Any?): CustomData {
            var formattedValue = value.toString()
            val formattedType: CustomDataType?

            if (value is Number) {
                formattedType = CustomDataType.NUMBER
            } else if (value is Boolean) {
                formattedType = CustomDataType.BOOLEAN
                formattedValue = if (value) "true" else "false"
            } else {
                formattedType = CustomDataType.STRING
            }

            return CustomData(formattedType, formattedValue)
        }
    }
}
