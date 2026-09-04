package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.content.res.use
import com.google.android.material.textview.MaterialTextView
import org.wikipedia.R
import org.wikipedia.util.StringUtil

// TODO: Document where it is desirable to use this class vs. a vanilla TextView
open class AppTextView(context: Context, attrs: AttributeSet? = null) : MaterialTextView(context, attrs) {
    init {
        text = context.obtainStyledAttributes(attrs, R.styleable.AppTextView).use {
            StringUtil.fromHtml(it.getString(R.styleable.AppTextView_html))
        }
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        try {
            // Workaround for some obscure AOSP crashes when highlighting text.
            return super.dispatchTouchEvent(event)
        } catch (e: Exception) {
            // ignore
        }
        return true
    }
}
