package org.wikipedia.views

import android.app.Activity
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.core.view.updateLayoutParams
import androidx.core.widget.ImageViewCompat
import org.wikipedia.R
import org.wikipedia.databinding.ViewImageTitleDescriptionBinding
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil

internal class ImageTitleDescriptionView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    private val binding = ViewImageTitleDescriptionBinding.inflate(LayoutInflater.from(context), this)
    var tooltipText: String = ""

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setOnLongClickListener {
            if (tooltipText.isNotEmpty()) {
                FeedbackUtil.showTooltip(context as Activity, binding.description, tooltipText,
                    aboveOrBelow = false,
                    autoDismiss = true
                )
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
        binding.image.setImageResource(imageDrawable)
    }

    fun getDescriptionView(): View {
        return binding.description
    }

    fun setGoodnessState(severity: Int) {
        val iconRes: Int
        val iconTint: Int
        val textRes: Int

        when (severity) {
            0 -> { iconRes = R.drawable.ic_check_borderless; iconTint = R.attr.success_color; textRes = R.string.suggested_edits_quality_perfect_text; }
            1 -> { iconRes = R.drawable.ic_check_borderless; iconTint = R.attr.success_color; textRes = R.string.suggested_edits_quality_excellent_text; }
            2 -> { iconRes = R.drawable.ic_check_borderless; iconTint = R.attr.success_color; textRes = R.string.suggested_edits_quality_very_good_text; }
            3 -> { iconRes = R.drawable.ic_check_borderless; iconTint = R.attr.success_color; textRes = R.string.suggested_edits_quality_good_text; }
            4 -> { iconRes = R.drawable.ic_check_borderless; iconTint = R.attr.highlight_color; textRes = R.string.suggested_edits_quality_okay_text; }
            5 -> { iconRes = R.drawable.ic_check_borderless; iconTint = R.attr.highlight_color; textRes = R.string.suggested_edits_quality_sufficient_text; }
            else -> { iconRes = R.drawable.ic_exclamation_borderless; iconTint = R.attr.destructive_color; textRes = R.string.suggested_edits_quality_poor_text; }
        }

        binding.circularProgressBar.setCurrentProgress(100.0) // TODO: verify if we are no longer need the percentage.
        binding.circularProgressBar.progressColor = ResourceUtil.getThemedColor(context, iconTint)
        binding.circularProgressBar.visibility = View.VISIBLE

        ImageViewCompat.setImageTintList(binding.circularProgressBarOverlay, ResourceUtil.getThemedColorStateList(context, R.attr.paper_color))
        binding.circularProgressBarOverlay.visibility = View.VISIBLE

        binding.title.text = context.getString(textRes)

        binding.image.setImageResource(iconRes)
        ImageViewCompat.setImageTintList(binding.image, ResourceUtil.getThemedColorStateList(context, iconTint))

        binding.image.updateLayoutParams {
            width = DimenUtil.roundedDpToPx(DimenUtil.getDimension(R.dimen.suggested_edits_icon_size) * 3 / 4)
            height = width
        }
        binding.image.requestLayout()
    }
}
