package org.wikipedia.notifications

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ItemNotificationFilterBinding
import org.wikipedia.notifications.NotificationsFilterActivity.Filter
import org.wikipedia.search.SearchFragment
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.ViewUtil

class NotificationFilterItemView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    interface Callback {
        fun onCheckedChanged(filter: Filter?)
    }

    private var binding = ItemNotificationFilterBinding.inflate(LayoutInflater.from(context), this)
    private var filter: Filter? = null
    var callback: Callback? = null

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DimenUtil.roundedDpToPx(48f))
        setBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            foreground = AppCompatResources.getDrawable(context, ResourceUtil.getThemedAttributeId(context, R.attr.selectableItemBackground))
        }
        setOnClickListener {
            callback?.onCheckedChanged(filter)
        }
    }

    fun setContents(filter: Filter) {
        this.filter = filter
        binding.notificationFilterTitle.text = getTitleFor(filter.filterCode)
        binding.notificationFilterCheck.isVisible = filter.isEnabled()
        getTitleCodeFor(filter.filterCode)?.let {
            binding.notificationFilterLanguageCode.text = it
            binding.notificationFilterLanguageCode.visibility = View.VISIBLE
            ViewUtil.formatLangButton(binding.notificationFilterLanguageCode, it,
                SearchFragment.LANG_BUTTON_TEXT_SIZE_SMALLER, SearchFragment.LANG_BUTTON_TEXT_SIZE_LARGER)
        } ?: run {
            if (filter.filterCode == context.getString(R.string.notifications_all_wikis_text) || filter.filterCode == context.getString(R.string.notifications_all_types_text))
                binding.notificationFilterLanguageCode.visibility = View.INVISIBLE
            else binding.notificationFilterLanguageCode.visibility = View.GONE
        }
        filter.imageRes?.let {
            ImageViewCompat.setImageTintList(binding.notificationFilterWikiLogo,
                ColorStateList.valueOf(ResourceUtil.getThemedColor(context, R.attr.secondary_text_color)))
            if (NotificationCategory.isFiltersGroup(filter.filterCode)) {
                val colorStateList = ColorStateList.valueOf(ContextCompat.getColor(context,
                    ResourceUtil.getThemedAttributeId(context, NotificationCategory.find(filter.filterCode).iconColor)))
                ImageViewCompat.setImageTintList(binding.notificationFilterWikiLogo, colorStateList)
            }
            binding.notificationFilterWikiLogo.setImageDrawable(AppCompatResources.getDrawable(context, it))
            binding.notificationFilterWikiLogo.visibility = View.VISIBLE
        } ?: run {
            binding.notificationFilterWikiLogo.visibility = View.GONE
        }
    }

    fun setSingleLabel(text: String) {
        val accentColor = ResourceUtil.getThemedColor(context, R.attr.colorAccent)
        binding.notificationFilterLanguageCode.visibility = View.GONE
        binding.notificationFilterWikiLogo.visibility = View.VISIBLE
        ImageViewCompat.setImageTintList(binding.notificationFilterWikiLogo, ColorStateList.valueOf(accentColor))
        binding.notificationFilterWikiLogo.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_add_gray_themed_24dp))
        binding.notificationFilterCheck.visibility = View.GONE
        binding.notificationFilterTitle.setTextColor(accentColor)
        binding.notificationFilterTitle.text = text.uppercase()
        binding.notificationFilterTitle.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        binding.notificationFilterTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
    }

    private fun getTitleCodeFor(filterCode: String): String? {
        return if (filterCode == Constants.WIKI_CODE_COMMONS || filterCode == Constants.WIKI_CODE_WIKIDATA ||
                filterCode == context.getString(R.string.notifications_all_wikis_text) ||
                filterCode == context.getString(R.string.notifications_all_types_text) || NotificationCategory.isFiltersGroup(filterCode)) null
        else filterCode
    }

    private fun getTitleFor(filterCode: String): String {
        if (NotificationCategory.isFiltersGroup(filterCode)) {
            return context.getString(NotificationCategory.find(filterCode).title)
        }
        return when (filterCode) {
            Constants.WIKI_CODE_COMMONS -> context.getString(R.string.wikimedia_commons)
            Constants.WIKI_CODE_WIKIDATA -> context.getString(R.string.wikidata)
            context.getString(R.string.notifications_all_wikis_text) -> filterCode
            context.getString(R.string.notifications_all_types_text) -> filterCode
            else -> WikipediaApp.getInstance().language().getAppLanguageCanonicalName(filterCode).orEmpty()
        }
    }
}
