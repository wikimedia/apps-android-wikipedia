package org.wikipedia.views

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.util.Pair
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.net.toUri
import org.wikipedia.databinding.ViewWikiArticleCardBinding
import org.wikipedia.donate.donationreminder.DonationReminderHelper
import org.wikipedia.extensions.setLayoutDirectionByLang
import org.wikipedia.page.PageTitle
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.TransitionUtil

class WikiArticleCardView(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {

    val binding = ViewWikiArticleCardBinding.inflate(LayoutInflater.from(context), this)

    fun setTitle(title: String) {
        binding.articleTitle.text = StringUtil.fromHtml(title)
    }

    fun setDescription(description: String?) {
        binding.articleDescription.text = description
    }

    fun getImageView(): FaceAndColorDetectImageView {
        return binding.articleImage
    }

    fun setExtract(extract: String?, maxLines: Int) {
        binding.articleExtract.text = StringUtil.fromHtml(extract)
        binding.articleExtract.maxLines = maxLines
    }

    fun getSharedElements(): Array<Pair<View, String>> {
        return TransitionUtil.getSharedElements(context, binding.articleTitle, binding.articleDescription, binding.articleImage)
    }

    fun setImageUri(uri: Uri?, hideInLandscape: Boolean = true) {
        if (uri == null || (DimenUtil.isLandscape(context) && hideInLandscape) || !Prefs.isImageDownloadEnabled) {
            binding.articleImageContainer.visibility = GONE
        } else {
            binding.articleImageContainer.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT,
                    DimenUtil.leadImageHeightForDevice(context) - DimenUtil.getToolbarHeightPx(context))
            binding.articleImageContainer.visibility = VISIBLE
            binding.articleImage.loadImage(uri)
        }
    }

    fun prepareForTransition(title: PageTitle) {
        setImageUri(title.thumbUrl?.toUri())
        if (!DonationReminderHelper.hasActiveReminder) {
            setTitle(title.displayText)
            setDescription(title.description)
        }
        binding.articleDivider.visibility = GONE
        setLayoutDirectionByLang(title.wikiSite.languageCode)
    }
}
