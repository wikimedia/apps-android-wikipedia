package org.wikipedia.feed.view;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.PopupMenu;
import androidx.constraintlayout.widget.ConstraintLayout;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.feed.model.Card;
import org.wikipedia.util.DimenUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CardHeaderView extends ConstraintLayout {
    public interface Callback {
        boolean onRequestDismissCard(@NonNull Card card);
        void onRequestEditCardLanguages(@NonNull Card card);
        void onRequestCustomize(@NonNull Card card);
    }

    @BindView(R.id.view_card_header_title) TextView titleView;
    @BindView(R.id.view_list_card_header_lang_background) View langCodeBackground;
    @BindView(R.id.view_list_card_header_lang_code) TextView langCodeView;
    @Nullable private Card card;
    @Nullable private Callback callback;
    private static final int PADDING = 4;

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
        setPadding(0, 0, 0, DimenUtil.roundedDpToPx(PADDING));
    }

    @NonNull public CardHeaderView setCard(@NonNull Card card) {
        this.card = card;
        return this;
    }

    @NonNull public CardHeaderView setCallback(@Nullable Callback callback) {
        this.callback = callback;
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
