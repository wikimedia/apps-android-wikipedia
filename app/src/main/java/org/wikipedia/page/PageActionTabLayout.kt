package org.wikipedia.page

import android.content.Context
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import androidx.core.widget.TextViewCompat
import com.google.android.material.textview.MaterialTextView
import org.wikipedia.R
import org.wikipedia.page.action.PageActionItem
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.ConfigurableTabLayout

class PageActionTabLayout constructor(context: Context, attrs: AttributeSet? = null) : ConfigurableTabLayout(context, attrs) {

    lateinit var callback: PageActionItem.Callback

    init {
        update()
    }

    fun update() {
        removeAllViews()
        Prefs.customizeToolbarOrder.forEach {
            val view = MaterialTextView(context)
            view.gravity = Gravity.CENTER
            view.setPadding(0, DimenUtil.roundedDpToPx(8f), 0, 0)
            view.setBackgroundResource(ResourceUtil.getThemedAttributeId(context, R.attr.selectableItemBackgroundBorderless))
            view.setTextColor(ResourceUtil.getThemedColor(context, R.attr.chip_text_color))
            view.textAlignment = TEXT_ALIGNMENT_CENTER
            view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 10f)
            view.maxLines = 2
            view.ellipsize = TextUtils.TruncateAt.END

            val item = PageActionItem.find(it)
            view.id = item.hashCode()
            view.text = context.getString(item.titleResId)
            view.contentDescription = view.text
            FeedbackUtil.setButtonLongPressToast(view)
            TextViewCompat.setCompoundDrawableTintList(view, ResourceUtil.getThemedColorStateList(context, R.attr.chip_text_color))
            view.setCompoundDrawablesWithIntrinsicBounds(0, item.iconResId, 0, 0)
            view.compoundDrawablePadding = -DimenUtil.roundedDpToPx(4f)
            view.setOnClickListener { v ->
                if (isEnabled(v)) {
                    item.select(callback)
                }
            }
            view.isFocusable = true
            addView(view, LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f))
        }
    }
}
