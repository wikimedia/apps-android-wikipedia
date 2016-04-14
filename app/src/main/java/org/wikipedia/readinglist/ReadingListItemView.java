package org.wikipedia.readinglist;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.wikipedia.R;
import org.wikipedia.page.PageTitle;
import org.wikipedia.views.ViewUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ReadingListItemView extends LinearLayout {
    @Bind(R.id.item_container) LinearLayout containerView;
    @Bind(R.id.item_title) TextView titleView;
    @Bind(R.id.item_count) TextView countView;
    @Bind(R.id.item_description) TextView descriptionView;

    @Bind(R.id.item_image_row_1) View imageViewRow1;
    @Bind(R.id.item_image_row_2) View imageViewRow2;
    @Bind(R.id.item_image_1) SimpleDraweeView imageView1;
    @Bind(R.id.item_image_2) SimpleDraweeView imageView2;
    @Bind(R.id.item_image_3) SimpleDraweeView imageView3;
    @Bind(R.id.item_image_4) SimpleDraweeView imageView4;

    @Nullable private ReadingList readingList;
    @Nullable private OnClickListener clickListener;

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
        containerView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clickListener != null) {
                    clickListener.onClick(ReadingListItemView.this);
                }
            }
        });
        countView.setText(readingList.getPages().size() == 1
                ? getResources().getString(R.string.reading_list_item_count_singular)
                : String.format(getResources().getString(R.string.reading_list_item_count_plural), readingList.getPages().size()));

        updateDetails();
        getThumbnails();
    }

    @Override
    public void setOnClickListener(OnClickListener listener) {
        clickListener = listener;
    }

    private void init() {
        inflate(getContext(), R.layout.item_reading_list, this);
        ButterKnife.bind(this);

        setLayoutParams(new LinearLayoutCompat.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        clearThumbnails();
    }

    private void getThumbnails() {
        ReadingListImageFetcher.getThumbnails(readingList, new ReadingListImageFetcher.CompleteListener() {
            @Override
            public void onComplete() {
                updateThumbnails();
            }

            @Override
            public void onError(Throwable e) {
            }
        });
    }

    private void updateDetails() {
        if (readingList == null) {
            return;
        }
        titleView.setText(TextUtils.isEmpty(readingList.getTitle())
                ? getResources().getString(R.string.reading_list_untitled)
                : readingList.getTitle());
        descriptionView.setText(readingList.getDescription());
    }

    private void clearThumbnails() {
        ViewUtil.loadImageUrlInto(imageView1, null);
        imageView2.setVisibility(GONE);
        imageView3.setVisibility(GONE);
        imageView4.setVisibility(GONE);
        imageViewRow2.setVisibility(GONE);
    }

    private void updateThumbnails() {
        clearThumbnails();
        List<String> thumbUrls = new ArrayList<>();
        for (PageTitle title : readingList.getPages()) {
            if (!TextUtils.isEmpty(title.getThumbUrl())) {
                thumbUrls.add(title.getThumbUrl());
            }
        }
        int thumbIndex = 0;
        if (thumbUrls.size() > thumbIndex) {
            ViewUtil.loadImageUrlInto(imageView1, thumbUrls.get(thumbIndex));
        }
        if (thumbUrls.size() > ++thumbIndex) {
            imageView2.setVisibility(VISIBLE);
            ViewUtil.loadImageUrlInto(imageView2, thumbUrls.get(thumbIndex));
        }
        if (thumbUrls.size() > ++thumbIndex) {
            imageViewRow2.setVisibility(VISIBLE);
            imageView3.setVisibility(VISIBLE);
            ViewUtil.loadImageUrlInto(imageView3, thumbUrls.get(thumbIndex));
        }
        if (thumbUrls.size() > ++thumbIndex) {
            imageView4.setVisibility(VISIBLE);
            ViewUtil.loadImageUrlInto(imageView4, thumbUrls.get(thumbIndex));
        }
    }
}
