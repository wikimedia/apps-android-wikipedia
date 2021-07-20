package org.wikipedia.dataclient.restbase

class RbDefinition(val usagesByLang: Map<String, Array<Usage>>) {

    class Usage(val partOfSpeech: String, val definitions: Array<Definition>)

    class Definition(val definition: String, val examples: Array<String>?)
}
