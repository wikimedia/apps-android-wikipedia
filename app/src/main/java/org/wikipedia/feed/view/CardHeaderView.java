package org.wikipedia.feed.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.PopupMenu;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.feed.model.Card;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CardHeaderView extends ConstraintLayout {
    public interface Callback {
        boolean onRequestDismissCard(@NonNull Card card);
        void onRequestEditCardLanguages(@NonNull Card card);
        void onRequestCustomize(@NonNull Card card);
    }

    @BindView(R.id.view_card_header_image) AppCompatImageView imageView;
    @BindView(R.id.view_card_header_title) TextView titleView;
    @BindView(R.id.view_card_header_subtitle) TextView subtitleView;
    @BindView(R.id.view_list_card_header_lang_background) View langCodeBackground;
    @BindView(R.id.view_list_card_header_lang_code) TextView langCodeView;
    @Nullable private Card card;
    @Nullable private Callback callback;

    public CardHeaderView(Context context) {
        super(context);
        init();
    }

    public CardHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CardHeaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.view_card_header, this);
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

    @NonNull public CardHeaderView setLangCode(@Nullable String langCode) {
        if (TextUtils.isEmpty(langCode) || WikipediaApp.getInstance().language().getAppLanguageCodes().size() < 2) {
            langCodeBackground.setVisibility(GONE);
            langCodeView.setVisibility(GONE);
        } else {
            langCodeBackground.setVisibility(VISIBLE);
            langCodeView.setVisibility(VISIBLE);
            langCodeView.setText(langCode);
        }
        return this;
    }

    @VisibleForTesting @Nullable Card getCard() {
        return card;
    }

    @OnClick(R.id.view_list_card_header_menu) void onMenuClick(View v) {
        showOverflowMenu(v);
    }

    private void showOverflowMenu(View anchorView) {
        PopupMenu menu = new PopupMenu(anchorView.getContext(), anchorView, Gravity.END);
        menu.getMenuInflater().inflate(R.menu.menu_feed_card_header, menu.getMenu());
        MenuItem editCardLangItem = menu.getMenu().findItem(R.id.menu_feed_card_edit_card_languages);
        editCardLangItem.setVisible(card.type().contentType().isPerLanguage());
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

                case R.id.menu_feed_card_edit_card_languages:
                    if (callback != null & card != null) {
                        callback.onRequestEditCardLanguages(card);
                    }
                    return true;

                case R.id.menu_feed_card_customize:
                    if (callback != null & card != null) {
                        callback.onRequestCustomize(card);
                    }
                    return true;
                default:
                    return false;
            }
        }
    }
}
