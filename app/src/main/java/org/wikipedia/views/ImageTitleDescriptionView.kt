package org.wikipedia.views

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import org.wikipedia.R
import org.wikipedia.databinding.ViewImageTitleDescriptionBinding
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil

internal class ImageTitleDescriptionView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    private val binding = ViewImageTitleDescriptionBinding.inflate(LayoutInflater.from(context), this)
    var tooltipText: String = ""

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setOnLongClickListener {
            if (tooltipText.isNotEmpty()) {
                FeedbackUtil.showTooltip(context as Activity, binding.description, tooltipText, false, true)
            }
            true
        }
    }

    fun setTitle(titleText: String) {
        binding.title.text = titleText
    }

    fun setDescription(descriptionText: String) {
        binding.description.text = descriptionText
    }

    fun setImageDrawable(@DrawableRes imageDrawable: Int) {
        binding.image.setImageDrawable(AppCompatResources.getDrawable(context, imageDrawable))
    }

    fun getDescriptionView(): View {
        return binding.description
    }

    fun setGoodnessState(severity: Int) {
        val iconRes: Int
        val iconTint: Int
        val backgroundTint: Int
        val textRes: Int
        val circleProgress: Double

        when (severity) {
            0 -> { iconRes = R.drawable.ic_check_borderless; iconTint = R.color.green50; backgroundTint = R.color.green90; textRes = R.string.suggested_edits_quality_perfect_text; circleProgress = 100.0 }
            1 -> { iconRes = R.drawable.ic_check_borderless; iconTint = R.color.green50; backgroundTint = R.color.green90; textRes = R.string.suggested_edits_quality_excellent_text; circleProgress = 85.0 }
            2 -> { iconRes = R.drawable.ic_check_borderless; iconTint = R.color.green50; backgroundTint = R.color.green90; textRes = R.string.suggested_edits_quality_very_good_text; circleProgress = 75.0 }
            3 -> { iconRes = R.drawable.ic_check_borderless; iconTint = R.color.green50; backgroundTint = R.color.green90; textRes = R.string.suggested_edits_quality_good_text; circleProgress = 55.0 }
            4 -> { iconRes = R.drawable.ic_check_borderless; iconTint = R.color.yellow50; backgroundTint = R.color.yellow90; textRes = R.string.suggested_edits_quality_okay_text; circleProgress = 40.0 }
            5 -> { iconRes = R.drawable.ic_check_borderless; iconTint = R.color.yellow50; backgroundTint = R.color.yellow90; textRes = R.string.suggested_edits_quality_sufficient_text; circleProgress = 30.0 }
            else -> { iconRes = R.drawable.ic_exclamation_borderless; iconTint = R.color.red50; backgroundTint = R.color.red90; textRes = R.string.suggested_edits_quality_poor_text; circleProgress = 20.0 }
        }

        binding.circularProgressBar.setCurrentProgress(circleProgress)
        binding.circularProgressBar.progressBackgroundColor = ContextCompat.getColor(context, backgroundTint)
        binding.circularProgressBar.progressColor = ContextCompat.getColor(context, iconTint)
        binding.circularProgressBar.visibility = View.VISIBLE

        ImageViewCompat.setImageTintList(binding.circularProgressBarOverlay, AppCompatResources.getColorStateList(context, backgroundTint))
        binding.circularProgressBarOverlay.visibility = View.VISIBLE

        binding.title.text = context.getString(textRes)

        binding.image.setImageResource(iconRes)
        ImageViewCompat.setImageTintList(binding.image, AppCompatResources.getColorStateList(context, iconTint))

        val params = binding.image.layoutParams
        params.width = DimenUtil.roundedDpToPx(DimenUtil.getDimension(R.dimen.suggested_edits_icon_size) * 3 / 4)
        params.height = params.width
        binding.image.layoutParams = params
        binding.image.requestLayout()
    }
}
