package org.wikipedia.feed.configure

import android.content.Context
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.WikipediaApp
import org.wikipedia.feed.FeedContentType
import org.wikipedia.util.DimenUtil
import org.wikipedia.views.LangCodeView

class LanguageItemAdapter(private val context: Context,
                          private val contentType: FeedContentType) : RecyclerView.Adapter<LanguageItemHolder>() {
    val langList = mutableListOf<String>()

    init {
        if (contentType.langCodesSupported.isEmpty()) {
            // all languages supported
            langList.addAll(WikipediaApp.instance.languageState.appLanguageCodes)
        } else {
            // take the intersection of the supported languages and the available app languages
            langList.addAll(WikipediaApp.instance.languageState.appLanguageCodes.filter { contentType.langCodesSupported.contains(it) })
        }
    }

    override fun getItemCount(): Int {
        return langList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, type: Int): LanguageItemHolder {
        val view = LangCodeView(context)
        val params = MarginLayoutParams(DimenUtil.roundedDpToPx(24f), ViewGroup.LayoutParams.WRAP_CONTENT)
        params.rightMargin = DimenUtil.roundedDpToPx(2f)
        view.layoutParams = params
        return LanguageItemHolder(context, view)
    }

    override fun onBindViewHolder(holder: LanguageItemHolder, pos: Int) {
        holder.bindItem(langList[pos], !contentType.langCodesDisabled.contains(langList[pos]))
    }
}
