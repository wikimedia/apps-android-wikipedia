package org.wikipedia.views


import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_image_title_description.view.*
import org.wikipedia.R

internal class ImageTitleDescriptionView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : LinearLayout(context, attrs, defStyle) {

    init {
        View.inflate(context, R.layout.view_image_title_description, this)
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    fun setTitle(titleText: String) {
        title.text = titleText
    }

    fun setDescription(descriptionText: String) {
        description.text = descriptionText
    }


    fun setImageDrawable(imageDrawable: Drawable) {
        image.setImageDrawable(imageDrawable)
    }

    fun setImageBackground(drawable: Drawable?) {
        imageBackground.background = drawable

    }

    interface Callback {
        fun onViewClick(task: ImageTitleDescriptionView, isTranslate: Boolean)
    }
}
