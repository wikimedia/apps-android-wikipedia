package org.wikipedia.dataclient.mwapi

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class MwAuthManagerInfo(val requests: List<Request> = emptyList()) {
    @JsonClass(generateAdapter = true)
    class Request(val id: String = "", internal val metadata: Map<String, String> = emptyMap(),
                  internal val required: String = "", internal val provider: String = "",
                  internal val account: String = "", val fields: Map<String, Field> = emptyMap())

    @JsonClass(generateAdapter = true)
    class Field(internal val type: String? = null, val value: String? = null,
                internal val label: String? = null, internal val help: String? = null,
                internal val optional: Boolean = false, internal val sensitive: Boolean = false)
}
