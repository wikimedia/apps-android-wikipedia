package org.wikipedia.views;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.TextViewCompat;

import com.facebook.drawee.view.SimpleDraweeView;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import org.wikipedia.R;
import org.wikipedia.readinglist.database.ReadingList;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.StringUtil;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;

import static org.wikipedia.util.ResourceUtil.getThemedColor;

/*
 * TODO: Use this for future RecyclerView updates where we show a list of pages
 * (e.g. History, Search, Disambiguation)
 */
public class PageItemView<T> extends ConstraintLayout {

    public interface Callback<T> {
        void onClick(@Nullable T item);
        boolean onLongClick(@Nullable T item);
        void onThumbClick(@Nullable T item);
        void onActionClick(@Nullable T item, @NonNull View view);
        void onSecondaryActionClick(@Nullable T item, @NonNull View view);
        void onListChipClick(@NonNull ReadingList readingList);
    }

    @BindView(R.id.page_list_item_title) TextView titleView;
    @BindView(R.id.page_list_item_description) TextView descriptionView;
    @BindView(R.id.page_list_item_image) SimpleDraweeView imageView;
    @BindView(R.id.page_list_item_action_secondary) ImageView secondaryActionView;
    @BindView(R.id.page_list_item_secondary_container) View secondaryContainer;
    @BindView(R.id.page_list_item_selected_image) View imageSelectedView;
    @BindView(R.id.page_list_header_text) GoneIfEmptyTextView headerView;
    @BindView(R.id.page_list_item_circular_progress_bar) CircularProgressBar circularProgressBar;
    @BindView(R.id.chips_scrollview) View chipsScrollView;
    @BindView(R.id.reading_lists_chip_group) ChipGroup readingListsChipGroup;

    @Nullable private Callback<T> callback;
    @Nullable private T item;
    @Nullable private String imageUrl;
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

    public void setTitleMaxLines(int linesCount) {
        titleView.setMaxLines(linesCount);
    }

    public void setTitleEllipsis() {
        titleView.setEllipsize(TextUtils.TruncateAt.END);
    }

    public void setDescription(@Nullable CharSequence text) {
        descriptionView.setText(text);
    }

    public void setDescriptionMaxLines(int linesCount) {
        descriptionView.setMaxLines(linesCount);
    }

    public void setDescriptionEllipsis() {
        descriptionView.setEllipsize(TextUtils.TruncateAt.END);
    }

    public void setImageUrl(@Nullable String url) {
        imageUrl = url;
        updateSelectedState();
    }

    public void setSecondaryActionIcon(@DrawableRes int id, boolean show) {
        secondaryActionView.setImageResource(id);
        secondaryActionView.setVisibility(show ? VISIBLE : GONE);
        secondaryContainer.setVisibility(show ? VISIBLE : GONE);
    }

    public void setProgress(int progress) {
        circularProgressBar.setCurrentProgress(progress);
    }

    public void setCircularProgressVisibility(boolean visible) {
        circularProgressBar.setVisibility(visible ? VISIBLE : GONE);
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

    public void setListItemImageDimensions(int width, int height) {
        imageView.getLayoutParams().width = width;
        imageView.getLayoutParams().height = height;
        requestLayout();
    }

    public void setUpChipGroup(List<ReadingList> readingLists) {
        chipsScrollView.setVisibility(VISIBLE);
        chipsScrollView.setFadingEdgeLength(0);
        readingListsChipGroup.removeAllViews();
        for (ReadingList readingList : readingLists) {
            Chip chip = new Chip(readingListsChipGroup.getContext());
            TextViewCompat.setTextAppearance(chip, R.style.CustomChipStyle);
            chip.setText(readingList.title());
            chip.setClickable(true);
            chip.setChipBackgroundColorResource(ResourceUtil.getThemedAttributeId(getContext(), R.attr.chip_background_color));
            chip.setOnClickListener(v -> {
                if (callback != null) {
                    callback.onListChipClick(readingList);
                }
            });
            readingListsChipGroup.addView(chip);
        }
    }

    public void hideChipGroup() {
        chipsScrollView.setVisibility(GONE);
    }

    public void setSearchQuery(@Nullable String searchQuery) {
        // highlight search term within the text
        StringUtil.boldenKeywordText(titleView, titleView.getText().toString(), searchQuery);
    }

    @OnClick void onClick() {
        if (callback != null) {
            callback.onClick(item);
        }
    }

    @OnLongClick boolean onLongClick() {
        if (callback != null) {
            return callback.onLongClick(item);
        }
        return false;
    }

    @OnClick(R.id.page_list_item_image) void onThumbClick() {
        if (callback != null) {
            callback.onThumbClick(item);
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

        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        final int topBottomPadding = 16;
        setPadding(0, DimenUtil.roundedDpToPx(topBottomPadding), 0, DimenUtil.roundedDpToPx(topBottomPadding));
        setBackgroundColor(ResourceUtil.getThemedColor(getContext(), R.attr.paper_color));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            setForeground(AppCompatResources.getDrawable(getContext(), ResourceUtil.getThemedAttributeId(getContext(), R.attr.selectableItemBackground)));
        }

        FeedbackUtil.setToolbarButtonLongPressToast(secondaryActionView);
    }

    private void updateSelectedState() {
        if (selected) {
            imageSelectedView.setVisibility(VISIBLE);
            imageView.setVisibility(GONE);
            setBackgroundColor(getThemedColor(getContext(), R.attr.multi_select_background_color));
        } else {
            imageView.setVisibility(TextUtils.isEmpty(imageUrl) ? GONE : VISIBLE);
            ViewUtil.loadImageUrlInto(imageView, imageUrl);
            imageSelectedView.setVisibility(GONE);
            setBackgroundColor(getThemedColor(getContext(), R.attr.paper_color));
        }
    }

    @SuppressWarnings("checkstyle:magicnumber")
    public void setViewsGreyedOut(boolean greyedOut) {
        // Cannot use isAttachedToWindow() because the first two item will be reset when the setHistoryEntry() getting called even they are not visible.
        if (titleView == null || descriptionView == null || imageView == null) {
            return;
        }
        final float alpha = greyedOut ? 0.5f : 1.0f;
        titleView.setAlpha(alpha);
        descriptionView.setAlpha(alpha);
        imageView.setAlpha(alpha);
    }
}
