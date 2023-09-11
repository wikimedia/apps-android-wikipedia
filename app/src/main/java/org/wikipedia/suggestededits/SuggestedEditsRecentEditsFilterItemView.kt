package org.wikipedia.suggestededits

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ItemSuggestedEditsRecentEditsFilterBinding
import org.wikipedia.search.SearchFragment
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.ViewUtil

class SuggestedEditsRecentEditsFilterItemView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    interface Callback {
        fun onCheckedChanged(filter: SuggestedEditsRecentEditsFilterActivity.Filter?)
    }

    private var binding = ItemSuggestedEditsRecentEditsFilterBinding.inflate(LayoutInflater.from(context), this)
    private var filter: SuggestedEditsRecentEditsFilterActivity.Filter? = null
    var callback: Callback? = null

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DimenUtil.roundedDpToPx(48f))
        setBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            foreground = AppCompatResources.getDrawable(context, ResourceUtil.getThemedAttributeId(context, androidx.appcompat.R.attr.selectableItemBackground))
        }
        setOnClickListener {
            callback?.onCheckedChanged(filter)
        }
    }

    fun setContents(filter: SuggestedEditsRecentEditsFilterActivity.Filter) {
        this.filter = filter
        binding.recentEditsFilterTitle.text = getTitleFor(filter)
        binding.recentEditsFilterCheck.isVisible = filter.isEnabled()

        getTitleCodeFor(filter)?.let {
            binding.recentEditsFilterLanguageCode.text = it
            binding.recentEditsFilterLanguageCode.visibility = View.VISIBLE
            ViewUtil.formatLangButton(binding.recentEditsFilterLanguageCode, it,
                SearchFragment.LANG_BUTTON_TEXT_SIZE_SMALLER, SearchFragment.LANG_BUTTON_TEXT_SIZE_LARGER)
        } ?: run {
            binding.recentEditsFilterLanguageCode.visibility = View.GONE
        }

        binding.recentEditsFilterWikiLogo.visibility = View.GONE
    }

    fun setSingleLabel(text: String) {
        val accentColor = ResourceUtil.getThemedColorStateList(context, R.attr.progressive_color)
        binding.recentEditsFilterLanguageCode.visibility = View.GONE
        binding.recentEditsFilterWikiLogo.visibility = View.VISIBLE
        ImageViewCompat.setImageTintList(binding.recentEditsFilterWikiLogo, accentColor)
        binding.recentEditsFilterWikiLogo.setImageResource(R.drawable.ic_mode_edit_white_24dp)
        binding.recentEditsFilterCheck.visibility = View.GONE
        binding.recentEditsFilterTitle.setTextColor(accentColor)
        binding.recentEditsFilterTitle.text = text
        binding.recentEditsFilterTitle.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        binding.recentEditsFilterTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
    }

    private fun getTitleCodeFor(filter: SuggestedEditsRecentEditsFilterActivity.Filter): String? {
        return if (filter.filterCode != context.getString(R.string.patroller_tasks_filters_all_text) && filter.type == SuggestedEditsRecentEditsFilterActivity.FILTER_TYPE_WIKI) filter.filterCode
        else null
    }

    private fun getTitleFor(filter: SuggestedEditsRecentEditsFilterActivity.Filter): String {
        if (filter.type == SuggestedEditsRecentEditsFilterActivity.FILTER_TYPE_CATEGORY) {
            return context.getString(SuggestedEditsRecentEditsFilterTypes.find(filter.filterCode).title)
        }
        return when (filter.filterCode) {
            context.getString(R.string.notifications_all_wikis_text) -> filter.filterCode
            else -> WikipediaApp.instance.languageState.getAppLanguageCanonicalName(filter.filterCode).orEmpty()
        }
    }
}
