package org.wikipedia.feed.view;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v7.widget.CardView;
import android.widget.ImageView;
import android.widget.TextView;

import org.wikipedia.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public abstract class StaticCardView extends CardView {
    @BindView(R.id.view_static_card_title) TextView title;
    @BindView(R.id.view_static_card_subtitle) TextView subtitle;
    @BindView(R.id.view_static_card_icon) ImageView icon;

    public StaticCardView(Context context) {
        super(context);

        inflate(getContext(), R.layout.view_static_card, this);
        ButterKnife.bind(this);
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

    protected String getString(@StringRes int id) {
        return getResources().getString(id);
    }
}
