package org.wikipedia.views

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.content.withStyledAttributes
import org.wikipedia.R
import org.wikipedia.util.StringUtil

// TODO: Document where it is desirable to use this class vs. a vanilla TextView
open class AppTextView constructor(context: Context, attrs: AttributeSet? = null) : ConfigurableTextView(context, attrs) {
    init {
        if (attrs != null) {
            context.withStyledAttributes(attrs, R.styleable.AppTextView) {
                val htmlText = getString(R.styleable.AppTextView_html)
                if (htmlText != null) {
                    text = StringUtil.fromHtml(htmlText)
                }
            }
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
