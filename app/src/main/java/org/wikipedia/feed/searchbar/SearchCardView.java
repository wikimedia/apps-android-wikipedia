package org.wikipedia.feed.searchbar;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.feed.FeedViewCallback;
import org.wikipedia.util.FeedbackUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SearchCardView extends CardView {
    @BindView(R.id.search_container) View searchContainer;
    @BindView(R.id.voice_search_button) View voiceSearchButton;
    @Nullable private FeedViewCallback callback;

    public SearchCardView(Context context) {
        super(context);

        inflate(getContext(), R.layout.view_search_bar, this);
        ButterKnife.bind(this);
        FeedbackUtil.setToolbarButtonLongPressToast(voiceSearchButton);

        searchContainer.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) {
                    callback.onSearchRequested();
                }
            }
        });

        voiceSearchButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (callback != null) {
                    callback.onVoiceSearchRequested();
                }
            }
        });
    }

    @NonNull public SearchCardView setCallback(@Nullable FeedViewCallback callback) {
        this.callback = callback;
        return this;
    }

    public void set(@NonNull SearchCard card) {
    }
}