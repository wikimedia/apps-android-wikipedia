package org.wikipedia.feed.onthisday;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.wikipedia.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class OnThisDayActionsView extends LinearLayout{
    public interface Callback {
        void onAddPageToList();
        void onSharePage();
    }

    @BindView(R.id.on_this_day_item_title) TextView titleView;

    @Nullable
    private Callback callback;

    public OnThisDayActionsView(Context context) {
        super(context);
        init();
    }

    public OnThisDayActionsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public OnThisDayActionsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public OnThisDayActionsView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void setState(@NonNull String pageTitle) {
        titleView.setText(pageTitle);
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    @OnClick(R.id.on_this_day_item_share) void onShareClick(View view) {
        if (callback != null) {
            callback.onSharePage();
        }
    }

    @OnClick(R.id.on_this_day_item_add_to_list) void onAddPageToListClick(View view) {
        if (callback != null) {
            callback.onAddPageToList();
        }
    }

    private void init() {
        inflate(getContext(), R.layout.view_on_this_day_page_actions, this);
        ButterKnife.bind(this);
        setOrientation(VERTICAL);
    }

}
