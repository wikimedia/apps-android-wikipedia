package org.wikipedia.edits;

import android.app.Activity;
import android.net.Uri;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.snackbar.Snackbar;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.UriUtil;

import static org.wikipedia.settings.Prefs.setShouldShowSuggestedEditsSurvey;

public final class EditsSurvey {
    private static final int VALID_SUGGESTED_EDITS_COUNT_FOR_SURVEY = 3;

    public static void maybeRunSurvey(@NonNull Activity activity) {
        if (Prefs.shouldShowSuggestedEditsSurvey()) {
            Snackbar snackbar = FeedbackUtil.makeSnackbar(activity, activity.getString(R.string.suggested_edits_snackbar_survey_text), FeedbackUtil.LENGTH_MEDIUM);
            TextView actionView = snackbar.getView().findViewById(R.id.snackbar_action);
            actionView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_open_in_new_accent_24, 0);
            actionView.setCompoundDrawablePadding(activity.getResources().getDimensionPixelOffset(R.dimen.margin));
            snackbar.setAction(activity.getString(R.string.suggested_edits_snackbar_survey_action_text), (v) -> openSurveyInBrowser());
            snackbar.show();
            Prefs.setShouldShowSuggestedEditsSurvey(false);
        }
    }

    public static void onEditSuccess() {
        Prefs.setSuggestedEditsCountForSurvey(Prefs.getSuggestedEditsCountForSurvey() + 1);
        if (Prefs.getSuggestedEditsCountForSurvey() == 1 || (Prefs.getSuggestedEditsCountForSurvey() == VALID_SUGGESTED_EDITS_COUNT_FOR_SURVEY && !Prefs.wasSuggestedEditsSurveyClicked())) {
            setShouldShowSuggestedEditsSurvey(true);
        }
    }

    private static void openSurveyInBrowser() {
        Prefs.setSuggestedEditsSurveyClicked(true);
        UriUtil.visitInExternalBrowser(WikipediaApp.getInstance(),
                Uri.parse(WikipediaApp.getInstance().getString(R.string.suggested_edits_survey_url)));
    }

    private EditsSurvey() {
    }
}
