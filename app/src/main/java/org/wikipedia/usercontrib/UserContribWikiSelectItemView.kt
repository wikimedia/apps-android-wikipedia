package org.wikipedia.usercontrib

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ItemUserContribWikiSelectBinding
import org.wikipedia.search.SearchFragment
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.ViewUtil

class UserContribWikiSelectItemView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    interface Callback {
        fun onSelected(item: UserContribWikiSelectActivity.Item?)
    }

    private var item: UserContribWikiSelectActivity.Item? = null
    private var binding = ItemUserContribWikiSelectBinding.inflate(LayoutInflater.from(context), this)
    private val labelTypeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    var callback: Callback? = null

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DimenUtil.roundedDpToPx(48f))
        setBackgroundResource(ResourceUtil.getThemedAttributeId(context, R.attr.selectableItemBackground))
        setOnClickListener {
            callback?.onSelected(item)
        }
    }

    fun setContents(item: UserContribWikiSelectActivity.Item, checked: Boolean) {
        this.item = item
        binding.wikiTitle.text = WikipediaApp.instance.languageState.getWikiLanguageName(item.itemCode)
        binding.itemCheck.isVisible = checked
        getTitleCodeFor(item.itemCode)?.let {
            binding.languageCode.text = it
            binding.languageCode.visibility = View.VISIBLE
            ViewUtil.formatLangButton(binding.languageCode, it, SearchFragment.LANG_BUTTON_TEXT_SIZE_SMALLER, SearchFragment.LANG_BUTTON_TEXT_SIZE_LARGER)
        } ?: run {
            binding.languageCode.visibility = View.GONE
        }
        item.imageRes?.let {
            ImageViewCompat.setImageTintList(binding.wikiLogo, ResourceUtil.getThemedColorStateList(context, R.attr.secondary_text_color))
            binding.wikiLogo.setImageResource(it)
            binding.wikiLogo.visibility = View.VISIBLE
        } ?: run {
            binding.wikiLogo.visibility = View.GONE
        }
    }

    fun setSingleLabel(text: String) {
        val accentColor = ResourceUtil.getThemedColorStateList(context, R.attr.colorAccent)
        binding.languageCode.visibility = View.GONE
        binding.wikiLogo.visibility = View.VISIBLE
        ImageViewCompat.setImageTintList(binding.wikiLogo, accentColor)
        binding.wikiLogo.setImageResource(R.drawable.ic_mode_edit_themed_24dp)
        binding.itemCheck.visibility = View.GONE
        binding.wikiTitle.setTextColor(accentColor)
        binding.wikiTitle.text = text.uppercase()
        binding.wikiTitle.typeface = labelTypeface
        binding.wikiTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
    }

    private fun getTitleCodeFor(itemCode: String): String? {
        return if (itemCode == Constants.WIKI_CODE_COMMONS || itemCode == Constants.WIKI_CODE_WIKIDATA) null
        else itemCode
    }
}
