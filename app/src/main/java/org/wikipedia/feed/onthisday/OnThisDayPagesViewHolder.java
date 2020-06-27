package org.wikipedia.feed.onthisday;

import android.app.Activity;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import org.wikipedia.R;
import org.wikipedia.databinding.ItemOnThisDayPagesBinding;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.FaceAndColorDetectImageView;

public class OnThisDayPagesViewHolder extends RecyclerView.ViewHolder {
    public interface ItemCallBack {
        void onActionLongClick(@NonNull HistoryEntry entry);
    }

    private TextView pageItemTitleTextView;
    private TextView pageItemDescTextView;
    private FaceAndColorDetectImageView pageItemImageView;

    @Nullable private ItemCallBack itemCallback;

    private PageSummary selectedPage;

    OnThisDayPagesViewHolder(@NonNull Activity activity, @NonNull ItemOnThisDayPagesBinding binding,
                             @NonNull WikiSite wiki, boolean isSingleCard) {
        super(binding.getRoot());
        pageItemTitleTextView = binding.pageListItemTitle;
        pageItemDescTextView = binding.pageListItemDescription;
        pageItemImageView = binding.pageListItemImage;

        binding.getRoot().setCardBackgroundColor(ResourceUtil.getThemedColor(binding.getRoot().getContext(),
                R.attr.paper_color));
        binding.parent.setOnClickListener(v -> {
            PageTitle pageTitle = new PageTitle(selectedPage.getApiTitle(), wiki);
            HistoryEntry entry = new HistoryEntry(pageTitle,
                    isSingleCard ? HistoryEntry.SOURCE_ON_THIS_DAY_CARD : HistoryEntry.SOURCE_ON_THIS_DAY_ACTIVITY);

            activity.startActivity(PageActivity.newIntentForCurrentTab(activity, entry, pageTitle));
        });
        binding.parent.setOnLongClickListener(v -> {
            PageTitle pageTitle = new PageTitle(selectedPage.getApiTitle(), wiki);
            HistoryEntry entry = new HistoryEntry(pageTitle,
                    isSingleCard ? HistoryEntry.SOURCE_ON_THIS_DAY_CARD : HistoryEntry.SOURCE_ON_THIS_DAY_ACTIVITY);

            itemCallback.onActionLongClick(entry);

            return true;
        });

    }

    public void setFields(@NonNull PageSummary page) {
        selectedPage = page;
        pageItemDescTextView.setText(page.getDescription());
        pageItemDescTextView.setVisibility(TextUtils.isEmpty(page.getDescription()) ? View.GONE : View.VISIBLE);
        pageItemTitleTextView.setMaxLines(TextUtils.isEmpty(page.getDescription()) ? 2 : 1);
        pageItemTitleTextView.setText(StringUtil.fromHtml(page.getDisplayTitle()));
        setImage(page.getThumbnailUrl());
    }

    private void setImage(@Nullable String url) {
        if (url == null) {
            pageItemImageView.setVisibility(View.GONE);
        } else {
            pageItemImageView.setVisibility(View.VISIBLE);
            pageItemImageView.loadImage(Uri.parse(url));
        }
    }

    @NonNull public OnThisDayPagesViewHolder setCallback(@Nullable ItemCallBack itemCallback) {
        this.itemCallback = itemCallback;
        return this;
    }
}
