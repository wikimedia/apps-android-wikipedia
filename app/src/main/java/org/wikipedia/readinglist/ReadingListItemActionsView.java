package org.wikipedia.readinglist;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;

import org.wikipedia.databinding.ViewReadingListPageActionsBinding;
import org.wikipedia.util.StringUtil;

public class ReadingListItemActionsView extends LinearLayout {
    public interface Callback {
        void onToggleOffline();
        void onShare();
        void onAddToOther();
        void onMoveToOther();
        void onSelect();
        void onDelete();
    }

    private TextView titleView;
    private TextView removeTextView;
    private SwitchCompat offlineSwitchView;
    private ViewGroup moveItemContainer;
    private ViewGroup selectItemContainer;

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

    public void setState(@NonNull String pageTitle, @NonNull String removeFromListText, boolean offline, boolean hasActionMode) {
        offlineSwitchView.setChecked(offline);
        titleView.setText(StringUtil.fromHtml(pageTitle));
        removeTextView.setText(removeFromListText);
        selectItemContainer.setVisibility(hasActionMode ? View.GONE : View.VISIBLE);
        moveItemContainer.setVisibility(hasActionMode ? View.GONE : View.VISIBLE);
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    private void init() {
        final ViewReadingListPageActionsBinding binding = ViewReadingListPageActionsBinding.bind(this);

        titleView = binding.readingListItemTitle;
        removeTextView = binding.readingListItemRemoveText;
        offlineSwitchView = binding.readingListItemOfflineSwitch;
        moveItemContainer = binding.readingListItemMoveToOther;
        selectItemContainer = binding.readingListItemSelect;

        binding.readingListItemOffline.setOnClickListener(v -> {
            if (callback != null) {
                callback.onToggleOffline();
            }
        });
        binding.readingListItemShare.setOnClickListener(v -> {
            if (callback != null) {
                callback.onShare();
            }
        });
        binding.readingListItemAddToOther.setOnClickListener(v -> {
            if (callback != null) {
                callback.onAddToOther();
            }
        });
        moveItemContainer.setOnClickListener(v -> {
            if (callback != null) {
                callback.onMoveToOther();
            }
        });
        selectItemContainer.setOnClickListener(v -> {
            if (callback != null) {
                callback.onSelect();
            }
        });
        binding.readingListItemRemove.setOnClickListener(v -> {
            if (callback != null) {
                callback.onDelete();
            }
        });

        setOrientation(VERTICAL);
    }
}
