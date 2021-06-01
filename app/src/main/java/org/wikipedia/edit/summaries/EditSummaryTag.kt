package org.wikipedia.edit.summaries

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.setMargins
import androidx.core.view.setPadding
import org.wikipedia.R
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil

class EditSummaryTag @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = android.R.attr.textViewStyle) :
    AppCompatTextView(context, attrs, defStyle) {

    private var _selected = false
    val selected get() = _selected

    init {
        val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        val margin = DimenUtil.dpToPx(4F).toInt()
        val padding = DimenUtil.dpToPx(8F).toInt()
        params.setMargins(margin)
        layoutParams = params
        setPadding(padding)
        setOnClickListener {
            _selected = !_selected
            updateState()
        }
        updateState()
    }

    override fun toString() = text.toString()

    override fun setSelected(selected: Boolean) {
        this._selected = selected
        updateState()
    }

    private fun updateState() {
        @AttrRes val backgroundAttributeResource = if (selected) R.attr.edit_improve_tag_selected_drawable else R.attr.edit_improve_tag_unselected_drawable
        setBackgroundResource(ResourceUtil.getThemedAttributeId(context, backgroundAttributeResource))

        @ColorInt val textColor = ContextCompat.getColor(context, if (selected) android.R.color.white else ResourceUtil.getThemedAttributeId(context, R.attr.colorAccent))
        setTextColor(textColor)
    }
}
