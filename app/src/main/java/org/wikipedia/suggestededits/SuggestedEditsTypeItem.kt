package org.wikipedia.suggestededits

import android.content.Context
import android.content.res.ColorStateList
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import kotlinx.android.synthetic.main.item_suggested_edits_type.view.*
import org.wikipedia.R
import org.wikipedia.suggestededits.Contribution.Companion.ALL_EDIT_TYPES
import org.wikipedia.util.ResourceUtil

class SuggestedEditsTypeItem constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    var editType: Int = ALL_EDIT_TYPES
    private var callback: Callback? = null

    interface Callback {
        fun onClick(view: SuggestedEditsTypeItem)
    }

    init {
        View.inflate(context, R.layout.item_suggested_edits_type, this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            foreground = AppCompatResources.getDrawable(context, ResourceUtil.getThemedAttributeId(context, R.attr.selectableItemBackground))
        }
        setOnClickListener {
            if (callback != null) {
                callback!!.onClick(this)
            }
        }
    }

    fun setTitle(contributionTitle: String?) {
        title.text = contributionTitle
    }

    fun setImageResource(@DrawableRes imageResource: Int) {
        image.setImageResource(imageResource)
    }

    fun setEnabledStateUI() {
        title.setTextColor(ResourceUtil.getThemedColor(context, R.attr.colorAccent))
        ImageViewCompat.setImageTintList(image, ColorStateList.valueOf(ContextCompat.getColor(context, ResourceUtil.getThemedAttributeId(context, R.attr.colorAccent))))
        this.background = ContextCompat.getDrawable(context, R.drawable.rounded_12dp_accent90_fill)
    }

    fun setDisabledStateUI() {
        title.setTextColor(ResourceUtil.getThemedColor(context, R.attr.secondary_text_color))
        ImageViewCompat.setImageTintList(image, ColorStateList.valueOf(ContextCompat.getColor(context, ResourceUtil.getThemedAttributeId(context, R.attr.chart_shade4))))
        this.background = ContextCompat.getDrawable(context, R.drawable.rounded_12dp_corner_base90_fill)
    }

    fun setAttributes(title: String, imageResource: Int, editType: Int, callback: Callback) {
        setTitle(title)
        setImageResource(imageResource)
        this.editType = editType
        this.callback = callback
    }
}