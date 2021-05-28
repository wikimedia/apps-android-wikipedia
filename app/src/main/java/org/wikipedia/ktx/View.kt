package org.wikipedia.ktx

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsControllerCompat

inline val View.windowInsetsControllerCompat: WindowInsetsControllerCompat?
    get() = ViewCompat.getWindowInsetsController(this)
