package org.wikipedia.readinglist;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.DimenRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

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
    }

    @BindView(R.id.item_title) TextView titleView;
    @BindView(R.id.item_count) TextView countView;
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

    public void setReadingList(@NonNull ReadingList readingList) {
        this.readingList = readingList;

        countView.setText(readingList.getPages().size() == 1
                ? getResources().getString(R.string.reading_list_item_count_singular)
                : String.format(getResources().getString(R.string.reading_list_item_count_plural), readingList.getPages().size()));

        updateDetails();
        if (imageContainer.getVisibility() == VISIBLE) {
            getThumbnails();
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

    public void setTitleTextSize(@DimenRes int id) {
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                getContext().getResources().getDimension(id));
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

    private void getThumbnails() {
        ReadingListImageFetcher.getThumbnails(readingList, new ReadingListImageFetcher.CompleteListener() {
            @Override
            public void onComplete() {
                if (getWindowToken() == null) {
                    return;
                }
                updateThumbnails();
            }

            @Override
            public void onError(Throwable e) {
            }
        });
        updateThumbnails();
    }

    private void updateDetails() {
        if (readingList == null) {
            return;
        }
        titleView.setText(TextUtils.isEmpty(readingList.getTitle())
                ? getResources().getString(R.string.reading_list_untitled)
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
            view.getHierarchy().setFailureImage(R.drawable.ic_image_gray_24dp);
        } else {
            ViewUtil.loadImageUrlInto(view, url);
        }
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
                default:
                    break;
            }
            return false;
        }
    }
}
