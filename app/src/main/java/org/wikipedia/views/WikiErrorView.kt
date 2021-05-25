package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.content.res.AppCompatResources
import org.wikipedia.R
import org.wikipedia.databinding.ViewWikiErrorBinding
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.util.StringUtil
import org.wikipedia.util.ThrowableUtil.is404
import org.wikipedia.util.ThrowableUtil.isEmptyException
import org.wikipedia.util.ThrowableUtil.isOffline
import org.wikipedia.util.ThrowableUtil.isTimeout

class WikiErrorView : LinearLayout {

    var binding = ViewWikiErrorBinding.inflate(LayoutInflater.from(context), this)
    var retryClickListener: OnClickListener? = null
    var backClickListener: OnClickListener? = null
    var nextClickListener: OnClickListener? = null
    private var movementMethod = LinkMovementMethodExt.getExternalLinkMovementMethod()

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    init {
        binding.viewWikiErrorText.movementMethod = movementMethod
        binding.viewWikiErrorFooterText.movementMethod = movementMethod
    }

    fun setError(caught: Throwable?) {
        val resources = context.resources
        val errorType = getErrorType(caught)
        binding.viewWikiErrorIcon.setImageDrawable(AppCompatResources.getDrawable(context, errorType.icon))
        if (caught is MwException) {
            binding.viewWikiErrorText.text = StringUtil.fromHtml(caught.message)
        } else {
            binding.viewWikiErrorText.text = resources.getString(errorType.text)
        }
        binding.viewWikiErrorButton.text = resources.getString(errorType.buttonText)
        binding.viewWikiErrorButton.setOnClickListener(errorType.buttonClickListener(this))
        when {
            errorType.hasFooterText -> {
                binding.viewWikiErrorFooterLayout.visibility = VISIBLE
                binding.viewWikiErrorFooterText.text = resources.getString(errorType.footerText)
            }
            caught != null && caught !is MwException -> {
                binding.viewWikiErrorFooterLayout.visibility = VISIBLE
                binding.viewWikiErrorFooterText.text = StringUtil.fromHtml(caught.message)
            }
            else -> {
                binding.viewWikiErrorFooterLayout.visibility = GONE
            }
        }
    }

    private fun getErrorType(caught: Throwable?): ErrorType {
        caught?.let {
            when {
                is404(it) -> {
                    return ErrorType.PAGE_MISSING
                }
                isTimeout(it) -> {
                    return ErrorType.TIMEOUT
                }
                isOffline(it) -> {
                    return ErrorType.OFFLINE
                }
                isEmptyException(it) -> {
                    return ErrorType.EMPTY
                }
                else -> { }
            }
        }
        return ErrorType.GENERIC
    }

    enum class ErrorType(@DrawableRes val icon: Int,
                         @StringRes val text: Int,
                         @StringRes val buttonText: Int,
                         @StringRes val footerText: Int = 0) {

        PAGE_MISSING(R.drawable.ic_error_black_24dp, R.string.error_page_does_not_exist,
                R.string.page_error_back_to_main) {
            override fun buttonClickListener(errorView: WikiErrorView): OnClickListener? {
                return errorView.backClickListener
            }
        },
        TIMEOUT(R.drawable.ic_error_black_24dp, R.string.view_wiki_error_message_timeout,
                R.string.offline_load_error_retry) {
            override fun buttonClickListener(errorView: WikiErrorView): OnClickListener? {
                return errorView.retryClickListener
            }
        },
        OFFLINE(R.drawable.ic_portable_wifi_off_black_24px, R.string.view_wiki_error_message_offline,
                R.string.offline_load_error_retry) {
            override fun buttonClickListener(errorView: WikiErrorView): OnClickListener? {
                return errorView.retryClickListener
            }
        },
        EMPTY(R.drawable.ic_error_black_24dp, R.string.error_message_generic,
                R.string.error_next) {
            override fun buttonClickListener(errorView: WikiErrorView): OnClickListener? {
                return errorView.nextClickListener
            }
        },
        GENERIC(R.drawable.ic_error_black_24dp, R.string.error_message_generic,
                R.string.error_back) {
            override fun buttonClickListener(errorView: WikiErrorView): OnClickListener? {
                return errorView.backClickListener
            }
        };

        val hasFooterText: Boolean
            get() {
                return footerText != 0
            }

        abstract fun buttonClickListener(errorView: WikiErrorView): OnClickListener?
    }
}
