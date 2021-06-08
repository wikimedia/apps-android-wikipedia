package org.wikipedia.feed.configure

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.feed.FeedContentType
import org.wikipedia.util.DimenUtil

class LanguageItemAdapter(private val context: Context,
                          private val contentType: FeedContentType) : RecyclerView.Adapter<LanguageItemHolder>() {
    val langList = mutableListOf<String>()

    init {
        if (contentType.langCodesSupported.isEmpty()) {
            // all languages supported
            langList.addAll(WikipediaApp.getInstance().language().appLanguageCodes)
        } else {
            // take the intersection of the supported languages and the available app languages
            langList.addAll(WikipediaApp.getInstance().language().appLanguageCodes.filter { contentType.langCodesSupported.contains(it) })
        }
    }

    override fun getItemCount(): Int {
        return langList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): LanguageItemHolder {
        val view = View.inflate(context, R.layout.item_feed_content_type_lang_box, null)
        val params = MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.leftMargin = DimenUtil.roundedDpToPx(2f)
        params.rightMargin = params.leftMargin
        view.layoutParams = params
        return LanguageItemHolder(context, view)
    }

    override fun onBindViewHolder(holder: LanguageItemHolder, pos: Int) {
        holder.bindItem(langList[pos], !contentType.langCodesDisabled.contains(langList[pos]))
    }
}
