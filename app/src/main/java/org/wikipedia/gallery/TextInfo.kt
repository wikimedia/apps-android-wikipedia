package org.wikipedia.gallery

import java.io.Serializable

open class TextInfo(val lang: String = "", val text: String = "", val html: String = "") : Serializable
