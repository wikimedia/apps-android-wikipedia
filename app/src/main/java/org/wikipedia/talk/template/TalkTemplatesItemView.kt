package org.wikipedia.talk.template

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import org.wikipedia.R
import org.wikipedia.databinding.ItemTalkTemplatesBinding
import org.wikipedia.talk.db.TalkTemplate
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.ResourceUtil

class TalkTemplatesItemView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    interface Callback {
        fun onClick(talkTemplate: TalkTemplate)
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

    fun setContents(talkTemplate: TalkTemplate) {
        binding.listItem.text = talkTemplate.title
        binding.listItem.setOnClickListener {
            callback?.onClick(talkTemplate)
        }
        binding.listItem.setOnLongClickListener {
            callback?.onLongPress(talkTemplate.order - 1)
            true
        }
        binding.checkbox.setOnCheckedChangeListener { _, _ ->
            callback?.onCheckedChanged(talkTemplate.order - 1)
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
