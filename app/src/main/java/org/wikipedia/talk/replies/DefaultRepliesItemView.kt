package org.wikipedia.talk.replies

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import org.wikipedia.R
import org.wikipedia.databinding.ItemDefaultReplyBinding
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.ResourceUtil
import java.util.*

class DefaultRepliesItemView : LinearLayout {
    interface Callback {
        fun onCheckedChanged(position: Int)
        fun onLongPress(position: Int)
    }

    private var binding = ItemDefaultReplyBinding.inflate(LayoutInflater.from(context), this)
    private var position = 0
    var callback: Callback? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        setBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            foreground = AppCompatResources.getDrawable(context,
                ResourceUtil.getThemedAttributeId(context, R.attr.selectableItemBackground))
        }
        binding.replyCheckbox.setOnCheckedChangeListener { _, _ ->
            callback?.onCheckedChanged(position)
            updateBackgroundColor()
        }
        setOnLongClickListener {
            callback?.onLongPress(position)
            true
        }
        DeviceUtil.setContextClickAsLongClick(this)
    }

    private fun updateBackgroundColor() {
        setBackgroundColor(if (binding.replyCheckbox.isChecked) ResourceUtil.getThemedColor(context, R.attr.multi_select_background_color)
        else ResourceUtil.getThemedColor(context, R.attr.paper_color))
    }

    fun setContents(defaultReply: String, position: Int) {
        this.position = position
        binding.replyOrder.text = position.toString()
        binding.replyContent.text = defaultReply.capitalize(Locale.getDefault())
    }

    fun setCheckBoxEnabled(enabled: Boolean) {
        binding.replyOrder.visibility = if (enabled) GONE else VISIBLE
        binding.replyCheckbox.visibility = if (enabled) VISIBLE else GONE
        if (!enabled) {
            binding.replyCheckbox.isChecked = false
            setBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
        }
    }

    fun setCheckBoxChecked(checked: Boolean) {
        binding.replyCheckbox.isChecked = checked
        updateBackgroundColor()
    }

    fun setDragHandleEnabled(enabled: Boolean) {
        binding.replyDragHandle.visibility = if (enabled) VISIBLE else GONE
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setDragHandleTouchListener(listener: OnTouchListener?) {
        binding.replyDragHandle.setOnTouchListener(listener)
    }
}
