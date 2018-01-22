package org.wikipedia.readinglist;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.SwitchCompat;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wikipedia.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ReadingListItemActionsView extends LinearLayout {
    public interface Callback {
        void onToggleOffline();
        void onShare();
        void onAddToOther();
        void onDelete();
    }

    @BindView(R.id.reading_list_item_title) TextView titleView;
    @BindView(R.id.reading_list_item_remove_text) TextView removeTextView;
    @BindView(R.id.reading_list_item_offline_switch) SwitchCompat offlineSwitchView;

    @Nullable private Callback callback;

    public ReadingListItemActionsView(Context context) {
        super(context);
        init();
    }

    public ReadingListItemActionsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ReadingListItemActionsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ReadingListItemActionsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void setState(@NonNull String pageTitle, @NonNull String listTitle, boolean offline) {
        offlineSwitchView.setChecked(offline);
        titleView.setText(pageTitle);
        removeTextView.setText(getResources().getString(R.string.reading_list_remove_from_list, listTitle));
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    @OnClick(R.id.reading_list_item_offline) void onOfflineClick(View view) {
        if (callback != null) {
            callback.onToggleOffline();
        }
    }

    @OnClick(R.id.reading_list_item_share) void onShareClick(View view) {
        if (callback != null) {
            callback.onShare();
        }
    }

    @OnClick(R.id.reading_list_item_add_to_other) void onAddToOtherClick(View view) {
        if (callback != null) {
            callback.onAddToOther();
        }
    }

    @OnClick(R.id.reading_list_item_remove) void onRemoveClick(View view) {
        if (callback != null) {
            callback.onDelete();
        }
    }

    private void init() {
        inflate(getContext(), R.layout.view_reading_list_page_actions, this);
        ButterKnife.bind(this);
        setOrientation(VERTICAL);
    }
}
