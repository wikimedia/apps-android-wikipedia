package org.wikipedia.feed.view;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.view.View;

import org.wikipedia.feed.FeedViewCallback;
import org.wikipedia.page.PageTitle;

public class PageTitleListCardItemView extends ListCardItemView {
    @Nullable private FeedViewCallback callback;
    @Nullable private PageTitle title;

    public PageTitleListCardItemView(Context context) {
        super(context);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (callback != null && title != null) {
                    callback.onSelectPage(title);
                }
            }
        });
    }

    @NonNull public PageTitleListCardItemView setCallback(@Nullable FeedViewCallback callback) {
        this.callback = callback;
        return this;
    }

    @NonNull public PageTitleListCardItemView setPageTitle(@NonNull PageTitle title) {
        this.title = title;
        titleView.setText(title.getDisplayText());
        subtitleView.setText(title.getDescription());
        imageView.setImageURI(TextUtils.isEmpty(title.getThumbUrl()) ? null : Uri.parse(title.getThumbUrl()));
        return this;
    }
}