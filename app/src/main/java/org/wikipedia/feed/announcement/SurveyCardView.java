package org.wikipedia.feed.announcement;

import android.content.Context;
import android.support.annotation.NonNull;

import org.wikipedia.R;

public class SurveyCardView extends AnnouncementCardView {
    public SurveyCardView(@NonNull Context context) {
        super(context);
        setNegativeActionVisible(true);
        setHeaderImageVisible(false);
        setFooterText(context.getString(R.string.view_survey_card_footer));
    }
}
