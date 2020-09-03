package org.wikipedia.feed.searchbar;

import android.content.Context;

import org.wikipedia.R;
import org.wikipedia.feed.view.DefaultFeedCardView;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.views.WikiCardView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SearchCardView extends DefaultFeedCardView<SearchCard> {
    public interface Callback {
        void onSearchRequested();
        void onVoiceSearchRequested();
    }

    @BindView(R.id.search_container) WikiCardView wikiCardView;

    public SearchCardView(Context context) {
        super(context);
        inflate(getContext(), R.layout.view_search_bar, this);
        ButterKnife.bind(this);
        wikiCardView.setCardBackgroundColor(ResourceUtil.getThemedColor(context, R.attr.color_group_22));
        FeedbackUtil.setButtonLongPressToast(findViewById(R.id.voice_search_button));
    }

    @OnClick(R.id.search_container) void onSearchClick() {
        if (getCallback() != null) {
            getCallback().onSearchRequested();
        }
    }

    @OnClick(R.id.voice_search_button) void onVoiceSearchClick() {
        if (getCallback() != null) {
            getCallback().onVoiceSearchRequested();
        }
    }
}
