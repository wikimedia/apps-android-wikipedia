package org.wikipedia.dataclient.restbase

import org.wikipedia.json.annotations.Required

class RbDefinition(private val usagesByLang: Map<String, Array<Usage>>) {

    fun getUsagesForLang(langCode: String): Array<Usage>? {
        return usagesByLang[langCode]
    }

    class Usage(

        val partOfSpeech: String,
        val definitions: Array<Definition>
    )

    class Definition(@field:Required val definition: String, val examples: Array<String>?)
}
