package org.wikipedia.views


import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity.CENTER_HORIZONTAL
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.view_image_title_description.view.*
import org.wikipedia.R
import org.wikipedia.util.DimenUtil

internal class ImageTitleDescriptionView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : LinearLayout(context, attrs, defStyle) {

    init {
        View.inflate(context, R.layout.view_image_title_description, this)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        circularProgressBar.setCurrentProgress(80.0)
    }

    fun setTitle(titleText: String) {
        title.text = titleText
    }

    fun setTitleTextSize(textSize: Float) {
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
    }

    fun setCaseForTitle(isAllCaps: Boolean) {
        title.isAllCaps = isAllCaps
    }

    fun setDescription(descriptionText: String) {
        description.text = descriptionText
    }

    fun setDescriptionTextSize(textSize: Float) {
        description.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
    }

    fun setImageDrawable(imageDrawable: Drawable) {
        image.setImageDrawable(imageDrawable)
    }

    fun setImageTint(color: Int) {
        image.imageTintList = ColorStateList.valueOf(ContextCompat.getColor(context, color))
    }

    fun setImageParams(width: Int, height: Int) {
        val parms = LayoutParams(width, height)
        image.setLayoutParams(parms)
        image.requestLayout()
    }

    fun setImageBackgroundParams(width: Int, height: Int) {
        val parms = FrameLayout.LayoutParams(width, height)
        imageBackground.setLayoutParams(parms)
        imageBackground.requestLayout()
    }

    fun setImageBackgroundTint(color: Int) {
        imageBackground.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(context, color))
    }

    fun setImageBackground(drawable: Drawable?) {
        imageBackground.background = drawable

    }

    fun showCircularProgressBar(show: Boolean) {
        circularProgressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun setUpViewForTablet() {
        imageTitleDescParentLayout.orientation = VERTICAL
        imageTitleDescParentLayout.gravity = CENTER_HORIZONTAL
        val params = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        params.setMargins(0, 0, 0, 0)
        imageTitleDescParentLayout.layoutParams = params
        val layoutParams = LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, 0.0f)
        layoutParams.setMargins(DimenUtil.roundedDpToPx(160.0f), 0, DimenUtil.roundedDpToPx(160.0f), 0)
        titleDescriptionLayout.layoutParams = layoutParams
        titleDescriptionLayout.gravity = CENTER_HORIZONTAL
        description.textAlignment = View.TEXT_ALIGNMENT_CENTER
    }

    interface Callback {
        fun onViewClick(task: ImageTitleDescriptionView, isTranslate: Boolean)
    }
}
