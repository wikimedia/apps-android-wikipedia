package org.wikipedia.feed.becauseyouread;

import android.support.annotation.NonNull;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.feed.model.ListCard;
import org.wikipedia.util.StringUtil;

import java.util.List;

public class BecauseYouReadCard extends ListCard<BecauseYouReadItemCard> {
    @NonNull private String title;

    public BecauseYouReadCard(@NonNull final String title,
                              @NonNull final List<BecauseYouReadItemCard> itemCards) {
        super(itemCards);
        this.title = StringUtil.removeUnderscores(title);
    }

    @Override
    @NonNull
    public String title() {
        return WikipediaApp.getInstance().getString(R.string.view_because_you_read_card_title);
    }

    @Override
    @NonNull
    public String subtitle() {
        return title;
    }
}
