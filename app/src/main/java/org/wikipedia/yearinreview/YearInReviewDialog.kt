package org.wikipedia.yearinreview

import android.app.Activity
import android.text.method.LinkMovementMethod
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.wikipedia.R
import org.wikipedia.auth.AccountUtil
import org.wikipedia.database.AppDatabase
import org.wikipedia.databinding.DialogFeedbackOptionsBinding
import org.wikipedia.readinglist.ReadingListActivity
import org.wikipedia.readinglist.ReadingListMode
import org.wikipedia.settings.Prefs
import org.wikipedia.settings.RemoteConfig
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.yearinreview.YearInReviewViewModel.Companion.CUT_OFF_DATE_FOR_SHOWING_YIR_READING_LIST_DIALOG
import org.wikipedia.yearinreview.YearInReviewViewModel.Companion.MIN_ARTICLES_FOR_CREATING_YIR_READING_LIST
import org.wikipedia.yearinreview.YearInReviewViewModel.Companion.MIN_SLIDES_FOR_CREATING_YIR_READING_LIST
import org.wikipedia.yearinreview.YearInReviewViewModel.Companion.YIR_YEAR
import org.wikipedia.yearinreview.YearInReviewViewModel.Companion.getYearInReviewModel
import java.time.Instant
import java.time.ZoneOffset

object YearInReviewDialog {
    private val userGroup = if (Prefs.appInstallId.hashCode() % 2 == 0) "A" else "B"

    suspend fun maybeShowCreateReadingListDialog(activity: Activity) {
        if (getYearInReviewModel()?.isReadingListDialogShown == true || !AccountUtil.isLoggedIn || !Prefs.isYearInReviewEnabled || (getYearInReviewModel()?.slideViewedCount ?: 0) < MIN_SLIDES_FOR_CREATING_YIR_READING_LIST) {
            return
        }

        val cutoffDate = Instant.parse(CUT_OFF_DATE_FOR_SHOWING_YIR_READING_LIST_DIALOG).toEpochMilli()
        if (System.currentTimeMillis() > cutoffDate) {
            return
        }

        val remoteConfig = RemoteConfig.config.commonv1?.getYirForYear(YIR_YEAR) ?: return
        val startMillis = remoteConfig.dataStartDate.toInstant(ZoneOffset.UTC).toEpochMilli()
        val endMillis = remoteConfig.dataEndDate.toInstant(ZoneOffset.UTC).toEpochMilli()
        val count = AppDatabase.instance.historyEntryDao().getDistinctEntriesCountBetween(startMillis, endMillis)

        if (count < MIN_ARTICLES_FOR_CREATING_YIR_READING_LIST || userGroup == "B") {
            return
        }

        val resource = activity.resources
        val title = resource.getString(R.string.year_in_review_reading_list_dialog_title)
        val message = resource.getString(R.string.year_in_review_reading_list_dialog_message, activity.resources.getString(R.string.year_in_review_reading_list_learn_more))
        MaterialAlertDialogBuilder(activity)
            .setTitle(title)
            .setMessage(StringUtil.fromHtml(message))
            .setPositiveButton(resource.getString(R.string.year_in_review_reading_list_dialog_positive_button_label)) { dialog, _ ->
                YearInReviewViewModel.updateYearInReviewModel { it.copy(isReadingListDialogShown = true) }
                activity.startActivity(ReadingListActivity.newIntent(activity, readingListMode = ReadingListMode.YEAR_IN_REVIEW))
                dialog.dismiss()
            }
            .setCancelable(false)
            .setNegativeButton(resource.getString(R.string.year_in_review_reading_list_dialog_negative_button_label)) { _, _ -> YearInReviewViewModel.updateYearInReviewModel { it.copy(isReadingListDialogShown = true) } }
            .show()
            .findViewById<TextView>(android.R.id.message)?.movementMethod = LinkMovementMethod.getInstance()
    }

