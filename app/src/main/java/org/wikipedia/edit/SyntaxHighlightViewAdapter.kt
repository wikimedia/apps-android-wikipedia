package org.wikipedia.edit

import android.content.Intent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import org.wikipedia.Constants
import org.wikipedia.analytics.eventplatform.PatrollerExperienceEvent
import org.wikipedia.edit.insertmedia.InsertMediaActivity
import org.wikipedia.edit.templates.TemplatesSearchActivity
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageTitle
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.search.SearchActivity

class SyntaxHighlightViewAdapter(
    val activity: AppCompatActivity,
    val pageTitle: PageTitle,
    val editText: SyntaxHighlightableEditText,
    private val wikiTextKeyboardView: WikiTextKeyboardView,
    private val wikiTextKeyboardFormattingView: WikiTextKeyboardFormattingView,
    private val wikiTextKeyboardHeadingsView: WikiTextKeyboardHeadingsView,
    private val invokeSource: Constants.InvokeSource,
    private val requestInsertMedia: ActivityResultLauncher<Intent>,
    private val isFromDiff: Boolean = false,
    showUserMention: Boolean = false
) : WikiTextKeyboardView.Callback {

    init {
        wikiTextKeyboardView.editText = editText
        wikiTextKeyboardView.callback = this
        wikiTextKeyboardFormattingView.editText = editText
        wikiTextKeyboardFormattingView.callback = this
        wikiTextKeyboardHeadingsView.editText = editText
        wikiTextKeyboardHeadingsView.callback = this
        wikiTextKeyboardView.userMentionVisible = showUserMention
        hideAllSyntaxModals()

        ViewCompat.setOnApplyWindowInsetsListener(activity.window.decorView) { _, insets ->
            val imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            hideAllSyntaxModals()
            wikiTextKeyboardView.isVisible = imeVisible && editText.isFocused
            insets
        }

        editText.setOnFocusChangeListener { _, hasFocus ->
            activity.window.decorView.requestApplyInsets()
        }
    }

    private val requestLinkFromSearch = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == SearchActivity.RESULT_LINK_SUCCESS) {
            it.data?.parcelableExtra<PageTitle>(SearchActivity.EXTRA_RETURN_LINK_TITLE)?.let { title ->
                wikiTextKeyboardView.insertLink(title, pageTitle.wikiSite.languageCode)
            }
        }
    }

    private val requestInsertTemplate = activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == TemplatesSearchActivity.RESULT_INSERT_TEMPLATE_SUCCESS) {
            it.data?.let { data ->
                val newWikiText = data.getStringExtra(TemplatesSearchActivity.RESULT_WIKI_TEXT)
                editText.inputConnection?.commitText(newWikiText, 1)
            }
        }
    }

    override fun onPreviewLink(title: String) {
        ExclusiveBottomSheetPresenter.show(activity.supportFragmentManager,
            LinkPreviewDialog.newInstance(HistoryEntry(PageTitle(title, pageTitle.wikiSite), HistoryEntry.SOURCE_INTERNAL_LINK)))
    }

    override fun onRequestInsertMedia() {
        requestInsertMedia.launch(InsertMediaActivity.newIntent(activity, pageTitle.wikiSite,
            if (invokeSource == Constants.InvokeSource.EDIT_ACTIVITY) pageTitle.displayText else "",
            invokeSource))
    }

    override fun onRequestInsertTemplate() {
        if (isFromDiff) {
            val activeInterface = if (invokeSource == Constants.InvokeSource.TALK_REPLY_ACTIVITY) "pt_talk" else "pt_edit"
            PatrollerExperienceEvent.logAction("template_init", activeInterface)
        }
        requestInsertTemplate.launch(TemplatesSearchActivity.newIntent(activity, pageTitle.wikiSite, isFromDiff, invokeSource))
    }

    override fun onRequestInsertLink() {
        requestLinkFromSearch.launch(SearchActivity.newIntent(activity, invokeSource, null, true))
    }

    override fun onRequestHeading() {
        if (wikiTextKeyboardHeadingsView.isVisible) {
            hideAllSyntaxModals()
            return
        }
        hideAllSyntaxModals()
        wikiTextKeyboardHeadingsView.isVisible = true
        wikiTextKeyboardView.onAfterHeadingsShown()
    }

    override fun onRequestFormatting() {
        if (wikiTextKeyboardFormattingView.isVisible) {
            hideAllSyntaxModals()
            return
        }
        hideAllSyntaxModals()
        wikiTextKeyboardFormattingView.isVisible = true
        wikiTextKeyboardView.onAfterFormattingShown()
    }

    override fun onSyntaxOverlayCollapse() {
        hideAllSyntaxModals()
    }

    private fun hideAllSyntaxModals() {
        wikiTextKeyboardHeadingsView.isVisible = false
        wikiTextKeyboardFormattingView.isVisible = false
        wikiTextKeyboardView.onAfterOverlaysHidden()
    }
}
