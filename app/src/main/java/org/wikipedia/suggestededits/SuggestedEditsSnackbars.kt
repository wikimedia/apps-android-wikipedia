package org.wikipedia.suggestededits

import android.app.Activity
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.ABTestSuggestedEditsSnackbarFunnel
import org.wikipedia.descriptions.DescriptionEditActivity.Action
import org.wikipedia.util.FeedbackUtil

object SuggestedEditsSnackbars {

    interface OpenPageListener {
        fun open()
    }

    @JvmStatic
    fun show(activity: Activity, action: Action?, targetLanguageCode: String? = null, enableViewAction: Boolean = false, listener: OpenPageListener? = null) {
        val app = WikipediaApp.getInstance()
        val abTestFunnel = ABTestSuggestedEditsSnackbarFunnel()
        if (!(action == Action.ADD_IMAGE_TAGS && !abTestFunnel.shouldSeeSnackbarAction())) {
            val snackbar = FeedbackUtil.makeSnackbar(activity,
                    if ((action == Action.TRANSLATE_DESCRIPTION || action == Action.TRANSLATE_CAPTION)
                            && app.language().appLanguageCodes.size > 1) activity.getString(
                            if (action == Action.TRANSLATE_DESCRIPTION)
                                if (abTestFunnel.shouldSeeSnackbarAction())
                                    R.string.description_edit_success_saved_in_lang_snackbar_se_promotion
                                else
                                    R.string.description_edit_success_saved_in_lang_snackbar
                            else if (abTestFunnel.shouldSeeSnackbarAction())
                                R.string.description_edit_success_saved_image_caption_in_lang_snackbar_se_promotion
                            else
                                R.string.description_edit_success_saved_image_caption_in_lang_snackbar, app.language().getAppLanguageLocalizedName(targetLanguageCode))
                    else activity.getString(
                            if (action == Action.ADD_DESCRIPTION)
                                if (abTestFunnel.shouldSeeSnackbarAction())
                                    R.string.description_edit_success_saved_snackbar_se_promotion
                                else R.string.description_edit_success_saved_snackbar
                            else if (action == Action.ADD_IMAGE_TAGS)
                                if (abTestFunnel.shouldSeeSnackbarAction())
                                    R.string.description_edit_success_saved_image_tags_snackbar_se_promotion
                                else R.string.description_edit_success_saved_image_tags_snackbar
                            else if (abTestFunnel.shouldSeeSnackbarAction())
                                R.string.description_edit_success_saved_image_caption_snackbar_se_promotion
                            else R.string.description_edit_success_saved_image_caption_snackbar), FeedbackUtil.LENGTH_DEFAULT)
            if (abTestFunnel.shouldSeeSnackbarAction() && action != null) {
                snackbar.setAction(R.string.suggested_edits_tasks_onboarding_get_started) { activity.startActivity(SuggestionsActivity.newIntent(activity, action)) }
            } else if (enableViewAction && listener != null) {
                snackbar.setAction(R.string.suggested_edits_article_cta_snackbar_action) { listener.open() }
            }
            snackbar.show()
        }
        abTestFunnel.logSnackbarShown()
    }
}
