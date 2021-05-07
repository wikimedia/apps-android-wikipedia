package org.wikipedia.feed.image

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import org.apache.commons.lang3.StringUtils
import org.wikipedia.databinding.ViewCardFeaturedImageBinding
import org.wikipedia.feed.view.DefaultFeedCardView
import org.wikipedia.feed.view.FeedAdapter
import org.wikipedia.richtext.RichTextUtil.stripHtml
import org.wikipedia.views.ImageZoomHelper.Companion.setViewZoomable
import org.wikipedia.views.ViewUtil.adjustImagePlaceholderHeight
import org.wikipedia.views.ViewUtil.loadImage

class FeaturedImageCardView(context: Context) : DefaultFeedCardView<FeaturedImageCard>(context) {

    interface Callback {
        fun onShareImage(card: FeaturedImageCard)
        fun onDownloadImage(image: FeaturedImage)
        fun onFeaturedImageSelected(card: FeaturedImageCard)
    }

    private val binding = ViewCardFeaturedImageBinding.inflate(LayoutInflater.from(context), this, true)

    override var card: FeaturedImageCard? = null
        set(value) {
            field = value
            value?.let {
                image(it.baseImage())
                description(StringUtils.defaultString(it.description()))
                header(it)
                setClickListeners()
            }
        }
    override var callback: FeedAdapter.Callback? = null
        set(value) {
            field = value
            binding.viewFeaturedImageCardHeader.setCallback(value)
        }

    private fun image(image: FeaturedImage) {
        binding.viewFeaturedImageCardContentContainer.post {
            if (!isAttachedToWindow) {
                return@post
            }
            loadImage(image)
        }
    }

    private fun loadImage(image: FeaturedImage) {
        setViewZoomable(binding.viewFeaturedImageCardImage)
        loadImage(binding.viewFeaturedImageCardImage, image.thumbnailUrl)
        binding.viewFeaturedImageCardImagePlaceholder.layoutParams = LayoutParams(
            binding.viewFeaturedImageCardContentContainer.width,
            adjustImagePlaceholderHeight(
                binding.viewFeaturedImageCardContentContainer.width.toFloat(),
                image.thumbnail.width.toFloat(),
                image.thumbnail.height.toFloat()
            )
        )
    }

    private fun description(text: String) {
        binding.viewFeaturedImageCardImageDescription.text = stripHtml(text)
    }

    private fun setClickListeners() {
        binding.viewFeaturedImageCardContentContainer.setOnClickListener(CardClickListener())
        binding.viewFeaturedImageCardDownloadButton.setOnClickListener(CardDownloadListener())
        binding.viewFeaturedImageCardShareButton.setOnClickListener(CardShareListener())
    }

    private fun header(card: FeaturedImageCard) {
        binding.viewFeaturedImageCardHeader.setTitle(card.title())
            .setLangCode(null)
            .setCard(card)
            .setCallback(callback)
    }

    private inner class CardClickListener : OnClickListener {
        override fun onClick(v: View) {
            card?.let {
                callback?.onFeaturedImageSelected(it)
            }
        }
    }

    private inner class CardDownloadListener : OnClickListener {
        override fun onClick(v: View) {
            card?.let {
                callback?.onDownloadImage(it.baseImage())
            }
        }
    }

    private inner class CardShareListener : OnClickListener {
        override fun onClick(v: View) {
            card?.let {
                callback?.onShareImage(it)
            }
        }
    }
}
