package org.wikipedia.views

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.android.synthetic.main.view_dialog_title_with_image.view.*
import org.wikipedia.R

@SuppressLint("ViewConstructor")
class DialogTitleWithImage(
        context: Context,
        @StringRes titleRes: Int,
        @DrawableRes imageRes: Int,
        private val preserveImageAspect: Boolean
) : LinearLayout(context) {

    init {
        View.inflate(context, R.layout.view_dialog_title_with_image, this)
        orientation = VERTICAL
        title.setText(titleRes)
        image.setImageResource(imageRes)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        if (image.drawable != null && preserveImageAspect) {
            val params = image.layoutParams as LayoutParams
            params.height = (image.drawable.intrinsicHeight.toDouble() / image.drawable.intrinsicWidth * image.width).toInt()
            image.layoutParams = params
        }
    }
}
