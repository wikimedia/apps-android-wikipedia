package org.wikipedia.views;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.widget.PopupWindowCompat;
import android.support.v7.widget.CardView;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.DimenUtil;

import java.text.ParseException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ReadingListsOverflowView extends FrameLayout {

    public interface Callback {
        void sortByClick();
        void createNewListClick();
        void refreshClick();
    }

    @BindView(R.id.reading_lists_overflow_last_sync) TextView lastSync;
    @Nullable private Callback callback;
    @Nullable private PopupWindow popupWindowHost;

    public ReadingListsOverflowView(Context context) {
        super(context);
        init();
    }

    public void show(@NonNull View anchorView, @Nullable Callback callback) {
        this.callback = callback;
        popupWindowHost = new PopupWindow(this, ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, true);
        popupWindowHost.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        PopupWindowCompat.setOverlapAnchor(popupWindowHost, true);
        final int compatOffset = 8;
        PopupWindowCompat.showAsDropDown(popupWindowHost, anchorView, 0, Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
                ? -DimenUtil.getToolbarHeightPx(anchorView.getContext()) + DimenUtil.roundedDpToPx(compatOffset) : 0, Gravity.END);

        String lastSyncTime = Prefs.getReadingListsLastSyncTime();
        lastSync.setVisibility(TextUtils.isEmpty(lastSyncTime) ? View.GONE : View.VISIBLE);
        if (!TextUtils.isEmpty(lastSyncTime)) {
            try {
                lastSync.setText(String.format(getContext().getString(R.string.reading_list_menu_last_sync),
                        DateUtil.getReadingListsLastSyncDateString(Prefs.getReadingListsLastSyncTime())));
            } catch (ParseException e) {
                // ignore
            }
        }
    }

    @OnClick({R.id.reading_lists_overflow_sort_by, R.id.reading_lists_overflow_create_new_list,
            R.id.reading_lists_overflow_refresh})
    void onItemClick(View view) {
        if (popupWindowHost != null) {
            popupWindowHost.dismiss();
            popupWindowHost = null;
        }
        if (callback == null) {
            return;
        }
        switch (view.getId()) {
            case R.id.reading_lists_overflow_sort_by:
                callback.sortByClick();
                break;
            case R.id.reading_lists_overflow_create_new_list:
                callback.createNewListClick();
                break;
            case R.id.reading_lists_overflow_refresh:
                callback.refreshClick();
                break;
            default:
                break;
        }
    }

    private void init() {
        inflate(getContext(), R.layout.view_reading_lists_overflow, this);
        ButterKnife.bind(this);

        CardView cardContainer = findViewById(R.id.reading_lists_overflow_card_container);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            cardContainer.setPreventCornerOverlap(false);
        }
    }
}
