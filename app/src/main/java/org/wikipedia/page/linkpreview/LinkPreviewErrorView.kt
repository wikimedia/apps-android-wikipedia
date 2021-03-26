package org.wikipedia.page.linkpreview

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import butterknife.ButterKnife
import org.wikipedia.R
import org.wikipedia.databinding.ViewLinkPreviewErrorBinding

class LinkPreviewErrorView : LinearLayout {
    interface Callback {
        fun onAddToList()
        fun onDismiss()
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val binding = ViewLinkPreviewErrorBinding.inflate(LayoutInflater.from(context), this)
    var callback: Callback? = null
    val addToListCallback = OverlayViewAddToListCallback()
    val dismissCallback = OverlayViewDismissCallback()

    init {
        inflate(context, R.layout.view_link_preview_error, this)
        ButterKnife.bind(this)
    }

    fun setError(caught: Throwable?) {
        val errorType = LinkPreviewErrorType.get(caught)
        binding.viewLinkPreviewErrorIcon.setImageDrawable(AppCompatResources.getDrawable(context, errorType.icon))

        if (errorType === LinkPreviewErrorType.OFFLINE) {
            val message = (resources.getString(R.string.page_offline_notice_cannot_load_while_offline) +
                    resources.getString(R.string.page_offline_notice_add_to_reading_list)).trimIndent()
            binding.viewLinkPreviewErrorText.text = message
        } else {
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
