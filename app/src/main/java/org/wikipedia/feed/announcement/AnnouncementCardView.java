package org.wikipedia.feed.announcement;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.widget.ImageView;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.view.DefaultFeedCardView;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.ItemTouchHelperSwipeAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AnnouncementCardView extends DefaultFeedCardView<AnnouncementCard>
        implements ItemTouchHelperSwipeAdapter.SwipeableView {
    public interface Callback {
        void onAnnouncementPositiveAction(@NonNull Uri uri);
        void onAnnouncementNegativeAction(@NonNull Card card);
    }

    @BindView(R.id.view_announcement_header_image) ImageView headerImageView;
    @BindView(R.id.view_announcement_text) TextView textView;
    @BindView(R.id.view_announcement_action_positive) TextView actionViewPositive;
    @BindView(R.id.view_announcement_action_negative) TextView actionViewNegative;
    @BindView(R.id.view_announcement_footer_text) TextView footerTextView;

    public AnnouncementCardView(@NonNull Context context) {
        super(context);
        inflate(context, R.layout.view_card_announcement, this);
        ButterKnife.bind(this);

        setHeaderImageVisible(false);
        setNegativeActionVisible(false);
        footerTextView.setMovementMethod(new LinkMovementMethod());
    }

    @Override public void setCard(@NonNull AnnouncementCard card) {
        super.setCard(card);

        if (!TextUtils.isEmpty(card.extract())) {
            textView.setText(StringUtil.fromHtml(card.extract()));
        }

        if (!card.hasAction()) {
            actionViewPositive.setVisibility(GONE);
            actionViewNegative.setVisibility(GONE);
        } else {
            actionViewPositive.setVisibility(VISIBLE);
            actionViewNegative.setVisibility(VISIBLE);
            actionViewPositive.setText(card.actionTitle());
        }
    }

    @OnClick(R.id.view_announcement_action_positive)
    void onPositiveActionClick() {
        if (getCallback() != null && getCard() != null) {
            getCallback().onAnnouncementPositiveAction(getCard().actionUri());
        }
    }

    @OnClick(R.id.view_announcement_action_negative)
    void onNegativeActionClick() {
        if (getCallback() != null && getCard() != null) {
            getCallback().onAnnouncementNegativeAction(getCard());
        }
    }

    protected void setNegativeActionVisible(boolean visible) {
        actionViewNegative.setVisibility(visible ? VISIBLE : GONE);
    }

    protected void setHeaderImageVisible(boolean visible) {
        headerImageView.setVisibility(visible ? VISIBLE : GONE);
    }

    protected void setHeaderImage(@DrawableRes int id) {
        headerImageView.setImageDrawable(ContextCompat.getDrawable(getContext(), id));
    }

    protected void setFooterText(@NonNull String text) {
        footerTextView.setText(StringUtil.fromHtml(text));
    }
}
