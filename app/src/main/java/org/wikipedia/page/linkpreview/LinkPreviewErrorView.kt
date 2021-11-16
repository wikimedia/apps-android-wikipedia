package org.wikipedia.page.linkpreview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import org.wikipedia.R
import org.wikipedia.databinding.ViewLinkPreviewErrorBinding
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.util.StringUtil

class LinkPreviewErrorView : LinearLayout {
    interface Callback {
        fun onAddToList()
        fun onDismiss()
        fun onPageMissingMessage(): String
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val binding = ViewLinkPreviewErrorBinding.inflate(LayoutInflater.from(context), this)
    var callback: Callback? = null
    val addToListCallback = OverlayViewAddToListCallback()
    val dismissCallback = OverlayViewDismissCallback()

    fun setError(caught: Throwable?) {
        val errorType = LinkPreviewErrorType[caught]
        binding.viewLinkPreviewErrorIcon.setImageDrawable(AppCompatResources.getDrawable(context, errorType.icon))

        if (errorType === LinkPreviewErrorType.OFFLINE) {
            val message = (resources.getString(R.string.page_offline_notice_cannot_load_while_offline) +
                    "\n" +
                    resources.getString(R.string.page_offline_notice_add_to_reading_list)).trimIndent()
            binding.viewLinkPreviewErrorText.text = message
        } else {
            if (errorType == LinkPreviewErrorType.PAGE_MISSING) {
                binding.viewLinkPreviewErrorText.text = StringUtil.fromHtml(context.resources.getString(errorType.text,
                    callback?.onPageMissingMessage()?.replace("User:","")))
                binding.viewLinkPreviewErrorText.movementMethod = LinkMovementMethodExt.getExternalLinkMovementMethod()
            } else
                binding.viewLinkPreviewErrorText.text = context.resources.getString(errorType.text)
        }
    }

    inner class OverlayViewAddToListCallback : LinkPreviewOverlayView.Callback {
        override fun onPrimaryClick() {
            callback?.onAddToList()
        }
        override fun onSecondaryClick() {}
        override fun onTertiaryClick() {}
    }

    inner class OverlayViewDismissCallback : LinkPreviewOverlayView.Callback {
        override fun onPrimaryClick() {
            callback?.onDismiss()
        }
        override fun onSecondaryClick() {}
        override fun onTertiaryClick() {}
    }
}
