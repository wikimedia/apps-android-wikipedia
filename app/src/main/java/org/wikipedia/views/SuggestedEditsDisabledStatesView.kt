package org.wikipedia.views


import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import kotlinx.android.synthetic.main.view_suggested_edits_disabled_states.view.*
import org.wikipedia.R
import org.wikipedia.util.UriUtil

internal class SuggestedEditsDisabledStatesView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : LinearLayout(context, attrs, defStyle) {

    init {
        View.inflate(context, R.layout.view_suggested_edits_disabled_states, this)
        setUpExternalLinks()
    }

    private fun setUpExternalLinks() {
        editingTipsLayout.setOnClickListener { UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(R.string.about_wikipedia_url))) }
        suggestedEditsHelpLayout.setOnClickListener { UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(R.string.about_wikipedia_url))) }
    }

    fun hideImage() {
        image.visibility = GONE
    }

    fun hideInfoText() {
        infoTextView.visibility = GONE
    }

    fun hideHelpLink() {
        suggestedEditsHelpLayout.visibility = GONE
        helpLayoutBorder.visibility = View.GONE
    }
}
