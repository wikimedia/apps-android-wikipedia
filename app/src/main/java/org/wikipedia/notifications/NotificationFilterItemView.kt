package org.wikipedia.notifications

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
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
        fun onCheckedChanged(langCode: String)
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
            val filterTitleText = binding.notificationFilterTitle.text.toString()
            NotificationCategory.findOrNull(filterTitleText)?.let {
                callback?.onCheckedChanged(it.id)
                return@setOnClickListener
            } ?: run {
                when (filterTitleText) {
                    context.getString(R.string.notifications_all_wikis_text) -> callback?.onCheckedChanged(context.getString(R.string.notifications_all_wikis_text))
                    context.getString(R.string.notifications_all_types_text) -> callback?.onCheckedChanged(context.getString(R.string.notifications_all_types_text))
                    context.getString(R.string.wikimedia_commons) -> callback?.onCheckedChanged("commons")
                    context.getString(R.string.wikidata) -> callback?.onCheckedChanged("wikidata")
                    else -> callback?.onCheckedChanged(filter?.filterCode.toString())
                }
            }
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
            binding.notificationFilterWikiLogo.imageTintList = ColorStateList.valueOf(ResourceUtil.getThemedColor(context, R.attr.secondary_text_color))
            if (NotificationCategory.isFiltersGroup(filter.filterCode)) {
                binding.notificationFilterWikiLogo.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, NotificationCategory.find(filter.filterCode).iconColor))
            }
            binding.notificationFilterWikiLogo.setImageDrawable(AppCompatResources.getDrawable(context, it))
            binding.notificationFilterWikiLogo.visibility = View.VISIBLE
        } ?: run {
            binding.notificationFilterWikiLogo.visibility = View.GONE
        }
    }

    private fun getTitleCodeFor(filterCode: String?): String? {
        return if (filterCode == "commons" || filterCode == "wikidata" || filterCode == context.getString(R.string.notifications_all_wikis_text) ||
            filterCode == context.getString(R.string.notifications_all_types_text) || NotificationCategory.isFiltersGroup(filterCode.orEmpty())) null
        else filterCode
    }

    private fun getTitleFor(languageCode: String?): String {
        if (languageCode != null && NotificationCategory.isFiltersGroup(languageCode)) {
            return context.getString(NotificationCategory.find(languageCode).title)
        }
        return when (languageCode) {
            "commons" -> context.getString(R.string.wikimedia_commons)
            "wikidata" -> context.getString(R.string.wikidata)
            context.getString(R.string.notifications_all_wikis_text) -> languageCode
            context.getString(R.string.notifications_all_types_text) -> languageCode
            else -> WikipediaApp.getInstance().language().getAppLanguageCanonicalName(languageCode).orEmpty()
        }
    }
}
