package org.wikipedia.notifications

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import org.wikipedia.R
import org.wikipedia.databinding.ItemNotificationFilterBinding
import org.wikipedia.search.SearchFragment
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.views.ViewUtil

class NotificationFilterItemView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    interface Callback {
        fun onCheckedChanged(langCode: String)
    }

    private var binding = ItemNotificationFilterBinding.inflate(LayoutInflater.from(context), this)
    var callback: Callback? = null

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200)
        setBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            foreground =
                AppCompatResources.getDrawable(context, ResourceUtil.getThemedAttributeId(context, R.attr.selectableItemBackground))
        }
        setOnClickListener {
            when (binding.notificationFilterTitle.text.toString()) {
                context.getString(R.string.wikimedia_commons) -> callback?.onCheckedChanged("commons")
                context.getString(R.string.wikidata) -> callback?.onCheckedChanged("wikidata")
                else -> callback?.onCheckedChanged(binding.notificationFilterLanguageCode.text!!.toString())
            }
        }
        DeviceUtil.setContextClickAsLongClick(this)
    }

    fun setContents(langCode: String?, title: String, selected: Boolean, imageRes: Int?) {
        binding.notificationFilterTitle.text = title
        binding.notificationFilterCheck.visibility = if (selected) View.VISIBLE else View.GONE
        langCode?.let {
            binding.notificationFilterLanguageCode.text = langCode
            binding.notificationFilterLanguageCode.visibility = View.VISIBLE
            ViewUtil.formatLangButton(binding.notificationFilterLanguageCode, langCode, SearchFragment.LANG_BUTTON_TEXT_SIZE_SMALLER, SearchFragment.LANG_BUTTON_TEXT_SIZE_LARGER)
        } ?: run {
            binding.notificationFilterLanguageCode.visibility = View.GONE
        }
        imageRes?.let {
            binding.notificationFilterWikiLogo.setImageDrawable(AppCompatResources.getDrawable(context, imageRes))
            binding.notificationFilterWikiLogo.visibility = View.VISIBLE
        } ?: run {
            binding.notificationFilterWikiLogo.visibility = View.GONE
        }
    }
}
