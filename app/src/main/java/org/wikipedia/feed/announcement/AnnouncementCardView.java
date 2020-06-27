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
import org.wikipedia.databinding.ViewCardAnnouncementBinding;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.view.DefaultFeedCardView;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.FaceAndColorDetectImageView;

public class AnnouncementCardView extends DefaultFeedCardView<AnnouncementCard> {
    public interface Callback {
        void onAnnouncementPositiveAction(@NonNull Card card, @NonNull Uri uri);
        void onAnnouncementNegativeAction(@NonNull Card card);
    }

    private FaceAndColorDetectImageView headerImageView;
    private TextView textView;
    private Button actionViewPositive;
    private Button actionViewNegative;
    private TextView footerTextView;
    private View footerBorderView;
    @Nullable private Callback callback;

    public AnnouncementCardView(@NonNull Context context) {
        super(context);
        final ViewCardAnnouncementBinding binding = ViewCardAnnouncementBinding.bind(this);

        headerImageView = binding.viewAnnouncementHeaderImage;
        textView = binding.viewAnnouncementText;
        actionViewPositive = binding.viewAnnouncementActionPositive;
        actionViewNegative = binding.viewAnnouncementActionNegative;
        footerTextView = binding.viewAnnouncementFooterText;
        footerBorderView = binding.viewAnnouncementFooterBorder;

        actionViewPositive.setOnClickListener(v -> {
            if (getCard() != null) {
                if (getCallback() != null) {
                    getCallback().onAnnouncementPositiveAction(getCard(), getCard().actionUri());
                } else if (callback != null) {
                    callback.onAnnouncementPositiveAction(getCard(), getCard().actionUri());
                }
            }
        });
        actionViewNegative.setOnClickListener(v -> {
            if (getCard() != null) {
                if (getCallback() != null) {
                    getCallback().onAnnouncementNegativeAction(getCard());
                } else if (callback != null) {
                    callback.onAnnouncementNegativeAction(getCard());
                }
            }
        });

        setNegativeActionVisible(true);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
        footerTextView.setMovementMethod(LinkMovementMethod.getInstance());
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

    public void setCallback(@NonNull Callback callback) {
        this.callback = callback;
    }

    protected void setNegativeActionVisible(boolean visible) {
        actionViewNegative.setVisibility(visible ? VISIBLE : GONE);
    }
}
