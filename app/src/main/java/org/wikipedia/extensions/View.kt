package org.wikipedia.extensions

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.wikipedia.util.L10nUtil
import java.util.Locale
import kotlin.coroutines.CoroutineContext

fun View.coroutineScope(coroutineContext: CoroutineContext = Dispatchers.Main): CoroutineScope {
    return (context as? AppCompatActivity)?.lifecycleScope ?: CoroutineScope(coroutineContext)
}

fun View.setTextDirectionByLang(lang: String) {
    textDirection = if (L10nUtil.isLangRTL(lang)) View.TEXT_DIRECTION_RTL else View.TEXT_DIRECTION_LTR
}

fun View.setLayoutDirectionByLang(lang: String) {
    layoutDirection = TextUtils.getLayoutDirectionFromLocale(Locale(lang))
}

fun View.ensureSoftwareBitmaps() {
    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            getChildAt(i).ensureSoftwareBitmaps()
        }
    } else if (this is ImageView) {
        val drawable = drawable
        if (drawable is BitmapDrawable) {
            val bmp = drawable.bitmap
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bmp?.config == Bitmap.Config.HARDWARE) {
                val softwareCopy = bmp.copy(Bitmap.Config.ARGB_8888, false)
                setImageDrawable(softwareCopy.toDrawable(resources))
            }
        }
    }
}
