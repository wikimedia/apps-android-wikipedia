package org.wikipedia.dataclient.restbase

import kotlinx.serialization.Serializable

@Serializable
class RbDefinition(val usagesByLang: Map<String, List<Usage>>) {

    @Serializable
    class Usage(val partOfSpeech: String = "", val definitions: List<Definition>)

    @Serializable
    class Definition(val definition: String = "", val examples: List<String>? = null)
}
