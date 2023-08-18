package org.wikipedia.talk.template

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import org.wikipedia.R
import org.wikipedia.databinding.ItemTalkTemplatesBinding
import org.wikipedia.talk.db.TalkTemplate
import org.wikipedia.util.ResourceUtil

class TalkTemplatesItemView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    interface Callback {
        fun onClick(talkTemplate: TalkTemplate)
    }

    private var binding = ItemTalkTemplatesBinding.inflate(LayoutInflater.from(context), this)
    var callback: Callback? = null

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
        setBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
    }

    fun setContents(talkTemplate: TalkTemplate) {
        binding.listItem.text = talkTemplate.title
        binding.listItem.setOnClickListener {
            callback?.onClick(talkTemplate)
        }
        binding.listItem.setOnLongClickListener {
            callback?.onClick(talkTemplate)
            true
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setDragHandleTouchListener(listener: OnTouchListener?) {
        binding.dragHandle.setOnTouchListener(listener)
    }
}
