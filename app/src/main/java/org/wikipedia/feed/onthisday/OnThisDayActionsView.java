package org.wikipedia.feed.onthisday;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.databinding.ViewOnThisDayPageActionsBinding;

public class OnThisDayActionsView extends LinearLayout{
    public interface Callback {
        void onAddPageToList();
        void onSharePage();
    }

    private TextView titleView;

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

    public void setState(@NonNull String pageTitle) {
        titleView.setText(pageTitle);
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    private void init() {
        final ViewOnThisDayPageActionsBinding binding = ViewOnThisDayPageActionsBinding.bind(this);

        titleView = binding.onThisDayItemTitle;
        binding.onThisDayItemShare.setOnClickListener(v -> {
            if (callback != null) {
                callback.onSharePage();
            }
        });
        binding.onThisDayItemAddToList.setOnClickListener(v -> {
            if (callback != null) {
                callback.onAddPageToList();
            }
        });

        setOrientation(VERTICAL);
    }
}
