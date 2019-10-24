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

internal class SuggestedEditsDisabledStatesView constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {

    init {
        View.inflate(context, R.layout.view_suggested_edits_disabled_states, this)
        setUpExternalLinks()
    }

    private fun setUpExternalLinks() {
        editingTipsLayout.setOnClickListener { UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(R.string.about_wikipedia_url))) }
        suggestedEditsHelpLayout.setOnClickListener { UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(R.string.about_wikipedia_url))) }
    }

    fun setMessageText(text: String) {
        messageTextView.text = StringUtil.fromHtml(text)
    }

    fun hideImage() {
        image.visibility = GONE
    }

    fun hideTipsLink() {
        editingTipsLayout.visibility = GONE
        linksDivider.visibility = GONE
    }

    fun setImage(@DrawableRes drawableRes: Int) {
        image.setImageResource(drawableRes)
    }

    fun showImage() {
        image.visibility = VISIBLE
    }

    fun showTipsLink() {
        editingTipsLayout.visibility = VISIBLE
        linksDivider.visibility = VISIBLE
    }
}
