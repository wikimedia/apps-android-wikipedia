package org.wikipedia.gallery

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
open class TextInfo(val lang: String = "", val text: String = "", val html: String = "") :
    Parcelable
