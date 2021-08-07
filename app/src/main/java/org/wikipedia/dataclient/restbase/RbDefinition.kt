package org.wikipedia.dataclient.restbase

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
class RbDefinition(val usagesByLang: Map<String, Array<Usage>> = emptyMap()) {
    @JsonClass(generateAdapter = true)
    class Usage(val partOfSpeech: String = "", val definitions: Array<Definition> = emptyArray())

    @JsonClass(generateAdapter = true)
    class Definition(val definition: String = "", val examples: Array<String> = emptyArray())
}
