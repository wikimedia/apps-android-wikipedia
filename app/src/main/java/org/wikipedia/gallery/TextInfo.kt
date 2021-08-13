package org.wikipedia.gallery

import kotlinx.serialization.Serializable

@Serializable
open class TextInfo(val lang: String = "", val text: String = "", val html: String = "")
