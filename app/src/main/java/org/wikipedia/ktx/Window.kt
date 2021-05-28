package org.wikipedia.ktx

import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

inline val Window.insetsControllerCompat: WindowInsetsControllerCompat?
    get() = WindowCompat.getInsetsController(this, decorView)
