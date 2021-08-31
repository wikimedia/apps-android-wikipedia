package org.wikipedia.dataclient.mwapi

internal class MwAuthManagerInfo {

    val requests: List<Request>? = null

    internal class Request(val id: String? = null,
                           private val metadata: Map<String, String>? = null,
                           private val required: String? = null,
                           private val provider: String? = null,
                           private val account: String? = null,
                           val fields: Map<String, Field>? = null)

    internal class Field(private val type: String? = null,
                         private val label: String? = null,
                         private val help: String? = null,
                         private val optional: Boolean = false,
                         private val sensitive: Boolean = false,
                         val value: String? = null)
}
