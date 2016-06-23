package org.wikipedia.feed.random;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.feed.view.StaticCardView;

public class RandomCardView extends StaticCardView {
    public RandomCardView(@NonNull Context context) {
        super(context);
    }

    public void set(@NonNull RandomCard card) {
        setTitle(getString(R.string.view_random_card_title));
        setSubtitle(String.format(getString(R.string.view_random_card_subtitle),
                WikipediaApp.getInstance().getAppLanguageLocalizedName(card.site().languageCode())));
    }

    private String getString(@StringRes int id) {
        return getResources().getString(id);
    }
}
