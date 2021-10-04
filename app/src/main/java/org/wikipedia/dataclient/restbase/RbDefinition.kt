package org.wikipedia.dataclient.restbase

import kotlinx.serialization.Serializable

@Serializable
class RbDefinition(val usagesByLang: Map<String, Array<Usage>>) {

    @Serializable
    class Usage(val partOfSpeech: String, val definitions: Array<Definition>)

    @Serializable
    class Definition(val definition: String, val examples: Array<String>?)
}
