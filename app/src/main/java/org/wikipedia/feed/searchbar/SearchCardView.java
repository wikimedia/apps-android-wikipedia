package org.wikipedia.feed.searchbar;

import android.content.Context;
import android.view.View;

import org.wikipedia.R;
import org.wikipedia.feed.view.DefaultFeedCardView;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class SearchCardView extends DefaultFeedCardView<SearchCard> {
    public interface Callback {
        void onSearchRequested(View view);
        void onVoiceSearchRequested();
    }

    public SearchCardView(Context context) {
        super(context);
        inflate(getContext(), R.layout.view_search_bar, this);
        setCardBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.color_group_22));
        ButterKnife.bind(this);
        FeedbackUtil.setButtonLongPressToast(findViewById(R.id.voice_search_button));
    }

    @OnClick(R.id.search_container) void onSearchClick(View v) {
        if (getCallback() != null) {
            getCallback().onSearchRequested(v);
        }
    }

    @OnClick(R.id.voice_search_button) void onVoiceSearchClick() {
        if (getCallback() != null) {
            getCallback().onVoiceSearchRequested();
        }
    }
}
