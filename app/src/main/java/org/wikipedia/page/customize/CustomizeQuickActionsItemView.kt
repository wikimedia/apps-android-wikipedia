package org.wikipedia.page.customize

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import org.wikipedia.R
import org.wikipedia.databinding.ItemCustomizeQuickActionsBinding
import org.wikipedia.util.ResourceUtil

class CustomizeQuickActionsItemView : LinearLayout {
    private var binding = ItemCustomizeQuickActionsBinding.inflate(LayoutInflater.from(context), this)
    private var position = 0

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        setBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
    }

    fun setContents(quickActionItem: QuickActionItem, position: Int) {
        this.position = position
        binding.listItem.text = context.getString(quickActionItem.titleResId)
        binding.listItem.setCompoundDrawablesWithIntrinsicBounds(quickActionItem.iconResId, 0, 0, 0)
    }

    fun setDragHandleEnabled(enabled: Boolean) {
        binding.dragHandle.visibility = if (enabled) VISIBLE else GONE
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setDragHandleTouchListener(listener: OnTouchListener?) {
        binding.dragHandle.setOnTouchListener(listener)
    }
}
