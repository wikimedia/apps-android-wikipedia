package org.wikipedia.feed.onboarding;

import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.wikipedia.R;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.view.DefaultFeedCardView;
import org.wikipedia.views.ItemTouchHelperSwipeAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class OnboardingCardView extends DefaultFeedCardView<OnboardingCard>
        implements ItemTouchHelperSwipeAdapter.SwipeableView {
    public interface Callback {
        void onOnboardingPositiveAction(@NonNull Card card, @NonNull OnboardingCard.OnboardingAction action);
    }

    @BindView(R.id.onboarding_card_image) SimpleDraweeView imageView;
    @BindView(R.id.onboarding_card_header_image) ImageView headerImageView;
    @BindView(R.id.onboarding_card_header_title) TextView headerTitleView;
    @BindView(R.id.onboarding_card_action_positive) TextView actionViewPositive;

    public OnboardingCardView(@NonNull Context context) {
        super(context);
        inflate(context, R.layout.view_card_onboarding, this);
        ButterKnife.bind(this);
    }

    @Override public void setCard(@NonNull OnboardingCard card) {
        super.setCard(card);

        headerImageView.setImageResource(card.headerImage());
        headerTitleView.setText(card.headerText());
        imageView.setImageURI(card.fullImage());
        actionViewPositive.setText(card.positiveText());
    }

    @OnClick(R.id.onboarding_card_action_positive)
    void onPositiveActionClick() {
        if (getCallback() != null && getCard() != null) {
            getCallback().onOnboardingPositiveAction(getCard(), getCard().action());
        }
    }
}
