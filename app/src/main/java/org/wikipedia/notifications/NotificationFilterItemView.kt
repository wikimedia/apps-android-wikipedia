package org.wikipedia.notifications

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.core.widget.ImageViewCompat
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ItemNotificationFilterBinding
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil

class NotificationFilterItemView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    interface Callback {
        fun onCheckedChanged(filter: NotificationFilterActivity.Filter?)
    }

    private var binding = ItemNotificationFilterBinding.inflate(LayoutInflater.from(context), this)
    private var filter: NotificationFilterActivity.Filter? = null
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

    fun setContents(filter: NotificationFilterActivity.Filter) {
        this.filter = filter
        binding.notificationFilterTitle.text = getTitleFor(filter.filterCode)
        binding.notificationFilterCheck.isVisible = filter.isEnabled()
        getTitleCodeFor(filter.filterCode)?.let {
            binding.notificationFilterLanguageCode.setLangCode(it)
            binding.notificationFilterLanguageCode.visibility = View.VISIBLE
        } ?: run {
            if (filter.filterCode == context.getString(R.string.notifications_all_wikis_text) || filter.filterCode == context.getString(R.string.notifications_all_types_text))
                binding.notificationFilterLanguageCode.visibility = View.INVISIBLE
            else binding.notificationFilterLanguageCode.visibility = View.GONE
        }
        filter.imageRes?.let {
            ImageViewCompat.setImageTintList(binding.notificationFilterWikiLogo,
                ResourceUtil.getThemedColorStateList(context, R.attr.placeholder_color))
            binding.notificationFilterWikiLogo.setImageResource(it)
            binding.notificationFilterWikiLogo.visibility = View.VISIBLE
        } ?: run {
            binding.notificationFilterWikiLogo.visibility = View.GONE
        }
    }

    fun setSingleLabel(text: String) {
        val accentColor = ResourceUtil.getThemedColorStateList(context, R.attr.progressive_color)
        binding.notificationFilterLanguageCode.visibility = View.GONE
        binding.notificationFilterWikiLogo.visibility = View.VISIBLE
        ImageViewCompat.setImageTintList(binding.notificationFilterWikiLogo, accentColor)
        binding.notificationFilterWikiLogo.setImageResource(R.drawable.ic_mode_edit_white_24dp)
        binding.notificationFilterCheck.visibility = View.GONE
        binding.notificationFilterTitle.setTextColor(accentColor)
        binding.notificationFilterTitle.text = text
        binding.notificationFilterTitle.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
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
            else -> WikipediaApp.instance.languageState.getAppLanguageLocalizedName(filterCode).orEmpty()
        }
    }
}
