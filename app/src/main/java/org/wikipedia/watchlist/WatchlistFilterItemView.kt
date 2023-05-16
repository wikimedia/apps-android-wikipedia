package org.wikipedia.watchlist

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
import org.wikipedia.databinding.ItemWatchlistFilterBinding
import org.wikipedia.search.SearchFragment
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.ViewUtil

class WatchlistFilterItemView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    interface Callback {
        fun onCheckedChanged(filter: WatchlistFilterActivity.Filter?)
    }

    private var binding = ItemWatchlistFilterBinding.inflate(LayoutInflater.from(context), this)
    private var filter: WatchlistFilterActivity.Filter? = null
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

    fun setContents(filter: WatchlistFilterActivity.Filter) {
        this.filter = filter
        binding.watchlistFilterTitle.text = getTitleFor(filter)
        binding.watchlistFilterCheck.isVisible = filter.isEnabled()
        binding.watchlistFilterCheck.setImageResource(if (filter.isCheckBox) R.drawable.ic_check_borderless else R.drawable.ic_baseline_radio_button_checked_24)

        getTitleCodeFor(filter)?.let {
            binding.watchlistFilterLanguageCode.text = it
            binding.watchlistFilterLanguageCode.visibility = View.VISIBLE
            ViewUtil.formatLangButton(binding.watchlistFilterLanguageCode, it,
                SearchFragment.LANG_BUTTON_TEXT_SIZE_SMALLER, SearchFragment.LANG_BUTTON_TEXT_SIZE_LARGER)
        } ?: run {
            binding.watchlistFilterLanguageCode.visibility = View.GONE
        }

        binding.watchlistFilterWikiLogo.visibility = View.GONE
    }

    fun setSingleLabel(text: String) {
        val accentColor = ResourceUtil.getThemedColorStateList(context, R.attr.progressive_color)
        binding.watchlistFilterLanguageCode.visibility = View.GONE
        binding.watchlistFilterWikiLogo.visibility = View.VISIBLE
        ImageViewCompat.setImageTintList(binding.watchlistFilterWikiLogo, accentColor)
        binding.watchlistFilterWikiLogo.setImageResource(R.drawable.ic_mode_edit_white_24dp)
        binding.watchlistFilterCheck.visibility = View.GONE
        binding.watchlistFilterTitle.setTextColor(accentColor)
        binding.watchlistFilterTitle.text = text
        binding.watchlistFilterTitle.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        binding.watchlistFilterTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
    }

    private fun getTitleCodeFor(filter: WatchlistFilterActivity.Filter): String? {
        return if (filter.filterCode != context.getString(R.string.watchlist_filter_all_text) && filter.type == WatchlistFilterActivity.FILTER_TYPE_WIKI) filter.filterCode
        else null
    }

    private fun getTitleFor(filter: WatchlistFilterActivity.Filter): String {
        if (filter.type == WatchlistFilterActivity.FILTER_TYPE_CATEGORY) {
            return context.getString(WatchlistFilterTypes.find(filter.filterCode).title)
        }
        return when (filter.filterCode) {
            context.getString(R.string.notifications_all_wikis_text) -> filter.filterCode
            else -> WikipediaApp.instance.languageState.getAppLanguageCanonicalName(filter.filterCode).orEmpty()
        }
    }
}
