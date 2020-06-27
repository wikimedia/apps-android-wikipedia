package org.wikipedia.readinglist;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.databinding.ViewReadingListHeaderBinding;
import org.wikipedia.readinglist.database.ReadingList;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.util.GradientUtil;
import org.wikipedia.views.FaceAndColorDetectImageView;
import org.wikipedia.views.ViewUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReadingListHeaderView extends FrameLayout {
    private View emptyView;
    private View imageContainerView;
    private List<FaceAndColorDetectImageView> imageViews;

    @Nullable private ReadingList readingList;

    public ReadingListHeaderView(Context context) {
        super(context);
        init();
    }

    public ReadingListHeaderView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ReadingListHeaderView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setReadingList(@NonNull ReadingList readingList) {
        this.readingList = readingList;
        if (readingList.pages().isEmpty()) {
            imageContainerView.setVisibility(GONE);
            emptyView.setVisibility(VISIBLE);
        } else {
            imageContainerView.setVisibility(VISIBLE);
            emptyView.setVisibility(GONE);
            updateThumbnails();
        }
    }

    private void init() {
        final ViewReadingListHeaderBinding binding = ViewReadingListHeaderBinding.bind(this);

        emptyView = binding.readingListHeaderEmptyImage;
        imageContainerView = binding.readingListHeaderImageContainer;
        imageViews = Arrays.asList(binding.readingListHeaderImage0, binding.readingListHeaderImage1,
                binding.readingListHeaderImage2, binding.readingListHeaderImage3,
                binding.readingListHeaderImage4, binding.readingListHeaderImage5);

        setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        binding.readingListHeaderImageGradient.setBackground(GradientUtil
                .getPowerGradient(R.color.black54, Gravity.TOP));

        if (isInEditMode()) {
            return;
        }
        clearThumbnails();
    }

    private void clearThumbnails() {
        for (FaceAndColorDetectImageView imageView : imageViews) {
            ViewUtil.loadImage(imageView, null);
        }
    }

    private void updateThumbnails() {
        if (readingList == null) {
            return;
        }
        clearThumbnails();
        List<String> thumbUrls = new ArrayList<>();
        for (ReadingListPage page : readingList.pages()) {
            if (!TextUtils.isEmpty(page.thumbUrl())) {
                thumbUrls.add(page.thumbUrl());
            }
            if (thumbUrls.size() > imageViews.size()) {
                break;
            }
        }
        for (int i = thumbUrls.size(); i < imageViews.size() && i < readingList.pages().size(); i++) {
            thumbUrls.add("");
        }
        for (int i = 0; i < thumbUrls.size() && i < imageViews.size(); ++i) {
            loadThumbnail(imageViews.get(i), thumbUrls.get(i));
        }
    }

    private void loadThumbnail(@NonNull FaceAndColorDetectImageView view, @Nullable String url) {
        ViewUtil.loadImage(view, url);
    }
}
