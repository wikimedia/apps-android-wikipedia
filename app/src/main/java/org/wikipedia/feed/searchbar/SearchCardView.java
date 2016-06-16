package org.wikipedia.feed.searchbar;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.util.FeedbackUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SearchCardView extends CardView {
    @BindView(R.id.search_container) View searchContainer;
    @BindView(R.id.voice_search_button) View voiceSearchButton;

    public SearchCardView(Context context) {
        super(context);

        inflate(getContext(), R.layout.view_search_bar, this);
        ButterKnife.bind(this);
        FeedbackUtil.setToolbarButtonLongPressToast(voiceSearchButton);
    }

    public void set(@NonNull SearchCard card) {
    }
}