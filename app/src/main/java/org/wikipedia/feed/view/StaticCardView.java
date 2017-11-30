package org.wikipedia.feed.view;

import android.content.Context;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.feed.model.Card;
import org.wikipedia.views.ItemTouchHelperSwipeAdapter;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public abstract class StaticCardView<T extends Card> extends DefaultFeedCardView<T>
        implements ItemTouchHelperSwipeAdapter.SwipeableView {
    @BindView(R.id.view_static_card_container) View containerView;
    @BindView(R.id.view_static_card_title) TextView title;
    @BindView(R.id.view_static_card_subtitle) TextView subtitle;
    @BindView(R.id.view_static_card_icon) ImageView icon;
    @BindView(R.id.view_static_card_progress) View progress;
    @BindView(R.id.view_static_card_action_icon) ImageView actionIcon;
    @BindView(R.id.view_static_card_action_text) TextView actionText;

    public StaticCardView(Context context) {
        super(context);

        inflate(getContext(), R.layout.view_static_card, this);
        ButterKnife.bind(this);
        setProgress(false);
    }

    protected void setTitle(CharSequence text) {
        title.setText(text);
    }

    protected void setSubtitle(CharSequence text) {
        subtitle.setText(text);
    }

    protected void setIcon(@DrawableRes int id) {
        icon.setImageResource(id);
    }

    protected void setContainerBackground(@ColorRes int color) {
        containerView.setBackgroundColor(ContextCompat.getColor(getContext(), color));
    }

    protected void setAction(@DrawableRes int iconId, @StringRes int textId) {
        actionIcon.setImageResource(iconId);
        actionText.setText(textId);
    }

    protected void setProgress(boolean enabled) {
        icon.setVisibility(enabled ? GONE : VISIBLE);
        progress.setVisibility(enabled ? VISIBLE : GONE);
    }

    protected String getString(@StringRes int id) {
        return getResources().getString(id);
    }

    @OnClick(R.id.view_static_card_action_overflow) void onMenuClick(View v) {
        showOverflowMenu(v);
    }

    @OnClick(R.id.view_static_card_container) protected void onContentClick(View v) {
    }

    @OnClick(R.id.view_static_card_action_container) protected void onActionClick(View v) {
    }

    private void showOverflowMenu(View anchorView) {
        PopupMenu menu = new PopupMenu(anchorView.getContext(), anchorView);
        menu.getMenuInflater().inflate(R.menu.menu_feed_card_header, menu.getMenu());
        menu.setOnMenuItemClickListener(new OverflowMenuClickListener());
        menu.show();
    }

    private class OverflowMenuClickListener implements PopupMenu.OnMenuItemClickListener {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_feed_card_dismiss:
                    if (getCallback() != null & getCard() != null) {
                        return getCallback().onRequestDismissCard(getCard());
                    }
                    return false;
                case R.id.menu_feed_card_customize:
                    if (getCallback() != null & getCard() != null) {
                        getCallback().onRequestCustomize(getCard());
                    }
                    return true;
                default:
                    return false;
            }
        }
    }
}
