package org.wikipedia.views


import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import kotlinx.android.synthetic.main.view_suggested_edits_disabled_states.view.*
import org.wikipedia.R
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil

internal class SuggestedEditsDisabledStatesView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : LinearLayout(context, attrs, defStyle) {

    init {
        View.inflate(context, R.layout.view_suggested_edits_disabled_states, this)
        setUpExternalLinks()
        reset.setOnClickListener { visibility = GONE }
    }

    private fun setUpExternalLinks() {
        editingTipsLayout.setOnClickListener { UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(R.string.about_wikipedia_url))) }
        suggestedEditsHelpLayout.setOnClickListener { UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(R.string.about_wikipedia_url))) }
    }

    fun setMessageText(@StringRes messageRes: Int) {
        messageTextView.text = StringUtil.fromHtml(context.getString(messageRes))
    }

    fun hideImage() {
        image.visibility = GONE
    }

    fun hideHelpLink() {
        suggestedEditsHelpLayout.visibility = GONE
        helpLayoutBorder.visibility = GONE
    }

    fun setImage(@DrawableRes drawableRes: Int) {
        image.setImageResource(drawableRes)
    }

    fun unhideImage() {
        image.visibility = VISIBLE
    }

    fun unhideHelpLink() {
        suggestedEditsHelpLayout.visibility = VISIBLE
        helpLayoutBorder.visibility = VISIBLE
    }
}
