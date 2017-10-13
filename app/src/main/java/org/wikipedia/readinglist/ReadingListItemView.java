package org.wikipedia.readinglist;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.StyleRes;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.view.SimpleDraweeView;

import org.wikipedia.R;
import org.wikipedia.views.ViewUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ReadingListItemView extends FrameLayout {
    public interface Callback {
        void onClick(@NonNull ReadingList readingList);
        void onRename(@NonNull ReadingList readingList);
        void onEditDescription(@NonNull ReadingList readingList);
        void onDelete(@NonNull ReadingList readingList);
        void onSaveAllOffline(@NonNull ReadingList readingList);
        void onRemoveAllOffline(@NonNull ReadingList readingList);
    }

    public enum Description { DETAIL, SUMMARY }

    @BindView(R.id.item_title) TextView titleView;
    @BindView(R.id.item_reading_list_statistical_description) TextView statisticalDescriptionView;
    @BindView(R.id.item_description) TextView descriptionView;
    @BindView(R.id.item_overflow_menu)View overflowButton;

    @BindView(R.id.item_image_container) View imageContainer;
    @BindView(R.id.item_image_1) SimpleDraweeView imageView1;
    @BindView(R.id.item_image_2) SimpleDraweeView imageView2;
    @BindView(R.id.item_image_3) SimpleDraweeView imageView3;
    @BindView(R.id.item_image_4) SimpleDraweeView imageView4;

    @Nullable private Callback callback;
    @Nullable private ReadingList readingList;
    private boolean showDescriptionEmptyHint;

    public ReadingListItemView(Context context) {
        super(context);
        init();
    }

    public ReadingListItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ReadingListItemView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ReadingListItemView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void setReadingList(@NonNull ReadingList readingList, @NonNull Description description) {
        this.readingList = readingList;

        boolean isDetailView = description == Description.DETAIL;
        descriptionView.setMaxLines(isDetailView
                ? Integer.MAX_VALUE
                : getResources().getInteger(R.integer.reading_list_description_summary_view_max_lines));
        CharSequence text = isDetailView
                ? buildStatisticalDetailText(readingList)
                : buildStatisticalSummaryText(readingList);
        statisticalDescriptionView.setText(text);

        updateDetails();
        if (imageContainer.getVisibility() == VISIBLE) {
            updateThumbnails();
        }
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    public void setOverflowButtonVisible(boolean visible) {
        overflowButton.setVisibility(visible ? VISIBLE : GONE);
    }

    public void setThumbnailVisible(boolean visible) {
        imageContainer.setVisibility(visible ? VISIBLE : GONE);
    }

    public void setShowDescriptionEmptyHint(boolean show) {
        showDescriptionEmptyHint = show;
        updateDetails();
    }

    public void setTitleTextAppearance(@StyleRes int id) {
        TextViewCompat.setTextAppearance(titleView, id);
    }

    @OnClick void onClick(View view) {
        if (callback != null && readingList != null) {
            callback.onClick(readingList);
        }
    }

    @OnClick(R.id.item_overflow_menu) void showOverflowMenu(View anchorView) {
        PopupMenu menu = new PopupMenu(getContext(), anchorView);
        menu.getMenuInflater().inflate(R.menu.menu_reading_list_item, menu.getMenu());
        menu.setOnMenuItemClickListener(new OverflowMenuClickListener());
        menu.show();
    }

    private void init() {
        inflate(getContext(), R.layout.item_reading_list, this);
        ButterKnife.bind(this);

        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        setClickable(true);
        clearThumbnails();
    }

    private void updateDetails() {
        if (readingList == null) {
            return;
        }
        titleView.setText(TextUtils.isEmpty(readingList.getTitle())
                ? getString(R.string.reading_list_untitled)
                : readingList.getTitle());
        if (TextUtils.isEmpty(readingList.getDescription()) && showDescriptionEmptyHint) {
            descriptionView.setText(getContext().getString(R.string.reading_list_no_description));
            descriptionView.setTypeface(descriptionView.getTypeface(), Typeface.ITALIC);
        } else {
            descriptionView.setText(readingList.getDescription());
            descriptionView.setTypeface(descriptionView.getTypeface(), Typeface.NORMAL);
        }
    }

    private void clearThumbnails() {
        ViewUtil.loadImageUrlInto(imageView1, null);
        imageView1.getHierarchy().setFailureImage(null);
        ViewUtil.loadImageUrlInto(imageView2, null);
        imageView2.getHierarchy().setFailureImage(null);
        ViewUtil.loadImageUrlInto(imageView3, null);
        imageView3.getHierarchy().setFailureImage(null);
        ViewUtil.loadImageUrlInto(imageView4, null);
        imageView4.getHierarchy().setFailureImage(null);
    }

    private void updateThumbnails() {
        clearThumbnails();
        int thumbIndex = 0;
        if (readingList.getPages().size() > thumbIndex) {
            loadThumbnail(imageView1, readingList.getPages().get(thumbIndex).thumbnailUrl());
        }
        if (readingList.getPages().size() > ++thumbIndex) {
            loadThumbnail(imageView2, readingList.getPages().get(thumbIndex).thumbnailUrl());
        }
        if (readingList.getPages().size() > ++thumbIndex) {
            loadThumbnail(imageView3, readingList.getPages().get(thumbIndex).thumbnailUrl());
        }
        if (readingList.getPages().size() > ++thumbIndex) {
            loadThumbnail(imageView4, readingList.getPages().get(thumbIndex).thumbnailUrl());
        }
    }

    private void loadThumbnail(@NonNull SimpleDraweeView view, @Nullable String url) {
        if (TextUtils.isEmpty(url)) {
            view.getHierarchy().setFailureImage(R.drawable.ic_image_gray_24dp,
                    ScalingUtils.ScaleType.FIT_CENTER);
        } else {
            ViewUtil.loadImageUrlInto(view, url);
        }
    }

    @NonNull private String buildStatisticalSummaryText(@NonNull ReadingList readingList) {
        float listSize = statsTextListSize(readingList);
        return readingList.getPages().size() == 1
                ? getString(R.string.format_reading_list_statistical_summary_singular,
                    listSize)
                : getString(R.string.format_reading_list_statistical_summary_plural,
                    readingList.getPages().size(), listSize);
    }

    @NonNull private String buildStatisticalDetailText(@NonNull ReadingList readingList) {
        float listSize = statsTextListSize(readingList);
        return readingList.getPages().size() == 1
                ? getString(R.string.format_reading_list_statistical_detail_singular,
                    readingList.pagesOffline(), listSize)
                : getString(R.string.format_reading_list_statistical_detail_plural,
                    readingList.pagesOffline(), readingList.getPages().size(), listSize);
    }

    private float statsTextListSize(@NonNull ReadingList readingList) {
        int unitSize = Math.max(1, getResources().getInteger(R.integer.reading_list_item_size_bytes_per_unit));
        return readingList.logicalSize() / (float) unitSize;
    }

    @NonNull private String getString(@StringRes int id, @Nullable Object... formatArgs) {
        return getResources().getString(id, formatArgs);
    }

    private class OverflowMenuClickListener implements PopupMenu.OnMenuItemClickListener {
        @Override public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_reading_list_rename:
                    if (callback != null && readingList != null) {
                        callback.onRename(readingList);
                        return true;
                    }
                    break;
                case R.id.menu_reading_list_edit_description:
                    if (callback != null && readingList != null) {
                        callback.onEditDescription(readingList);
                        return true;
                    }
                    break;
                case R.id.menu_reading_list_delete:
                    if (callback != null && readingList != null) {
                        callback.onDelete(readingList);
                        return true;
                    }
                    break;
                case R.id.menu_reading_list_save_all_offline:
                    if (callback != null && readingList != null) {
                        callback.onSaveAllOffline(readingList);
                        return true;
                    }
                    break;
                case R.id.menu_reading_list_remove_all_offline:
                    if (callback != null && readingList != null) {
                        callback.onRemoveAllOffline(readingList);
                        return true;
                    }
                    break;
                default:
                    break;
            }
            return false;
        }
    }
}
