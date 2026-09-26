package org.wikipedia.search.semantic

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SemanticSearchPageHooks(
    val snippet: String? = null,
    val section: String? = null
) : Parcelable
