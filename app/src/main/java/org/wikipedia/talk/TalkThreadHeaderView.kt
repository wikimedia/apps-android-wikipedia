package org.wikipedia.talk

import android.content.Context
import android.text.method.MovementMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import org.wikipedia.R
import org.wikipedia.databinding.ItemTalkThreadHeaderBinding
import org.wikipedia.dataclient.discussiontools.ThreadItem
import org.wikipedia.page.PageTitle
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.staticdata.TalkAliasData
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil

class TalkThreadHeaderView constructor(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {
    interface Callback {
        fun onSubscribeClick()
    }

    var callback: Callback? = null
    private val binding = ItemTalkThreadHeaderBinding.inflate(LayoutInflater.from(context), this)

    init {
        layoutParams = ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        binding.subscribeButton.setOnClickListener {
            callback?.onSubscribeClick()
        }
    }

    fun bind(pageTitle: PageTitle, item: ThreadItem?, subscribed: Boolean, movementMethod: MovementMethod) {
        binding.pageTitleText.movementMethod = movementMethod
        val baseTitle = TalkTopicsActivity.getNonTalkPageTitle(pageTitle)
        binding.pageTitleText.text = StringUtil.fromHtml(pageTitle.namespace.ifEmpty { TalkAliasData.valueFor(pageTitle.wikiSite.languageCode) } +
                ": " + "<a href='" + baseTitle.uri + "'>${StringUtil.removeNamespace(pageTitle.displayText)}</a>")
        RichTextUtil.removeUnderlinesFromLinks(binding.pageTitleText)

        val titleStr = StringUtil.fromHtml(item?.html).toString().trim()
        binding.threadTitleText.text = titleStr.ifEmpty { context.getString(R.string.talk_no_subject) }

        binding.subscribeButton.text = context.getString(if (subscribed) R.string.talk_list_item_overflow_subscribed else R.string.talk_list_item_overflow_subscribe)
        binding.subscribeButton.setTextColor(ResourceUtil.getThemedColor(context, if (subscribed) R.attr.material_theme_secondary_color else R.attr.colorAccent))
        binding.subscribeButton.setIconResource(if (subscribed) R.drawable.ic_notifications_active else R.drawable.ic_notifications_black_24dp)
        binding.subscribeButton.iconTint = binding.subscribeButton.textColors
    }
}
