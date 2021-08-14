package org.wikipedia.feed.configure

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ItemFeedContentTypeLangSelectDialogBinding
import org.wikipedia.databinding.ItemFeedContentTypeLangSelectItemBinding

class ConfigureItemLanguageDialogView : FrameLayout {

    private val binding = ItemFeedContentTypeLangSelectDialogBinding.inflate(LayoutInflater.from(context), this, true)

    private lateinit var langList: List<String>
    private lateinit var disabledList: MutableList<String>

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        binding.languageList.layoutManager = LinearLayoutManager(context)
    }

    fun setContentType(langList: List<String>, disabledList: MutableList<String>) {
        this.langList = langList
        this.disabledList = disabledList
        binding.languageList.adapter = LanguageItemAdapter()
    }

    private inner class LanguageItemHolder constructor(private val itemBinding: ItemFeedContentTypeLangSelectItemBinding) :
        RecyclerView.ViewHolder(itemBinding.root), OnClickListener {
        private lateinit var langCode: String

        fun bindItem(langCode: String) {
            this.langCode = langCode
            itemBinding.feedContentTypeLangContainer.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            itemBinding.feedContentTypeLangName.text = WikipediaApp.getInstance().language().getAppLanguageLocalizedName(langCode)
            itemBinding.feedContentTypeLangContainer.setOnClickListener(this)
            itemBinding.feedContentTypeLangCheckbox.setOnClickListener(this)
            updateState()
        }

        override fun onClick(v: View) {
            if (disabledList.contains(langCode)) {
                disabledList.remove(langCode)
            } else {
                disabledList.add(langCode)
            }
            updateState()
        }

        private fun updateState() {
            itemBinding.feedContentTypeLangCheckbox.isChecked = !disabledList.contains(langCode)
        }
    }

    private inner class LanguageItemAdapter : RecyclerView.Adapter<LanguageItemHolder>() {
        override fun getItemCount(): Int {
            return langList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): LanguageItemHolder {
            val binding = ItemFeedContentTypeLangSelectItemBinding.inflate(LayoutInflater.from(context),
                null, false)
            return LanguageItemHolder(binding)
        }

        override fun onBindViewHolder(holder: LanguageItemHolder, pos: Int) {
            holder.bindItem(langList[pos])
        }
    }
}
