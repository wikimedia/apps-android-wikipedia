package org.wikipedia.feed.onthisday;

import android.app.Activity;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import org.wikipedia.R;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.FaceAndColorDetectImageView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;

public class OnThisDayPagesViewHolder extends RecyclerView.ViewHolder {
    public interface ItemCallBack {
        void onActionLongClick(@NonNull HistoryEntry entry);
    }

    @BindView(R.id.page_list_item_title) TextView pageItemTitleTextView;
    @BindView(R.id.page_list_item_description) TextView pageItemDescTextView;
    @BindView(R.id.page_list_item_image) FaceAndColorDetectImageView pageItemImageView;
    @BindView(R.id.parent) View parent;

    @Nullable private ItemCallBack itemCallback;

    private WikiSite wiki;
    private Activity activity;
    private PageSummary selectedPage;
    private final boolean isSingleCard;

    OnThisDayPagesViewHolder(@NonNull Activity activity, @NonNull MaterialCardView v, @NonNull WikiSite wiki, boolean isSingleCard) {
        super(v);
        ButterKnife.bind(this, v);
        this.wiki = wiki;
        this.isSingleCard = isSingleCard;
        this.activity = activity;
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

    @OnClick(R.id.parent) void onBaseViewClicked() {
        PageTitle pageTitle = selectedPage.getPageTitle(wiki);
        HistoryEntry entry = new HistoryEntry(pageTitle,
                isSingleCard ? HistoryEntry.SOURCE_ON_THIS_DAY_CARD : HistoryEntry.SOURCE_ON_THIS_DAY_ACTIVITY);

        activity.startActivity(PageActivity.newIntentForCurrentTab(activity, entry, pageTitle));
    }

    @OnLongClick(R.id.parent) boolean showOverflowMenu(View anchorView) {
        PageTitle pageTitle = selectedPage.getPageTitle(wiki);
        HistoryEntry entry = new HistoryEntry(pageTitle,
                isSingleCard ? HistoryEntry.SOURCE_ON_THIS_DAY_CARD : HistoryEntry.SOURCE_ON_THIS_DAY_ACTIVITY);

        itemCallback.onActionLongClick(entry);

        return true;
    }
}
