package org.wikipedia.feed.configure

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ItemFeedContentTypeLangSelectDialogBinding
import org.wikipedia.views.DefaultViewHolder

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

    private inner class LanguageItemHolder constructor(itemView: View) : DefaultViewHolder<View>(itemView), OnClickListener {
        private lateinit var langCode: String
        private val container = ViewCompat.requireViewById<View>(itemView, R.id.feed_content_type_lang_container)
        private val checkbox = ViewCompat.requireViewById<CheckBox>(itemView, R.id.feed_content_type_lang_checkbox)
        private val langNameView = ViewCompat.requireViewById<TextView>(itemView, R.id.feed_content_type_lang_name)

        fun bindItem(langCode: String) {
            this.langCode = langCode
            container.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            langNameView.text = WikipediaApp.getInstance().language().getAppLanguageLocalizedName(langCode)
            container.setOnClickListener(this)
            checkbox.setOnClickListener(this)
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
            checkbox.isChecked = !disabledList.contains(langCode)
        }
    }

    private inner class LanguageItemAdapter : RecyclerView.Adapter<LanguageItemHolder>() {
        override fun getItemCount(): Int {
            return langList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, type: Int): LanguageItemHolder {
            val view = inflate(context, R.layout.item_feed_content_type_lang_select_item, null)
            return LanguageItemHolder(view)
        }

        override fun onBindViewHolder(holder: LanguageItemHolder, pos: Int) {
            holder.bindItem(langList[pos])
        }
    }
}