    fun maybeShowYirReadingListSurveyDialog(activity: Activity) {
        if (Prefs.yearInReviewReadingListSurveyShown || Prefs.yearInReviewReadingListVisitCount < 2 || userGroup == "B") {
            return
        }

        val binding = DialogFeedbackOptionsBinding.inflate(activity.layoutInflater)
        binding.titleText.text = activity.getString(R.string.year_in_review_reading_list_survey_title)
        binding.messageText.text = activity.getString(R.string.year_in_review_reading_list_survey_subtitle)
        binding.feedbackInputContainer.isVisible = true
        binding.feedbackInputContainer.hint =
            activity.getString(R.string.year_in_review_survey_placeholder_text)

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setCancelable(false)
            .create()

        binding.cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        binding.submitButton.setOnClickListener {
            val selectedOption = getSelectedOption(binding)
            val feedbackText = binding.feedbackInput.text.toString()
            // @TODO: instrumentation
            FeedbackUtil.showMessage(activity, R.string.survey_dialog_submitted_snackbar)
            dialog.dismiss()
        }

        binding.feedbackInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.dialogContainer.postDelayed({
                    if (!activity.isDestroyed) {
                        binding.dialogContainer.fullScroll(ScrollView.FOCUS_DOWN)
                    }
                }, 200)
            }
        }
        Prefs.yearInReviewReadingListSurveyShown = true
        Prefs.yearInReviewReadingListVisitCount = 0
        dialog.show()
    }

    fun maybeShowYearInReviewFeedbackDialog(activity: Activity) {
        if (Prefs.yearInReviewSurveyState != YearInReviewSurveyState.SHOULD_SHOW) {
            return
        }
        val binding = DialogFeedbackOptionsBinding.inflate(activity.layoutInflater)
        binding.titleText.text = activity.getString(R.string.year_in_review_survey_title)
        binding.messageText.text = activity.getString(R.string.year_in_review_survey_subtitle)
        binding.feedbackInputContainer.isVisible = true
        binding.optionVerySatisfied.isVisible = true
        binding.optionVeryUnsatisfied.isVisible = true
        binding.feedbackInputContainer.hint =
            activity.getString(R.string.year_in_review_survey_placeholder_text)

        val dialog = MaterialAlertDialogBuilder(activity)
            .setView(binding.root)
            .setCancelable(false)
            .create()

        binding.cancelButton.setOnClickListener {
            dialog.dismiss()
        }
        binding.submitButton.setOnClickListener {
            val selectedOption = getSelectedOption(binding)
            val feedbackText = binding.feedbackInput.text.toString()
            // @TODO: instrumentation
            FeedbackUtil.showMessage(activity, R.string.survey_dialog_submitted_snackbar)
            dialog.dismiss()
        }

        binding.feedbackInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                binding.dialogContainer.postDelayed({
                    if (!activity.isDestroyed) {
                        binding.dialogContainer.fullScroll(ScrollView.FOCUS_DOWN)
                    }
                }, 200)
            }
        }
        dialog.show()
        Prefs.yearInReviewSurveyState = YearInReviewSurveyState.SHOWN
    }

    private fun getSelectedOption(binding: DialogFeedbackOptionsBinding): Int? {
        val selectedId = binding.feedbackRadioGroup.checkedRadioButtonId
        return when (selectedId) {
            R.id.optionVerySatisfied -> 1
            R.id.optionSatisfied -> 2
            R.id.optionNeutral -> 3
            R.id.optionUnsatisfied -> 4
            R.id.optionVeryUnsatisfied -> 5
            else -> null
        }
    }

    fun resetYearInReviewSurveyState() {
        Prefs.yearInReviewSurveyState = YearInReviewSurveyState.NOT_TRIGGERED
    }
}

enum class YearInReviewSurveyState {
    NOT_TRIGGERED,
    SHOULD_SHOW,
    SHOWN
}
