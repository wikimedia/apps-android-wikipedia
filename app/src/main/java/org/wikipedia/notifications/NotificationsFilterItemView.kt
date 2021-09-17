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
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.ResourceUtil

class NotificationsFilterItemView constructor(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs) {
    interface Callback {
        fun onCheckedChanged(langCode: String)
    }

    var callback: Callback? = null
    private var binding = ItemNotificationFilterBinding.inflate(LayoutInflater.from(context), this)

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200)
        setBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.paper_color))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            foreground =
                AppCompatResources.getDrawable(context, ResourceUtil.getThemedAttributeId(context, R.attr.selectableItemBackground))
        }
        setOnClickListener {
            when {
                binding.notificationFilterTitle.text.toString() == context.getString(R.string.wikimedia_commons) -> {
                    callback?.onCheckedChanged("commons")
                }
                binding.notificationFilterTitle.text.toString() == context.getString(R.string.wikidata) -> {
                    callback?.onCheckedChanged("wikidata")
                }
                else -> {
                    callback?.onCheckedChanged(binding.notificationFilterLanguageCode.text!!.toString())
                }
            }
        }
        DeviceUtil.setContextClickAsLongClick(this)
    }

    fun setContents(langCode: String?, title: String, selected: Boolean, imageRes: Int?) {
        binding.notificationFilterTitle.text = title
        binding.notificationFilterCheck.visibility = if (selected) View.VISIBLE else View.GONE
        if (langCode == null) binding.notificationFilterLanguageCode.visibility =
            GONE else View.VISIBLE
        langCode?.let { binding.notificationFilterLanguageCode.text = langCode }
        if (imageRes == null) binding.notificationFilterWikiLogo.visibility = GONE else View.VISIBLE
        imageRes?.let { binding.notificationFilterWikiLogo.setImageDrawable(AppCompatResources.getDrawable(context, imageRes)) }
    }
}
