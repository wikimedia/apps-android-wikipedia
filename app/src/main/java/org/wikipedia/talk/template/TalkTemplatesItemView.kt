package org.wikipedia.talk.template

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.text.TextUtils
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import org.wikipedia.R
import org.wikipedia.databinding.ItemTalkTemplatesBinding
import org.wikipedia.talk.db.TalkTemplate
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.ResourceUtil

class TalkTemplatesItemView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    interface Callback {
        fun onClick(position: Int)
        fun onCheckedChanged(position: Int)
        fun onLongPress(position: Int)
    }

    private var binding = ItemTalkTemplatesBinding.inflate(LayoutInflater.from(context), this)
    var callback: Callback? = null

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        setBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            foreground = AppCompatResources.getDrawable(context,
                ResourceUtil.getThemedAttributeId(context, androidx.appcompat.R.attr.selectableItemBackground))
        }
        DeviceUtil.setContextClickAsLongClick(this)
    }

    private fun updateBackgroundColor() {
        setBackgroundColor(if (binding.checkbox.isChecked) ResourceUtil.getThemedColor(context, R.attr.background_color)
        else ResourceUtil.getThemedColor(context, R.attr.paper_color))
    }

    fun setContents(talkTemplate: TalkTemplate, position: Int, isSaveMessagesTab: Boolean = false) {
        binding.listItemTitle.isVisible = !(isSaveMessagesTab && position == 0)
        binding.listItemTitle.text = talkTemplate.subject
        binding.listItemDescription.text = talkTemplate.message
        binding.listItemDescription.setTypeface(Typeface.SANS_SERIF, if (position == 0 && isSaveMessagesTab) Typeface.ITALIC else Typeface.NORMAL)
        binding.listItemDescription.isSingleLine = !(position == 0 && isSaveMessagesTab)
        binding.listItem.setBackgroundResource(ResourceUtil.getThemedAttributeId(context,
            if (position == 0 && isSaveMessagesTab) R.attr.background_color else android.R.attr.selectableItemBackground))
        binding.listItemDescription.ellipsize =
            if (position == 0 && isSaveMessagesTab) null else TextUtils.TruncateAt.END

        binding.listItem.setOnClickListener {
            callback?.onClick(position)
        }
        binding.listItem.setOnLongClickListener {
            callback?.onLongPress(position)
            true
        }
        binding.checkbox.setOnCheckedChangeListener { _, _ ->
            callback?.onCheckedChanged(position)
            updateBackgroundColor()
        }
    }

    fun setCheckBoxEnabled(enabled: Boolean) {
        binding.checkbox.visibility = if (enabled) VISIBLE else GONE
        if (!enabled) {
            binding.checkbox.isChecked = false
            setBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
        }
    }

    fun setCheckBoxChecked(checked: Boolean) {
        binding.checkbox.isChecked = checked
        updateBackgroundColor()
    }

    fun setDragHandleEnabled(enabled: Boolean) {
        binding.dragHandle.visibility = if (enabled) VISIBLE else GONE
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setDragHandleTouchListener(listener: OnTouchListener?) {
        binding.dragHandle.setOnTouchListener(listener)
    }
}
