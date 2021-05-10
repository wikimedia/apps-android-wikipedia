package org.wikipedia.feed.configure

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ItemFeedContentTypeBinding
import org.wikipedia.feed.FeedContentType

class ConfigureItemView(context: Context) : FrameLayout(context) {

    interface Callback {
        fun onCheckedChanged(contentType: FeedContentType?, checked: Boolean)
        fun onLanguagesChanged(contentType: FeedContentType?)
    }

    private val binding = ItemFeedContentTypeBinding.inflate(LayoutInflater.from(context), this, true)

    private lateinit var contentType: FeedContentType
    private lateinit var adapter: LanguageItemAdapter
    var callback: Callback? = null

    init {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )

        binding.feedContentTypeLangList.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        binding.feedContentTypeCheckbox.setOnCheckedChangeListener { _, checked ->
            callback?.onCheckedChanged(contentType, checked)
        }

        binding.feedContentTypeLangListClickTarget.setOnClickListener {
            showLangSelectDialog()
        }
    }

    fun setContents(contentType: FeedContentType) {
        this.contentType = contentType
        binding.feedContentTypeTitle.setText(contentType.titleId())
        binding.feedContentTypeSubtitle.setText(contentType.subtitleId())
        binding.feedContentTypeCheckbox.isChecked = contentType.isEnabled
        if (contentType.isPerLanguage && WikipediaApp.getInstance().language().appLanguageCodes.size > 1) {
            binding.feedContentTypeLangListContainer.visibility = VISIBLE
            adapter = LanguageItemAdapter(context, contentType)
            binding.feedContentTypeLangList.adapter = adapter
        } else {
            binding.feedContentTypeLangListContainer.visibility = GONE
        }
    }

    private fun showLangSelectDialog() {
        val view = ConfigureItemLanguageDialogView(context)
        val tempDisabledList = contentType.langCodesDisabled.toMutableList()
        view.setContentType(adapter.langList, tempDisabledList)
        AlertDialog.Builder(context)
            .setView(view)
            .setTitle(contentType.titleId())
            .setPositiveButton(R.string.customize_lang_selection_dialog_ok_button_text) { _, _ ->
                contentType.langCodesDisabled.clear()
                contentType.langCodesDisabled.addAll(tempDisabledList)
                adapter.notifyDataSetChanged()
                callback?.onLanguagesChanged(contentType)
                val atLeastOneEnabled = adapter.langList.any { !tempDisabledList.contains(it) }
                binding.feedContentTypeCheckbox.isChecked = atLeastOneEnabled
            }
            .setNegativeButton(R.string.customize_lang_selection_dialog_cancel_button_text, null)
            .create()
            .show()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setDragHandleTouchListener(listener: OnTouchListener?) {
        binding.feedContentTypeDragHandle.setOnTouchListener(listener)
    }
}
