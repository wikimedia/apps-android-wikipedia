package org.wikipedia.suggestededits

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.widget.ImageViewCompat
import org.wikipedia.R
import org.wikipedia.databinding.ItemSuggestedEditsTypeBinding
import org.wikipedia.userprofile.Contribution.Companion.EDIT_TYPE_GENERIC
import org.wikipedia.util.ResourceUtil

class SuggestedEditsTypeItemView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    private val binding = ItemSuggestedEditsTypeBinding.inflate(LayoutInflater.from(context), this)
    private var editType: Int = EDIT_TYPE_GENERIC
    private var callback: Callback? = null

    interface Callback {
        fun onTypeItemClick(editType: Int)
    }

    init {
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        setOnClickListener {
            callback?.onTypeItemClick(editType)
        }
    }

    fun setEnabledStateUI() {
        binding.editTypeTitle.setTextColor(ResourceUtil.getThemedColor(context, R.attr.themed_icon_color))
        ImageViewCompat.setImageTintList(binding.editTypeImage, AppCompatResources.getColorStateList(context, ResourceUtil.getThemedAttributeId(context, R.attr.themed_icon_color)))
        this.background = ContextCompat.getDrawable(context, R.drawable.rounded_12dp_accent90_fill)
    }

    fun setDisabledStateUI() {
        binding.editTypeTitle.setTextColor(ResourceUtil.getThemedColor(context, R.attr.secondary_text_color))
        ImageViewCompat.setImageTintList(binding.editTypeImage, AppCompatResources.getColorStateList(context, ResourceUtil.getThemedAttributeId(context, R.attr.chart_shade4)))
        this.background = ContextCompat.getDrawable(context, R.drawable.rounded_12dp_corner_base90_fill)
    }

    fun setAttributes(title: String, imageResource: Int, editType: Int, callback: Callback) {
        binding.editTypeTitle.text = title
        binding.editTypeImage.setImageResource(imageResource)
        this.editType = editType
        this.callback = callback
    }
}
