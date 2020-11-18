package org.wikipedia.feed.announcement;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.view.DefaultFeedCardView;
import org.wikipedia.richtext.RichTextUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.FaceAndColorDetectImageView;
import org.wikipedia.views.WikiCardView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

public class AnnouncementCardView extends DefaultFeedCardView<AnnouncementCard> {
    public interface Callback {
        void onAnnouncementPositiveAction(@NonNull Card card, @NonNull Uri uri);
        void onAnnouncementNegativeAction(@NonNull Card card);
    }

    @BindView(R.id.view_announcement_container) WikiCardView container;
    @BindView(R.id.view_announcement_header_image) FaceAndColorDetectImageView headerImageView;
    @BindView(R.id.view_announcement_text) TextView textView;
    @BindView(R.id.view_announcement_action_positive) Button actionViewPositive;
    @BindView(R.id.view_announcement_action_negative) Button actionViewNegative;
    @BindView(R.id.view_announcement_action_positive_dialog) Button actionViewPositiveDialog;
    @BindView(R.id.view_announcement_action_negative_dialog) Button actionViewNegativeDialog;
    @BindView(R.id.view_announcement_card_buttons_container) View actionViewContainer;
    @BindView(R.id.view_announcement_card_dialog_buttons_container) View actionViewDialogsContainer;
    @BindView(R.id.view_announcement_footer_text) TextView footerTextView;
    @BindView(R.id.view_announcement_footer_border) View footerBorderView;
    @Nullable private Callback callback;

    public AnnouncementCardView(@NonNull Context context) {
        super(context);
        inflate(context, R.layout.view_card_announcement, this);
        ButterKnife.bind(this);

        textView.setMovementMethod(LinkMovementMethod.getInstance());
        footerTextView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    @Override public void setCard(@NonNull AnnouncementCard card) {
        super.setCard(card);

        if (!TextUtils.isEmpty(card.extract())) {
            textView.setText(StringUtil.fromHtml(card.extract()));
        }

        if (!card.hasAction()) {
            actionViewContainer.setVisibility(GONE);
        } else {
            actionViewContainer.setVisibility(VISIBLE);
            actionViewPositive.setText(card.actionTitle());
            actionViewPositiveDialog.setText(card.actionTitle());
        }
        if (!TextUtils.isEmpty(card.negativeText())) {
            actionViewNegative.setText(card.negativeText());
            actionViewNegativeDialog.setText(card.negativeText());
        } else {
            actionViewNegative.setVisibility(GONE);
            actionViewNegativeDialog.setVisibility(GONE);
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
            RichTextUtil.removeUnderlinesFromLinks(footerTextView);
        } else {
            footerTextView.setVisibility(GONE);
            footerBorderView.setVisibility(GONE);
        }

        if (card.hasBorder()) {
            container.setStrokeColor(getResources().getColor(R.color.red30));
            container.setStrokeWidth(10);
        } else {
            container.setDefaultBorder();
        }

        if (card.isArticlePlacement()) {
            actionViewContainer.setVisibility(GONE);
            actionViewDialogsContainer.setVisibility(VISIBLE);
            container.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
            container.setRadius(0);
        }
    }

    @OnClick({R.id.view_announcement_action_positive, R.id.view_announcement_action_positive_dialog})
    void onPositiveActionClick() {
        if (getCard() != null) {
            if (getCallback() != null) {
                getCallback().onAnnouncementPositiveAction(getCard(), getCard().actionUri());
            } else if (callback != null) {
                callback.onAnnouncementPositiveAction(getCard(), getCard().actionUri());
            }
        }
    }

    @OnClick({R.id.view_announcement_action_negative, R.id.view_announcement_action_negative_dialog})
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
}
