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
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ItemNotificationFilterBinding
import org.wikipedia.notifications.NotificationCategory.Companion
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
    var callback: Callback? = null
    var filter: Filter? = null

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, DimenUtil.dpToPx(48f).toInt())
        setBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            foreground = AppCompatResources.getDrawable(context, ResourceUtil.getThemedAttributeId(context, R.attr.selectableItemBackground))
        }
        setOnClickListener {
            NotificationCategory.FILTERS_GROUP.find { context.getString(it.title) == binding.notificationFilterTitle.text.toString() }?.let {
                callback?.onCheckedChanged(it.id)
                return@setOnClickListener
            } ?: run {
                when (binding.notificationFilterTitle.text.toString()) {
                    context.getString(R.string.notifications_all_wikis_text) -> callback?.onCheckedChanged(context.getString(R.string.notifications_all_wikis_text))
                    context.getString(R.string.notifications_all_types_text) -> callback?.onCheckedChanged(context.getString(R.string.notifications_all_types_text))
                    context.getString(R.string.wikimedia_commons) -> callback?.onCheckedChanged("commons")
                    context.getString(R.string.wikidata) -> callback?.onCheckedChanged("wikidata")
                    else -> callback?.onCheckedChanged(filter?.languageCode.toString())
                }
            }
        }
    }

    fun setContents(filter: Filter) {
        this.filter = filter
        binding.notificationFilterTitle.text = getTitleFor(filter.languageCode)
        binding.notificationFilterCheck.visibility = if (filter.isEnabled()) View.VISIBLE else View.GONE
        getTitleCodeFor(filter.languageCode)?.let {
            binding.notificationFilterLanguageCode.text = it
            binding.notificationFilterLanguageCode.visibility = View.VISIBLE
            ViewUtil.formatLangButton(binding.notificationFilterLanguageCode, it, SearchFragment.LANG_BUTTON_TEXT_SIZE_SMALLER, SearchFragment.LANG_BUTTON_TEXT_SIZE_LARGER)
        } ?: run {
            if (filter.languageCode == context.getString(R.string.notifications_all_wikis_text) || filter.languageCode == context.getString(R.string.notifications_all_types_text))
                binding.notificationFilterLanguageCode.visibility = View.INVISIBLE
            else binding.notificationFilterLanguageCode.visibility = View.GONE
        }
        filter.imageRes?.let {
            filter.languageCode.let { languageCode ->
                if (NotificationCategory.isFiltersGroup(languageCode)) binding.notificationFilterWikiLogo.imageTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, Companion.FILTERS_GROUP.find { category -> category.id == languageCode }!!.iconColor))
                else binding.notificationFilterWikiLogo.imageTintList = ColorStateList.valueOf(ResourceUtil.getThemedColor(context, R.attr.secondary_text_color))
            }
            binding.notificationFilterWikiLogo.setImageDrawable(AppCompatResources.getDrawable(context, it))
            binding.notificationFilterWikiLogo.visibility = View.VISIBLE
        } ?: run {
            binding.notificationFilterWikiLogo.visibility = View.GONE
        }
    }

    private fun getTitleCodeFor(languageCode: String?): String? {
        return if (languageCode == "commons" || languageCode == "wikidata" || languageCode == context.getString(R.string.notifications_all_wikis_text) ||
            languageCode == context.getString(R.string.notifications_all_types_text) || NotificationCategory.isFiltersGroup(languageCode!!)) null
        else languageCode
    }

    private fun getTitleFor(languageCode: String?): String {
        if (NotificationCategory.isFiltersGroup(languageCode!!)) {
            return context.getString(NotificationCategory.FILTERS_GROUP.find { it.id == languageCode }!!.title)
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
