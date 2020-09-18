package org.wikipedia.feed.onthisday;

import android.app.Activity;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import org.wikipedia.R;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.readinglist.MoveToReadingListDialog;
import org.wikipedia.readinglist.ReadingListBehaviorsUtil;
import org.wikipedia.readinglist.ReadingListBookmarkMenu;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.FaceAndColorDetectImageView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;

import static org.wikipedia.Constants.InvokeSource.NEWS_ACTIVITY;

public class OnThisDayPagesViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.page_list_item_title) TextView pageItemTitleTextView;
    @BindView(R.id.page_list_item_description) TextView pageItemDescTextView;
    @BindView(R.id.page_list_item_image) FaceAndColorDetectImageView pageItemImageView;
    @BindView(R.id.parent) View parent;

    private WikiSite wiki;
    private Activity activity;
    private FragmentManager fragmentManager;
    private PageSummary selectedPage;
    private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();

    OnThisDayPagesViewHolder(@NonNull Activity activity, @NonNull FragmentManager fragmentManager, @NonNull MaterialCardView v, @NonNull WikiSite wiki) {
        super(v);
        ButterKnife.bind(this, v);
        this.wiki = wiki;
        this.activity = activity;
        this.fragmentManager = fragmentManager;
        DeviceUtil.setContextClickAsLongClick(parent);
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

    @OnClick(R.id.parent) void onBaseViewClicked() {
        PageTitle pageTitle = selectedPage.getPageTitle(wiki);
        HistoryEntry entry = new HistoryEntry(pageTitle,
                HistoryEntry.SOURCE_ON_THIS_DAY_ACTIVITY);

        activity.startActivity(PageActivity.newIntentForCurrentTab(activity, entry, pageTitle));
    }

    @OnLongClick(R.id.parent) boolean showOverflowMenu(View anchorView) {
        PageTitle pageTitle = selectedPage.getPageTitle(wiki);
        HistoryEntry entry = new HistoryEntry(pageTitle,
                HistoryEntry.SOURCE_ON_THIS_DAY_ACTIVITY);

        new ReadingListBookmarkMenu(anchorView, true, new ReadingListBookmarkMenu.Callback() {
            @Override
            public void onAddRequest(boolean addToDefault) {
                if (addToDefault) {
                    ReadingListBehaviorsUtil.INSTANCE.addToDefaultList(activity, entry.getTitle(), NEWS_ACTIVITY,
                            readingListId ->
                                    bottomSheetPresenter.show(fragmentManager,
                                            MoveToReadingListDialog.newInstance(readingListId, entry.getTitle(), NEWS_ACTIVITY)));
                } else {
                    bottomSheetPresenter.show(fragmentManager,
                            AddToReadingListDialog.newInstance(entry.getTitle(), NEWS_ACTIVITY));
                }
            }

            @Override
            public void onMoveRequest(@Nullable ReadingListPage page) {
                bottomSheetPresenter.show(fragmentManager,
                        MoveToReadingListDialog.newInstance(page.listId(), entry.getTitle(), NEWS_ACTIVITY));
            }

            @Override
            public void onDeleted(@Nullable ReadingListPage page) {
                FeedbackUtil.showMessage(activity,
                        activity.getString(R.string.reading_list_item_deleted, entry.getTitle().getDisplayText()));
            }

            @Override
            public void onShare() {
                ShareUtil.shareText(activity, entry.getTitle());
            }
        }).show(entry.getTitle());

        return true;
    }
}
