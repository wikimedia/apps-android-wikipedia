package org.wikipedia.dataclient.restbase

import org.wikipedia.json.annotations.Required

class RbDefinition(@field:Required private val usagesByLang: Map<String, Array<Usage>>) {

    fun getUsagesForLang(langCode: String): Array<Usage>? {
        return usagesByLang[langCode]
    }

    class Usage(

        @field:Required val partOfSpeech: String,
        @field:Required val definitions: Array<Definition>
    )

    class Definition(@field:Required val definition: String, val examples: Array<String>?)
}
