package org.wikipedia.feed.view;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;

import org.wikipedia.R;
import org.wikipedia.databinding.ViewStaticCardBinding;
import org.wikipedia.feed.model.Card;

public abstract class StaticCardView<T extends Card> extends DefaultFeedCardView<T> {
    private View containerView;
    private TextView title;
    private TextView subtitle;
    private ImageView icon;
    private View progress;
    private ImageView actionIcon;
    private TextView actionText;

    public StaticCardView(Context context) {
        super(context);

        final ViewStaticCardBinding binding = ViewStaticCardBinding.inflate(LayoutInflater.from(context));

        containerView = binding.viewStaticCardContainer;
        title = binding.viewStaticCardTitle;
        subtitle = binding.viewStaticCardSubtitle;
        icon = binding.viewStaticCardIcon;
        progress = binding.viewStaticCardProgress;
        actionIcon = binding.viewStaticCardActionIcon;
        actionText = binding.viewStaticCardActionText;

        final View.OnClickListener blankListener = v -> {};
        containerView.setOnClickListener(blankListener);
        binding.viewStaticCardActionContainer.setOnClickListener(blankListener);
        binding.viewStaticCardActionOverflow.setOnClickListener(this::showOverflowMenu);

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

    private void showOverflowMenu(View anchorView) {
        PopupMenu menu = new PopupMenu(anchorView.getContext(), anchorView, Gravity.END);
        menu.getMenuInflater().inflate(R.menu.menu_feed_card_header, menu.getMenu());
        MenuItem editCardLangItem = menu.getMenu().findItem(R.id.menu_feed_card_edit_card_languages);
        editCardLangItem.setVisible(getCard().type().contentType().isPerLanguage());
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

                case R.id.menu_feed_card_edit_card_languages:
                    if (getCallback() != null & getCard() != null) {
                        getCallback().onRequestEditCardLanguages(getCard());
                    }
                    return true;

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
