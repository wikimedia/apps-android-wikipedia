package org.wikipedia.feed.announcement

import android.content.Context
import android.net.Uri
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import org.wikipedia.R
import org.wikipedia.databinding.ViewCardAnnouncementBinding
import org.wikipedia.feed.model.Card
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.StringUtil

class AnnouncementCardView(context: Context) : DefaultFeedCardView<AnnouncementCard>(context) {
    interface Callback {
        fun onAnnouncementPositiveAction(card: Card, uri: Uri)
        fun onAnnouncementNegativeAction(card: Card)
    }

    private val binding = ViewCardAnnouncementBinding.inflate(LayoutInflater.from(context), this, true)
    var localCallback: Callback? = null

    init {
        binding.viewAnnouncementText.movementMethod = LinkMovementMethod.getInstance()
        binding.viewAnnouncementFooterText.movementMethod = LinkMovementMethod.getInstance()
        binding.viewAnnouncementActionPositive.setOnClickListener { onPositiveActionClick() }
        binding.viewAnnouncementDialogActionPositive.setOnClickListener { onPositiveActionClick() }
        binding.viewAnnouncementActionNegative.setOnClickListener { onNegativeActionClick() }
        binding.viewAnnouncementDialogActionNegative.setOnClickListener { onNegativeActionClick() }
    }

    override var card: AnnouncementCard? = null
        set(value) {
            field = value
            value?.let {
                if (!it.extract().isNullOrEmpty()) {
                    binding.viewAnnouncementText.text = StringUtil.fromHtml(it.extract())
                }
                binding.viewAnnouncementCardButtonsContainer.isVisible = it.hasAction()
                if (it.hasAction()) {
                    binding.viewAnnouncementActionPositive.text = it.actionTitle()
                    binding.viewAnnouncementDialogActionPositive.text = it.actionTitle()
                }
                if (!it.negativeText().isNullOrEmpty()) {
                    binding.viewAnnouncementActionNegative.text = it.negativeText()
                    binding.viewAnnouncementDialogActionNegative.text = it.negativeText()
                } else {
                    binding.viewAnnouncementActionNegative.visibility = GONE
                    binding.viewAnnouncementDialogActionNegative.visibility = GONE
                }
                binding.viewAnnouncementHeaderImage.isVisible = it.hasImage()
                if (it.hasImage()) {
                    binding.viewAnnouncementHeaderImage.loadImage(it.image())
                }
                if (it.imageHeight() > 0) {
                    binding.viewAnnouncementHeaderImage.updateLayoutParams<MarginLayoutParams> {
                        height = DimenUtil.roundedDpToPx(it.imageHeight().toFloat())
                    }
                }
                if (it.hasFooterCaption()) {
                    binding.viewAnnouncementFooterText.text = StringUtil.fromHtml(it.footerCaption())
                    RichTextUtil.removeUnderlinesFromLinks(binding.viewAnnouncementFooterText)
                } else {
                    binding.viewAnnouncementFooterText.visibility = GONE
                    binding.viewAnnouncementFooterBorder.visibility = GONE
                }
                if (it.hasBorder()) {
                    binding.viewAnnouncementContainer.strokeColor = ContextCompat.getColor(context, R.color.red30)
                    binding.viewAnnouncementContainer.strokeWidth = 10
                } else {
                    binding.viewAnnouncementContainer.setDefaultBorder()
                }
                if (it.isArticlePlacement) {
                    binding.viewAnnouncementCardButtonsContainer.visibility = GONE
                    binding.viewAnnouncementCardDialogButtonsContainer.visibility = VISIBLE
                    binding.viewAnnouncementContainer.layoutParams = LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    binding.viewAnnouncementContainer.radius = 0f
                }
            }
        }

    private fun onPositiveActionClick() {
        card?.let {
            callback?.onAnnouncementPositiveAction(it, it.actionUri())
            localCallback?.onAnnouncementPositiveAction(it, it.actionUri())
        }
    }

    private fun onNegativeActionClick() {
        card?.let {
            callback?.onAnnouncementNegativeAction(it)
            localCallback?.onAnnouncementNegativeAction(it)
        }
    }
}
