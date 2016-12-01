package org.wikipedia.feed.announcement;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.feed.view.DefaultFeedCardView;
import org.wikipedia.views.ItemTouchHelperSwipeAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AnnouncementCardView extends DefaultFeedCardView<AnnouncementCard>
        implements ItemTouchHelperSwipeAdapter.SwipeableView {
    public interface Callback {
        void onAnnouncementAction(@NonNull Uri uri);
    }

    @BindView(R.id.view_announcement_text) TextView textView;
    @BindView(R.id.view_announcement_action) TextView actionView;

    public AnnouncementCardView(Context context) {
        super(context);
        inflate(context, R.layout.view_card_announcement, this);
        ButterKnife.bind(this);
    }

    @Override public void setCard(@NonNull AnnouncementCard card) {
        super.setCard(card);

        textView.setText(card.extract());

        if (!card.hasAction()) {
            actionView.setVisibility(GONE);
        } else {
            actionView.setVisibility(VISIBLE);
            actionView.setText(card.actionTitle());
        }
    }

    @OnClick(R.id.view_announcement_action)
    void onActionClick() {
        if (getCallback() != null && getCard() != null) {
            getCallback().onAnnouncementAction(getCard().actionUri());
        }
    }
}
