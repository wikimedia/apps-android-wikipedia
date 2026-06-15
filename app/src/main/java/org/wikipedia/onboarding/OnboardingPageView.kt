package org.wikipedia.onboarding

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import org.wikipedia.R
import org.wikipedia.databinding.ViewOnboardingPageBinding
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.util.StringUtil

class OnboardingPageView(context: Context, attrs: AttributeSet? = null) : ConstraintLayout(context, attrs) {
    interface Callback {
        fun onLinkClick(view: OnboardingPageView, url: String)
        fun onListActionButtonClicked(view: OnboardingPageView)
    }

    class DefaultCallback : Callback {
        override fun onLinkClick(view: OnboardingPageView, url: String) {}
        override fun onListActionButtonClicked(view: OnboardingPageView) {}
    }

    var callback: Callback? = null
    private val binding = ViewOnboardingPageBinding.inflate(LayoutInflater.from(context), this)
    private var listDataType: String? = null
    private var viewHeightDetected: Boolean = false

    init {
        attrs?.let { attrSet ->
            context.withStyledAttributes(attrSet, R.styleable.OnboardingPageView) {
                val imageResource = getResourceId(R.styleable.OnboardingPageView_centeredImage, -1)
                val primaryText = getString(R.styleable.OnboardingPageView_primaryText)
                val secondaryText = getString(R.styleable.OnboardingPageView_secondaryText)
                val tertiaryText = getString(R.styleable.OnboardingPageView_tertiaryText)
                listDataType = getString(R.styleable.OnboardingPageView_dataType)
                val background = getDrawable(R.styleable.OnboardingPageView_background)
                val imageSize = getDimension(R.styleable.OnboardingPageView_imageSize, 0f)
                val showPatrollerTasksButtons = getBoolean(R.styleable.OnboardingPageView_patrollerTasksButtons, false)
                background?.let { setBackground(it) }
                binding.imageViewCentered.isVisible = imageResource != -1
                if (imageSize > 0 && imageResource != -1) {
                    val centeredImage = AppCompatResources.getDrawable(context, imageResource)
                    if (centeredImage != null && centeredImage.intrinsicHeight > 0) {
                        binding.imageViewCentered.setImageDrawable(centeredImage)
                        val aspect =
                            centeredImage.intrinsicWidth.toFloat() / centeredImage.intrinsicHeight
                        binding.imageViewCentered.updateLayoutParams {
                            width = imageSize.toInt()
                            height = (imageSize / aspect).toInt()
                        }
                    }
                }
                binding.primaryTextView.visibility = if (primaryText.isNullOrEmpty()) GONE else VISIBLE
                binding.primaryTextView.text = primaryText
                binding.secondaryTextView.visibility = if (secondaryText.isNullOrEmpty()) GONE else VISIBLE
                binding.secondaryTextView.text = StringUtil.fromHtml(secondaryText)
                binding.tertiaryTextView.visibility = if (tertiaryText.isNullOrEmpty()) GONE else VISIBLE
                binding.tertiaryTextView.text = tertiaryText
                binding.secondaryTextView.movementMethod = LinkMovementMethodExt { url: String ->
                    callback?.onLinkClick(this@OnboardingPageView, url)
                }
                binding.patrollerTasksButtonsContainer?.root?.isVisible = showPatrollerTasksButtons
            }
        }
    }

    fun setSecondaryText(text: CharSequence?) {
        binding.secondaryTextView.text = text
    }

    fun setTertiaryTextViewVisible(isVisible: Boolean) {
        binding.tertiaryTextView.isVisible = isVisible
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (!viewHeightDetected) {
            if (binding.scrollView != null && binding.scrollViewContainer != null &&
                    binding.scrollView.height <= binding.scrollViewContainer.height) {
                // Remove layout gravity of the text below on small screens to make centered image visible
                removeScrollViewContainerGravity()
            }
            viewHeightDetected = true
        }
    }

    private fun removeScrollViewContainerGravity() {
        val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        )
        params.gravity = Gravity.NO_GRAVITY
        binding.scrollViewContainer?.layoutParams = params
    }
}
