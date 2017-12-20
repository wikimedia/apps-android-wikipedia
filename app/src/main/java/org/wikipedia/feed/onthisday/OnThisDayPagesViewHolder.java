package org.wikipedia.feed.onthisday;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.restbase.page.RbPageSummary;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.FaceAndColorDetectImageView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;

import static org.wikipedia.page.PageActivity.ACTION_LOAD_IN_NEW_TAB;
import static org.wikipedia.page.PageActivity.EXTRA_HISTORYENTRY;
import static org.wikipedia.page.PageActivity.EXTRA_PAGETITLE;

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
    private RbPageSummary selectedPage;
    private final boolean isSingleCard;

    OnThisDayPagesViewHolder(@NonNull CardView v, @NonNull WikiSite wiki, boolean isSingleCard) {
        super(v);
        ButterKnife.bind(this, v);
        v.setCardBackgroundColor(ResourceUtil.getThemedColor(v.getContext(), R.attr.paper_color));
        this.wiki = wiki;
        this.isSingleCard = isSingleCard;
    }

    public void setFields(@NonNull RbPageSummary page) {
        selectedPage = page;
        pageItemDescTextView.setText(StringUtils.capitalize(page.getDescription()));
        pageItemDescTextView.setVisibility(TextUtils.isEmpty(page.getDescription()) ? View.GONE : View.VISIBLE);
        pageItemTitleTextView.setMaxLines(TextUtils.isEmpty(page.getDescription()) ? 2 : 1);
        pageItemTitleTextView.setText(StringUtil.fromHtml(StringUtils.defaultString(page.getNormalizedTitle())));
        setImage(page.getThumbnailUrl());
    }

    private void setImage(@Nullable String url) {
        pageItemImageView.loadImage(url == null ? null : Uri.parse(url));
    }

    @NonNull public OnThisDayPagesViewHolder setCallback(@Nullable ItemCallBack itemCallback) {
        this.itemCallback = itemCallback;
        return this;
    }

    @OnClick(R.id.parent) void onBaseViewClicked() {
        Context context = WikipediaApp.getInstance().getApplicationContext();
        PageTitle pageTitle = new PageTitle(selectedPage.getTitle(), wiki);
        HistoryEntry entry = new HistoryEntry(pageTitle,
                isSingleCard ? HistoryEntry.SOURCE_ON_THIS_DAY_CARD : HistoryEntry.SOURCE_ON_THIS_DAY_ACTIVITY);
        Intent intent = new Intent(ACTION_LOAD_IN_NEW_TAB)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setClass(context, PageActivity.class)
                .putExtra(EXTRA_HISTORYENTRY, entry)
                .putExtra(EXTRA_PAGETITLE, pageTitle);
        context.startActivity(intent);
    }

    @OnLongClick(R.id.parent) boolean showOverflowMenu(View anchorView) {
        PageTitle pageTitle = new PageTitle(selectedPage.getTitle(), wiki);
        HistoryEntry entry = new HistoryEntry(pageTitle,
                isSingleCard ? HistoryEntry.SOURCE_ON_THIS_DAY_CARD : HistoryEntry.SOURCE_ON_THIS_DAY_ACTIVITY);

        itemCallback.onActionLongClick(entry);

        return true;
    }
}
