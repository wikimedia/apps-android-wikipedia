package org.wikipedia.page.customize

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import org.wikipedia.R
import org.wikipedia.databinding.ItemCustomizeToolbarBinding
import org.wikipedia.page.action.PageActionItem
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil

class CustomizeToolbarItemView : LinearLayout {
    private var binding = ItemCustomizeToolbarBinding.inflate(LayoutInflater.from(context), this)
    private var position = 0

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        setBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
        FeedbackUtil.setButtonOnClickToast(binding.listItem, binding.dragHandle)
    }

    fun setContents(pageActionItem: PageActionItem, position: Int) {
        this.position = position
        binding.listItem.text = context.getString(pageActionItem.titleResId)
        binding.listItem.setCompoundDrawablesWithIntrinsicBounds(pageActionItem.iconResId, 0, 0, 0)
    }

    fun setDragHandleEnabled(enabled: Boolean) {
        binding.dragHandle.visibility = if (enabled) VISIBLE else GONE
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setDragHandleTouchListener(listener: OnTouchListener?) {
        binding.dragHandle.setOnTouchListener(listener)
    }
}
