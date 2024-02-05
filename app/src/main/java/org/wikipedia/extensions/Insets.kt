@file:Suppress("NOTHING_TO_INLINE")

package org.wikipedia.extensions

import androidx.core.graphics.Insets

inline operator fun Insets.component1() = left

inline operator fun Insets.component2() = top

inline operator fun Insets.component3() = right

inline operator fun Insets.component4() = bottom
