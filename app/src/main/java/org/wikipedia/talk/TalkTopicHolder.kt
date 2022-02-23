package org.wikipedia.talk

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.database.AppDatabase
import org.wikipedia.databinding.ItemTalkTopicBinding
import org.wikipedia.dataclient.page.TalkPage
import org.wikipedia.page.PageTitle
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil

class TalkTopicHolder internal constructor(
    private val binding: ItemTalkTopicBinding,
    private val context: Context,
    private val pageTitle: PageTitle,
    private val invokeSource: Constants.InvokeSource
) : RecyclerView.ViewHolder(binding.root), View.OnClickListener {

    private val unreadTypeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    private var id: Int = 0

    fun bindItem(topic: TalkPage.Topic, searchQuery: String?) {
        id = topic.id
        val seen = AppDatabase.getAppDatabase().talkPageSeenDao().getTalkPageSeen(topic.getIndicatorSha()) != null
        var titleStr = RichTextUtil.stripHtml(topic.html).trim()
        if (titleStr.isEmpty()) {
            // build up a title based on the contents, massaging the html into plain text that
            // flows over a few lines...
            topic.replies?.firstOrNull()?.let {
                titleStr = RichTextUtil.stripHtml(it.html).replace("\n", " ")
                if (titleStr.length > MAX_CHARS_NO_SUBJECT) {
                    titleStr = titleStr.substring(0, MAX_CHARS_NO_SUBJECT) + "…"
                }
            }
        }

        binding.topicTitleText.text = titleStr.ifEmpty { context.getString(R.string.talk_no_subject) }
        binding.topicTitleText.visibility = View.VISIBLE
        binding.topicSubtitleText.visibility = View.GONE
        binding.topicTitleText.typeface = if (seen) Typeface.SANS_SERIF else unreadTypeface
        binding.topicTitleText.setTextColor(ResourceUtil.getThemedColor(context, if (seen) android.R.attr.textColorTertiary else R.attr.material_theme_primary_color))
        StringUtil.highlightAndBoldenText(binding.topicTitleText, searchQuery, true, Color.YELLOW)
        itemView.setOnClickListener(this)
    }

    override fun onClick(v: View?) {
        context.startActivity(TalkTopicActivity.newIntent(context, pageTitle, id, invokeSource))
    }

    companion object {
        private const val MAX_CHARS_NO_SUBJECT = 100
    }
}
