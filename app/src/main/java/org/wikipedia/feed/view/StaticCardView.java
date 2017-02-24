package org.wikipedia.feed.view;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.feed.model.Card;

import butterknife.BindView;
import butterknife.ButterKnife;

public abstract class StaticCardView<T extends Card> extends DefaultFeedCardView<T> {
    @BindView(R.id.view_static_card_title) TextView title;
    @BindView(R.id.view_static_card_subtitle) TextView subtitle;
    @BindView(R.id.view_static_card_icon) ImageView icon;
    @BindView(R.id.view_static_card_progress) View progress;

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

    protected void setIcon(@DrawableRes int resId) {
        icon.setImageResource(resId);
    }

    protected void setProgress(boolean enabled) {
        icon.setVisibility(enabled ? GONE : VISIBLE);
        progress.setVisibility(enabled ? VISIBLE : GONE);
    }

    protected String getString(@StringRes int id) {
        return getResources().getString(id);
    }
}
