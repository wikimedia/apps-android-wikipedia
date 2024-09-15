package org.wikipedia.descriptions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.ColorInt
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.activity.SingleFragmentActivity
import org.wikipedia.commons.ImagePreviewDialog
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageTitle
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.settings.Prefs
import org.wikipedia.suggestededits.PageSummaryForEdit
import org.wikipedia.util.DeviceUtil

class DescriptionEditActivity : SingleFragmentActivity<DescriptionEditFragment>(), DescriptionEditFragment.Callback {
    enum class Action {
        ADD_DESCRIPTION, TRANSLATE_DESCRIPTION, ADD_CAPTION, TRANSLATE_CAPTION, ADD_IMAGE_TAGS, IMAGE_RECOMMENDATIONS, VANDALISM_PATROL
    }

    private val viewModel: DescriptionEditViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (viewModel.action == Action.ADD_DESCRIPTION && Prefs.isDescriptionEditTutorialEnabled) {
            Prefs.isDescriptionEditTutorialEnabled = false
            startActivity(DescriptionEditTutorialActivity.newIntent(this))
        }
    }

    public override fun createFragment(): DescriptionEditFragment {
        // DescriptionEditFragment can access its activity's extras through the view model, so they
        // do not need to be explicitly passed.
        return DescriptionEditFragment()
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
        val summary = if (action == Action.TRANSLATE_DESCRIPTION) viewModel.targetSummary!! else viewModel.sourceSummary!!
        if (action == Action.ADD_CAPTION || action == Action.TRANSLATE_CAPTION) {
            ExclusiveBottomSheetPresenter.show(supportFragmentManager,
                    ImagePreviewDialog.newInstance(summary, action))
        } else {
            ExclusiveBottomSheetPresenter.show(supportFragmentManager,
                    LinkPreviewDialog.newInstance(HistoryEntry(summary.pageTitle,
                            if (intent.hasExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE) && intent.getSerializableExtra
                                    (Constants.INTENT_EXTRA_INVOKE_SOURCE) === InvokeSource.PAGE_ACTIVITY)
                                HistoryEntry.SOURCE_EDIT_DESCRIPTION else HistoryEntry.SOURCE_SUGGESTED_EDITS)))
        }
    }

    fun updateStatusBarColor(@ColorInt color: Int) {
        setStatusBarColor(color)
    }

    fun updateNavigationBarColor(@ColorInt color: Int) {
        setNavigationBarColor(color)
    }

    companion object {
        fun newIntent(context: Context,
                      title: PageTitle,
                      highlightText: String?,
                      sourceSummary: PageSummaryForEdit?,
                      targetSummary: PageSummaryForEdit?,
                      action: Action,
                      invokeSource: InvokeSource): Intent {
            return Intent(context, DescriptionEditActivity::class.java)
                    .putExtra(Constants.ARG_TITLE, title)
                    .putExtra(Constants.ARG_HIGHLIGHT_TEXT, highlightText)
                    .putExtra(Constants.ARG_SOURCE_SUMMARY, sourceSummary)
                    .putExtra(Constants.ARG_TARGET_SUMMARY, targetSummary)
                    .putExtra(Constants.INTENT_EXTRA_ACTION, action)
                    .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
        }
    }
}
