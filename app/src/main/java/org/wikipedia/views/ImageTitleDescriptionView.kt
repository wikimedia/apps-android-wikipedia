package org.wikipedia.views

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.view_image_title_description.view.*
import org.wikipedia.R

internal class ImageTitleDescriptionView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    init {
        View.inflate(context, R.layout.view_image_title_description, this)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        circularProgressBar.setCurrentProgress(80.0)
    }

    fun setTitle(titleText: String) {
        title.text = titleText
    }

    fun setDescription(descriptionText: String) {
        description.text = descriptionText
    }

    fun setImageDrawable(@DrawableRes imageDrawable: Int) {
        image.setImageDrawable(AppCompatResources.getDrawable(context, imageDrawable))
    }

    fun setGoodnessState(severity: Int) {
        val icon: Int
        val iconTint: Int
        val backgroundTint: Int
        val circleProgress: Double

        when(severity) {
            0 -> { icon = R.drawable.ic_check_black_24dp; iconTint = R.color.green50; backgroundTint = R.color.green90; circleProgress = 100.0 }
            1 -> { icon = R.drawable.ic_check_black_24dp; iconTint = R.color.green50; backgroundTint = R.color.green90; circleProgress = 85.0 }
            2 -> { icon = R.drawable.ic_check_black_24dp; iconTint = R.color.green50; backgroundTint = R.color.green90; circleProgress = 75.0 }
            3 -> { icon = R.drawable.ic_check_black_24dp; iconTint = R.color.green50; backgroundTint = R.color.green90; circleProgress = 55.0 }
            4 -> { icon = R.drawable.ic_check_black_24dp; iconTint = R.color.yellow50; backgroundTint = R.color.yellow90; circleProgress = 40.0 }
            5 -> { icon = R.drawable.ic_check_black_24dp; iconTint = R.color.yellow50; backgroundTint = R.color.yellow90; circleProgress = 30.0 }
            else -> { icon = R.drawable.ic_info_outline_black_24dp; iconTint = R.color.red50; backgroundTint = R.color.red90; circleProgress = 20.0 }
        }

        circularProgressBar.setCurrentProgress(circleProgress)
        circularProgressBar.progressBackgroundColor = ContextCompat.getColor(context, backgroundTint)
        circularProgressBar.progressColor = ContextCompat.getColor(context, iconTint)
        circularProgressBar.visibility = View.VISIBLE

        circularProgressBarOverlay.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, backgroundTint))
        circularProgressBarOverlay.visibility = View.VISIBLE

        image.setImageDrawable(AppCompatResources.getDrawable(context, icon))
        image.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, iconTint))
        image.setPadding(10, 10, 10, 10)
    }
}
