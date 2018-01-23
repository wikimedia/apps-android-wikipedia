package org.wikipedia.views;

import android.content.Context;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.view.View;
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
import butterknife.OnLongClick;

import static org.wikipedia.util.ResourceUtil.getThemedColor;

/*
 * TODO: Use this for future RecyclerView updates where we show a list of pages
 * (e.g. History, Search, Disambiguation)
 */
public class PageItemView<T> extends FrameLayout {
    public interface Callback<T> {
        void onClick(@Nullable T item);
        boolean onLongClick(@Nullable T item);
        void onThumbClick(@Nullable T item);
        void onActionClick(@Nullable T item, @NonNull View view);
        void onSecondaryActionClick(@Nullable T item, @NonNull View view);
    }

    @BindView(R.id.page_list_item_container) ViewGroup containerView;
    @BindView(R.id.page_list_item_title) TextView titleView;
    @BindView(R.id.page_list_item_description) TextView descriptionView;
    @BindView(R.id.page_list_item_image) SimpleDraweeView imageView;
    @BindView(R.id.page_list_item_action_primary) ImageView primaryActionView;
    @BindView(R.id.page_list_item_action_secondary) ImageView secondaryActionView;
    @BindView(R.id.page_list_item_selected_image) View imageSelectedView;
    @BindView(R.id.page_list_header_text) GoneIfEmptyTextView headerView;

    @Nullable private Callback<T> callback;
    @Nullable private T item;
    private boolean selected;

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
        primaryActionView.setImageResource(id);
        primaryActionView.setVisibility(VISIBLE);
    }

    public void setActionHint(@StringRes int id) {
        primaryActionView.setContentDescription(getContext().getString(id));
    }

    public void setSecondaryActionIcon(@DrawableRes int id, boolean show) {
        if (show) {
            secondaryActionView.setImageResource(id);
        }
        secondaryActionView.setVisibility(show ? VISIBLE : GONE);
    }

    public void setSecondaryActionHint(@StringRes int id) {
        secondaryActionView.setContentDescription(getContext().getString(id));
    }

    public void setHeaderText(@Nullable CharSequence text) {
        headerView.setText(text);
    }

    public void setSelected(boolean selected) {
        if (this.selected != selected) {
            this.selected = selected;
            updateSelectedState();
        }
    }

    public void addFooter(@NonNull View view) {
        containerView.addView(view);
    }

    @OnClick(R.id.page_list_item_container) void onClick() {
        if (callback != null) {
            callback.onClick(item);
        }
    }

    @OnLongClick(R.id.page_list_item_container) boolean onLongClick() {
        if (callback != null) {
            return callback.onLongClick(item);
        }
        return false;
    }

    @OnClick(R.id.page_list_item_image_container) void onThumbClick() {
        if (callback != null) {
            callback.onThumbClick(item);
        }
    }

    @OnClick(R.id.page_list_item_action_primary) void onActionClick(View v) {
        if (callback != null) {
            callback.onActionClick(item, v);
        }
    }

    @OnClick(R.id.page_list_item_action_secondary) void onSecondaryActionClick() {
        if (callback != null) {
            callback.onSecondaryActionClick(item, this);
        }
    }

    private void init() {
        inflate(getContext(), R.layout.item_page_list_entry, this);
        ButterKnife.bind(this);

        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        FeedbackUtil.setToolbarButtonLongPressToast(primaryActionView);
        FeedbackUtil.setToolbarButtonLongPressToast(secondaryActionView);
    }

    private void updateSelectedState() {
        imageView.setVisibility(selected ? GONE : VISIBLE);
        imageSelectedView.setVisibility(selected ? VISIBLE : GONE);
        // TODO: animate?
        containerView.setBackgroundColor(getThemedColor(getContext(),
                selected ? R.attr.multi_select_background_color : R.attr.paper_color));
    }
}
