package org.wikipedia.page.linkpreview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.isVisible
import org.wikipedia.databinding.ViewLinkPreviewOverlayBinding

class LinkPreviewOverlayView : FrameLayout {
    interface Callback {
        fun onPrimaryClick()
        fun onSecondaryClick()
        fun onTertiaryClick()
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val binding = ViewLinkPreviewOverlayBinding.inflate(LayoutInflater.from(context), this, true)
    var callback: Callback? = null

    init {
        binding.linkPreviewPrimaryButton.setOnClickListener {
            callback?.onPrimaryClick()
        }
        binding.linkPreviewSecondaryButton.setOnClickListener {
            callback?.onSecondaryClick()
        }
        binding.linkPreviewTertiaryButton.setOnClickListener {
            callback?.onTertiaryClick()
        }
    }

    fun setPrimaryButtonText(text: CharSequence?) {
        binding.linkPreviewPrimaryButton.text = text
    }

    fun showSecondaryButton(show: Boolean) {
        binding.linkPreviewSecondaryButton.isVisible = show
    }

    fun setSecondaryButtonText(text: CharSequence?) {
        binding.linkPreviewSecondaryButton.text = text
    }

    fun showTertiaryButton(show: Boolean) {
        binding.linkPreviewTertiaryButton.isVisible = show
    }
}
