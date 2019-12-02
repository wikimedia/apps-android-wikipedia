package org.wikipedia.feed.announcement;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.view.DefaultFeedCardView;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.FaceAndColorDetectImageView;
import org.wikipedia.views.ItemTouchHelperSwipeAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class AnnouncementCardView extends DefaultFeedCardView<AnnouncementCard>
        implements ItemTouchHelperSwipeAdapter.SwipeableView {
    public interface Callback {
        void onAnnouncementPositiveAction(@NonNull Card card, @NonNull Uri uri);
        void onAnnouncementNegativeAction(@NonNull Card card);
    }

    @BindView(R.id.view_announcement_header_image) FaceAndColorDetectImageView headerImageView;
    @BindView(R.id.view_announcement_text) TextView textView;
    @BindView(R.id.view_announcement_action_positive) Button actionViewPositive;
    @BindView(R.id.view_announcement_action_negative) Button actionViewNegative;
    @BindView(R.id.view_announcement_footer_text) TextView footerTextView;
    @BindView(R.id.view_announcement_footer_border) View footerBorderView;
    @Nullable private Callback callback;

    public AnnouncementCardView(@NonNull Context context) {
        super(context);
        inflate(context, R.layout.view_card_announcement, this);
        ButterKnife.bind(this);

        setNegativeActionVisible(true);
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
        if (!TextUtils.isEmpty(card.negativeText())) {
            actionViewNegative.setText(card.negativeText());
        } else {
            actionViewNegative.setVisibility(GONE);
        }

        if (card.hasImage()) {
            headerImageView.setVisibility(VISIBLE);
            headerImageView.loadImage(card.image());
        } else {
            headerImageView.setVisibility(GONE);
        }

        if (card.imageHeight() > 0) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) headerImageView.getLayoutParams();
            params.height = DimenUtil.roundedDpToPx(card.imageHeight());
            headerImageView.setLayoutParams(params);
        }

        if (card.hasFooterCaption()) {
            footerTextView.setText(StringUtil.fromHtml(card.footerCaption()));
        } else {
            footerTextView.setVisibility(GONE);
            footerBorderView.setVisibility(GONE);
        }

        if (card.hasBorder()) {
            setStrokeColor(getResources().getColor(R.color.red30));
            setStrokeWidth(10);
            setRadius(0);
        } else {
            setStrokeWidth(0);
        }
    }

    @OnClick(R.id.view_announcement_action_positive)
    void onPositiveActionClick() {
        if (getCard() != null) {
            if (getCallback() != null) {
                getCallback().onAnnouncementPositiveAction(getCard(), getCard().actionUri());
            } else if (callback != null) {
                callback.onAnnouncementPositiveAction(getCard(), getCard().actionUri());
            }
        }
    }

    @OnClick(R.id.view_announcement_action_negative)
    void onNegativeActionClick() {
        if (getCard() != null) {
            if (getCallback() != null) {
                getCallback().onAnnouncementNegativeAction(getCard());
            } else if (callback != null) {
                callback.onAnnouncementNegativeAction(getCard());
            }
        }
    }

    public void setCallback(@NonNull Callback callback) {
        this.callback = callback;
    }

    protected void setNegativeActionVisible(boolean visible) {
        actionViewNegative.setVisibility(visible ? VISIBLE : GONE);
    }
}
