package org.wikipedia.feed.onthisday;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;
import org.wikipedia.page.PageActivity;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.readinglist.LongPressMenu;
import org.wikipedia.readinglist.MoveToReadingListDialog;
import org.wikipedia.readinglist.ReadingListBehaviorsUtil;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.TabUtil;
import org.wikipedia.util.TransitionUtil;
import org.wikipedia.views.FaceAndColorDetectImageView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;

import static org.wikipedia.Constants.InvokeSource.NEWS_ACTIVITY;
import static org.wikipedia.Constants.InvokeSource.ON_THIS_DAY_ACTIVITY;

public class OnThisDayPagesViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.page_list_item_title) TextView pageItemTitleTextView;
    @BindView(R.id.page_list_item_description) TextView pageItemDescTextView;
    @BindView(R.id.page_list_item_image) FaceAndColorDetectImageView pageItemImageView;

    private final WikiSite wiki;
    private final Activity activity;
    private final FragmentManager fragmentManager;
    private PageSummary selectedPage;
    private final ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();

    OnThisDayPagesViewHolder(@NonNull Activity activity, @NonNull FragmentManager fragmentManager, @NonNull View v, @NonNull WikiSite wiki) {
        super(v);
        ButterKnife.bind(this, v);
        this.wiki = wiki;
        this.activity = activity;
        this.fragmentManager = fragmentManager;
        DeviceUtil.setContextClickAsLongClick(v);
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

    @OnClick
    void onBaseViewClicked() {
        HistoryEntry entry = new HistoryEntry(selectedPage.getPageTitle(wiki), HistoryEntry.SOURCE_ON_THIS_DAY_ACTIVITY);
        Pair<View, String>[] sharedElements = TransitionUtil.getSharedElements(activity, pageItemImageView);
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, sharedElements);
        Intent intent = PageActivity.newIntentForNewTab(activity, entry, entry.getTitle());
        if (sharedElements.length > 0) {
            intent.putExtra(Constants.INTENT_EXTRA_HAS_TRANSITION_ANIM, true);
        }
        activity.startActivity(intent, DimenUtil.isLandscape(activity) || sharedElements.length == 0 ? null : options.toBundle());
    }

    @OnLongClick
    boolean showOverflowMenu(View anchorView) {
        HistoryEntry entry = new HistoryEntry(selectedPage.getPageTitle(wiki), HistoryEntry.SOURCE_ON_THIS_DAY_ACTIVITY);

        new LongPressMenu(anchorView, true, new LongPressMenu.Callback() {
            @Override
            public void onOpenLink(@NonNull HistoryEntry entry) {
                PageActivity.newIntentForNewTab(activity, entry, entry.getTitle());
            }

            @Override
            public void onOpenInNewTab(@NonNull HistoryEntry entry) {
                TabUtil.openInNewBackgroundTab(entry);
                FeedbackUtil.showMessage(activity, R.string.article_opened_in_background_tab);
            }

            @Override
            public void onAddRequest(@NonNull HistoryEntry entry, boolean addToDefault) {
                if (addToDefault) {
                    ReadingListBehaviorsUtil.INSTANCE.addToDefaultList(activity, entry.getTitle(), NEWS_ACTIVITY,
                            readingListId ->
                                    bottomSheetPresenter.show(fragmentManager,
                                            MoveToReadingListDialog.newInstance(readingListId, entry.getTitle(), ON_THIS_DAY_ACTIVITY)));
                } else {
                    bottomSheetPresenter.show(fragmentManager,
                            AddToReadingListDialog.newInstance(entry.getTitle(), ON_THIS_DAY_ACTIVITY));
                }
            }

            @Override
            public void onMoveRequest(@Nullable ReadingListPage page, @NonNull HistoryEntry entry) {
                bottomSheetPresenter.show(fragmentManager,
                        MoveToReadingListDialog.newInstance(page.getListId(), entry.getTitle(), ON_THIS_DAY_ACTIVITY));
            }
        }).show(entry);

        return true;
    }
}
