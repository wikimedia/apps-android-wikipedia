package org.wikipedia.feed.onthisday;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.model.Thumbnail;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.FaceAndColorDetectImageView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static org.wikipedia.page.PageActivity.ACTION_LOAD_IN_NEW_TAB;
import static org.wikipedia.page.PageActivity.EXTRA_HISTORYENTRY;
import static org.wikipedia.page.PageActivity.EXTRA_PAGETITLE;

public class OnThisDayPagesViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.page_list_item_title) TextView pageItemTitleTextView;
    @BindView(R.id.page_list_item_description) TextView pageItemDescTextView;
    @BindView(R.id.page_list_item_image) FaceAndColorDetectImageView pageItemImageView;
    @BindView(R.id.parent) View parent;
    private WikiSite wiki;
    private OnThisDay.Page selectedPage;

    OnThisDayPagesViewHolder(@NonNull CardView v, @NonNull WikiSite wiki) {
        super(v);
        ButterKnife.bind(this, v);
        v.setCardBackgroundColor(ResourceUtil.getThemedColor(v.getContext(), R.attr.paper_color));
        this.wiki = wiki;
    }

    public void setFields(@NonNull final OnThisDay.Page page) {
        selectedPage = page;
        pageItemDescTextView.setText(StringUtil.fromHtml(StringUtils.defaultString(page.text())));
        pageItemTitleTextView.setText(StringUtil.fromHtml(StringUtils.defaultString(page.displayTitle())));
        setImage(page.thumbnail());
    }

    private void setImage(@Nullable Thumbnail thumbnail) {
        if (thumbnail != null) {
            pageItemImageView.setVisibility(View.VISIBLE);
            pageItemImageView.loadImage(thumbnail.source());
        } else {
            pageItemImageView.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.parent) void onBaseViewClicked() {
        Context context = WikipediaApp.getInstance().getApplicationContext();
        PageTitle pageTitle = new PageTitle(selectedPage.displayTitle(), wiki);
        HistoryEntry entry = new HistoryEntry(pageTitle, HistoryEntry.SOURCE_ON_THIS_DAY_LIST);
        Intent intent = new Intent(ACTION_LOAD_IN_NEW_TAB)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setClass(context, PageActivity.class)
                .putExtra(EXTRA_HISTORYENTRY, entry)
                .putExtra(EXTRA_PAGETITLE, pageTitle);
        context.startActivity(intent);
    }
}
