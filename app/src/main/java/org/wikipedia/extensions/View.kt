package org.wikipedia.extensions

import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AppCompatActivity
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
