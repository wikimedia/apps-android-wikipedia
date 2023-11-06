package org.wikipedia.views

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.content.res.use
import org.wikipedia.R
import org.wikipedia.util.StringUtil

// TODO: Document where it is desirable to use this class vs. a vanilla TextView
open class AppTextView constructor(context: Context, attrs: AttributeSet? = null) : ConfigurableTextView(context, attrs) {
    init {
        text = context.obtainStyledAttributes(attrs, R.styleable.AppTextView).use {
            StringUtil.fromHtml(it.getString(R.styleable.AppTextView_html))
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // Workaround for https://code.google.com/p/android/issues/detail?id=191430
        // which only occurs on API 23
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
            if (event.actionMasked == MotionEvent.ACTION_DOWN &&
                    selectionStart != selectionEnd) {
                val text = text
                setText(null)
                setText(text)
            }
        }
        try {
            // Workaround for some obscure AOSP crashes when highlighting text.
            return super.dispatchTouchEvent(event)
        } catch (e: Exception) {
            // ignore
        }
        return true
    }
}
