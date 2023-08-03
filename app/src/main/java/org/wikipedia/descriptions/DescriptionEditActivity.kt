package org.wikipedia.descriptions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.ColorInt
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.analytics.eventplatform.ABTest.Companion.GROUP_1
import org.wikipedia.analytics.eventplatform.MachineGeneratedArticleDescriptionsAnalyticsHelper
import org.wikipedia.auth.AccountUtil
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.readinglist.AddToReadingListDialog
import org.wikipedia.settings.Prefs
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.util.ClipboardUtil
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ReleaseUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.views.ImagePreviewDialog
import org.wikipedia.views.SuggestedArticleDescriptionsDialog

class DescriptionEditActivity : SingleFragmentActivity<DescriptionEditFragment>(), DescriptionEditFragment.Callback, LinkPreviewDialog.Callback {
    enum class Action {
        ADD_DESCRIPTION, TRANSLATE_DESCRIPTION, ADD_CAPTION, TRANSLATE_CAPTION, ADD_IMAGE_TAGS
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val action = intent.getSerializableExtra(Constants.INTENT_EXTRA_ACTION) as Action
        val pageTitle = intent.parcelableExtra<PageTitle>(Constants.ARG_TITLE)!!

        MachineGeneratedArticleDescriptionsAnalyticsHelper.isUserInExperiment = (ReleaseUtil.isPreBetaRelease && AccountUtil.isLoggedIn &&
                action == Action.ADD_DESCRIPTION && pageTitle.description.isNullOrEmpty() &&
                SuggestedArticleDescriptionsDialog.availableLanguages.contains(pageTitle.wikiSite.languageCode))

        val shouldShowAIOnBoarding = MachineGeneratedArticleDescriptionsAnalyticsHelper.isUserInExperiment &&
                MachineGeneratedArticleDescriptionsAnalyticsHelper.abcTest.group != GROUP_1

        if (action == Action.ADD_DESCRIPTION && Prefs.isDescriptionEditTutorialEnabled) {
            Prefs.isDescriptionEditTutorialEnabled = false
            startActivity(DescriptionEditTutorialActivity.newIntent(this, shouldShowAIOnBoarding))
        }
    }

    public override fun createFragment(): DescriptionEditFragment {
        val invokeSource = intent.getSerializableExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE) as InvokeSource
        val action = intent.getSerializableExtra(Constants.INTENT_EXTRA_ACTION) as Action
        val title = intent.parcelableExtra<PageTitle>(Constants.ARG_TITLE)!!
        return DescriptionEditFragment.newInstance(title,
                intent.getStringExtra(EXTRA_HIGHLIGHT_TEXT),
                intent.parcelableExtra(EXTRA_SOURCE_SUMMARY),
                intent.parcelableExtra(EXTRA_TARGET_SUMMARY),
                action,
                invokeSource)
    }

    override fun onBackPressed() {
        if (fragment.binding.fragmentDescriptionEditView.showingReviewContent()) {
            fragment.binding.fragmentDescriptionEditView.loadReviewContent(false)
        } else {
            DeviceUtil.hideSoftKeyboard(this)
            super.onBackPressed()
        }
    }

    override fun onDescriptionEditSuccess() {
        setResult(DescriptionEditSuccessActivity.RESULT_OK_FROM_EDIT_SUCCESS)
        finish()
    }

    override fun onBottomBarContainerClicked(action: Action) {
        val key = if (action == Action.TRANSLATE_DESCRIPTION) EXTRA_TARGET_SUMMARY else EXTRA_SOURCE_SUMMARY
        val summary = intent.parcelableExtra<PageSummaryForEdit>(key)!!
        if (action == Action.ADD_CAPTION || action == Action.TRANSLATE_CAPTION) {
            ExclusiveBottomSheetPresenter.show(supportFragmentManager,
                    ImagePreviewDialog.newInstance(summary, action))
        } else {
            ExclusiveBottomSheetPresenter.show(supportFragmentManager,
                    LinkPreviewDialog.newInstance(HistoryEntry(summary.pageTitle,
                            if (intent.hasExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE) && intent.getSerializableExtra
                                    (Constants.INTENT_EXTRA_INVOKE_SOURCE) === InvokeSource.PAGE_ACTIVITY)
                                HistoryEntry.SOURCE_EDIT_DESCRIPTION else HistoryEntry.SOURCE_SUGGESTED_EDITS), null))
        }
    }

    override fun onLinkPreviewLoadPage(title: PageTitle, entry: HistoryEntry, inNewTab: Boolean) {
        startActivity(PageActivity.newIntentForCurrentTab(this, entry, entry.title))
    }

    override fun onLinkPreviewCopyLink(title: PageTitle) {
        ClipboardUtil.setPlainText(this, text = title.uri)
        FeedbackUtil.showMessage(this, R.string.address_copied)
    }

    override fun onLinkPreviewAddToList(title: PageTitle) {
        ExclusiveBottomSheetPresenter.show(supportFragmentManager,
                AddToReadingListDialog.newInstance(title, InvokeSource.LINK_PREVIEW_MENU))
    }

    override fun onLinkPreviewShareLink(title: PageTitle) {
        ShareUtil.shareText(this, title)
    }

    fun updateStatusBarColor(@ColorInt color: Int) {
        setStatusBarColor(color)
    }

    fun updateNavigationBarColor(@ColorInt color: Int) {
        setNavigationBarColor(color)
    }

    companion object {
        private const val EXTRA_HIGHLIGHT_TEXT = "highlightText"
        private const val EXTRA_SOURCE_SUMMARY = "sourceSummary"
        private const val EXTRA_TARGET_SUMMARY = "targetSummary"

        fun newIntent(context: Context,
                      title: PageTitle,
                      highlightText: String?,
                      sourceSummary: PageSummaryForEdit?,
                      targetSummary: PageSummaryForEdit?,
                      action: Action,
                      invokeSource: InvokeSource): Intent {
            return Intent(context, DescriptionEditActivity::class.java)
                    .putExtra(Constants.ARG_TITLE, title)
                    .putExtra(EXTRA_HIGHLIGHT_TEXT, highlightText)
                    .putExtra(EXTRA_SOURCE_SUMMARY, sourceSummary)
                    .putExtra(EXTRA_TARGET_SUMMARY, targetSummary)
                    .putExtra(Constants.INTENT_EXTRA_ACTION, action)
                    .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
        }
    }
}
