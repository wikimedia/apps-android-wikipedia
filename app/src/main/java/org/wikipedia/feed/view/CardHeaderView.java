package org.wikipedia.feed.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.feed.model.Card;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CardHeaderView extends FrameLayout {
    public interface Callback {
        boolean onRequestDismissCard(@NonNull Card card);
    }

    @BindView(R.id.view_card_header_image) AppCompatImageView imageView;
    @BindView(R.id.view_card_header_title) TextView titleView;
    @BindView(R.id.view_card_header_subtitle) TextView subtitleView;
    @Nullable private Card card;
    @Nullable private Callback callback;

    public CardHeaderView(Context context) {
        super(context);
        inflate(context, R.layout.view_card_header, this);
        ButterKnife.bind(this);
    }

    @NonNull public CardHeaderView setCard(@NonNull Card card) {
        this.card = card;
        return this;
    }

    @NonNull public CardHeaderView setCallback(@Nullable Callback callback) {
        this.callback = callback;
        return this;
    }

    @NonNull public CardHeaderView setImage(@DrawableRes int resId) {
        imageView.setImageResource(resId);
        return this;
    }

    @NonNull public CardHeaderView setImageCircleColor(@ColorRes int color) {
        ColorStateList colorStateList = new ColorStateList(
                new int[][]{new int[]{}},
                new int[]{ContextCompat.getColor(getContext(), color)}
        );
        ViewCompat.setBackgroundTintList(imageView, colorStateList);
        return this;
    }

    @NonNull public CardHeaderView setTitle(@Nullable CharSequence title) {
        titleView.setText(title);
        return this;
    }

    @NonNull public CardHeaderView setTitle(@StringRes int id) {
        titleView.setText(id);
        return this;
    }

    @NonNull public CardHeaderView setSubtitle(@Nullable CharSequence subtitle) {
        subtitleView.setText(subtitle);
        return this;
    }

    @VisibleForTesting @Nullable Card getCard() {
        return card;
    }

    @OnClick(R.id.view_list_card_header_menu) void onMenuClick(View v) {
        showOverflowMenu(v);
    }

    private void showOverflowMenu(View anchorView) {
        PopupMenu menu = new PopupMenu(anchorView.getContext(), anchorView);
        menu.getMenuInflater().inflate(R.menu.menu_feed_card_header, menu.getMenu());
        menu.setOnMenuItemClickListener(new CardHeaderMenuClickListener());
        menu.show();
    }

    private class CardHeaderMenuClickListener implements PopupMenu.OnMenuItemClickListener {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_feed_card_dismiss:
                    if (callback != null & card != null) {
                        return callback.onRequestDismissCard(card);
                    }
                    return false;
                default:
                    return false;
            }
        }
    }
}
