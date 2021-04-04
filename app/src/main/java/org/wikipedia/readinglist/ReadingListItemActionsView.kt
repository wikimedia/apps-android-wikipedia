package org.wikipedia.readinglist

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.core.view.isGone
import org.wikipedia.databinding.ViewReadingListPageActionsBinding
import org.wikipedia.util.StringUtil

class ReadingListItemActionsView : LinearLayout {
    interface Callback {
        fun onToggleOffline()
        fun onShare()
        fun onAddToOther()
        fun onMoveToOther()
        fun onSelect()
        fun onDelete()
    }

    private val binding = ViewReadingListPageActionsBinding.inflate(LayoutInflater.from(context), this)
    var callback: Callback? = null

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        orientation = VERTICAL

        binding.readingListItemOffline.setOnClickListener {
            callback?.onToggleOffline()
        }

        binding.readingListItemShare.setOnClickListener {
            callback?.onShare()
        }

        binding.readingListItemAddToOther.setOnClickListener {
            callback?.onAddToOther()
        }

        binding.readingListItemMoveToOther.setOnClickListener {
            callback?.onMoveToOther()
        }

        binding.readingListItemSelect.setOnClickListener {
            callback?.onSelect()
        }

        binding.readingListItemRemove.setOnClickListener {
            callback?.onDelete()
        }
    }

    fun setState(pageTitle: String, removeFromListText: String, offline: Boolean, hasActionMode: Boolean) {
        binding.readingListItemOfflineSwitch.isChecked = offline
        binding.readingListItemTitle.text = StringUtil.fromHtml(pageTitle)
        binding.readingListItemRemoveText.text = removeFromListText
        binding.readingListItemSelect.isGone = hasActionMode
        binding.readingListItemMoveToOther.isGone = hasActionMode
    }
}
