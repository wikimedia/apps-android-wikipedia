package org.wikipedia.feed.offline;

import android.content.Context;

import org.wikipedia.R;
import org.wikipedia.feed.view.DefaultFeedCardView;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class OfflineCompilationCardView extends DefaultFeedCardView<OfflineCard> {

    public interface Callback {
        void onViewCompilations();
    }

    public OfflineCompilationCardView(Context context) {
        super(context);
        inflate(getContext(), R.layout.view_card_offline_compilation, this);
        ButterKnife.bind(this);
    }

    @OnClick(R.id.view_offline_action_my_compilations) void onViewCompilationsClick() {
        if (getCallback() != null) {
            getCallback().onViewCompilations();
        }
    }
}
