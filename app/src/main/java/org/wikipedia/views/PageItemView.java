package org.wikipedia.views;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.wikipedia.R;
import org.wikipedia.util.FeedbackUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/*
 * TODO: Use this for future RecyclerView updates where we show a list of pages
 * (e.g. History, Search, Disambiguation)
 */
public class PageItemView<T> extends FrameLayout {
    public interface Callback<T> {
        void onClick(@Nullable T item);
        void onActionClick(@Nullable T item);
    }

    @BindView(R.id.page_list_item_title) TextView titleView;
    @BindView(R.id.page_list_item_description) TextView descriptionView;
    @BindView(R.id.page_list_item_image) SimpleDraweeView imageView;
    @BindView(R.id.page_list_item_action_button) ImageView actionView;

    @Nullable private Callback<T> callback;
    @Nullable private T item;

    public PageItemView(@NonNull Context context) {
        super(context);
        init();
    }

    public void setItem(@Nullable T item) {
        this.item = item;
    }

    public void setCallback(@Nullable Callback<T> callback) {
        this.callback = callback;
    }

    public void setTitle(@Nullable CharSequence text) {
        titleView.setText(text);
    }

    public void setDescription(@Nullable CharSequence text) {
        descriptionView.setText(text);
    }

    public void setImageUrl(@Nullable String url) {
        ViewUtil.loadImageUrlInto(imageView, url);
    }

    public void setActionIcon(@DrawableRes int id) {
        actionView.setImageResource(id);
        actionView.setVisibility(VISIBLE);
    }

    public void setActionHint(@StringRes int id) {
        actionView.setContentDescription(getContext().getString(id));
    }

    @OnClick(R.id.page_list_item_container) void onClick() {
        if (callback != null) {
            callback.onClick(item);
        }
    }

    @OnClick(R.id.page_list_item_action_button) void onActionClick() {
        if (callback != null) {
            callback.onActionClick(item);
        }
    }

    private void init() {
        inflate(getContext(), R.layout.item_page_list_entry, this);
        ButterKnife.bind(this);

        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        FeedbackUtil.setToolbarButtonLongPressToast(actionView);
    }
}
