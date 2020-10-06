package org.wikipedia.feed.onthisday;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.FeedFunnel;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.feed.model.CardType;
import org.wikipedia.feed.view.CardFooterView;
import org.wikipedia.feed.view.CardHeaderView;
import org.wikipedia.feed.view.DefaultFeedCardView;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.readinglist.MoveToReadingListDialog;
import org.wikipedia.readinglist.ReadingListBehaviorsUtil;
import org.wikipedia.readinglist.ReadingListBookmarkMenu;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.util.DateUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.views.FaceAndColorDetectImageView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static org.wikipedia.Constants.InvokeSource.ON_THIS_DAY_CARD_BODY;
import static org.wikipedia.Constants.InvokeSource.ON_THIS_DAY_CARD_FOOTER;
import static org.wikipedia.Constants.InvokeSource.ON_THIS_DAY_CARD_YEAR;

public class OnThisDayCardView extends DefaultFeedCardView<OnThisDayCard> implements CardFooterView.Callback {
    @BindView(R.id.view_on_this_day_card_header) CardHeaderView headerView;
    @BindView(R.id.text) TextView descTextView;
    @BindView(R.id.year) TextView yearTextView;
    @BindView(R.id.years_text) TextView yearsInfoTextView;
    @BindView(R.id.pages_pager) ViewPager2 pagesViewPager;
    @BindView(R.id.pages_item_indicator_view) TabLayout indicatorView;
    @BindView(R.id.view_on_this_day_rtl_container) View rtlContainer;
    @BindView(R.id.card_footer_view) CardFooterView cardFooterView;
    @BindView(R.id.page_list_item_image) FaceAndColorDetectImageView otdEventImage;
    @BindView(R.id.page_list_item_title) TextView otdEventTitle;
    @BindView(R.id.page_list_item_description) TextView otdEventDescription;
    @BindView(R.id.otd_event_page) View otdEventView;
    private FeedFunnel funnel = new FeedFunnel(WikipediaApp.getInstance());
    private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();
    private int age;

    public OnThisDayCardView(@NonNull Context context) {
        super(context);
        inflate(getContext(), R.layout.view_card_on_this_day, this);
        ButterKnife.bind(this);
        setUpFooter();
    }

    private void setUpFooter() {
        indicatorView.setVisibility(GONE);
        cardFooterView.setFooterActionText(getContext().getString(R.string.more_events_text));
        cardFooterView.setCallback(this);
    }

    @Override
    public void onFooterClicked() {
        funnel.cardClicked(CardType.ON_THIS_DAY, getCard().wikiSite().languageCode());
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) getContext(),
                headerView.getTitleView(), getContext().getString(R.string.transition_on_this_day));
        getContext().startActivity(OnThisDayActivity.newIntent(getContext(), age, -1, getCard().wikiSite(),
                ON_THIS_DAY_CARD_FOOTER), options.toBundle());
    }

    @Override
    public void setCallback(@Nullable FeedAdapter.Callback callback) {
        super.setCallback(callback);
        headerView.setCallback(callback);
    }

    private void header(@NonNull OnThisDayCard card) {
        headerView.setTitle(card.title())
                .setLangCode(card.wikiSite().languageCode())
                .setCard(card)
                .setCallback(getCallback());
        descTextView.setText(card.text());
        yearTextView.setText(DateUtil.yearToStringWithEra(card.year()));
    }

    @Override
    public void setCard(@NonNull OnThisDayCard card) {
        super.setCard(card);
        this.age = card.getAge();
        setLayoutDirectionByWikiSite(card.wikiSite(), rtlContainer);
        yearsInfoTextView.setText(DateUtil.getYearDifferenceString(card.year()));
        updateOtdEventUI(card);
        header(card);
    }

    @OnClick({R.id.view_on_this_day_click_container, R.id.year}) void onCardClicked(View view) {
        boolean isYearClicked = view.getId() == R.id.year;
        funnel.cardClicked(CardType.ON_THIS_DAY, getCard().wikiSite().languageCode());
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) getContext(),
                headerView.getTitleView(), getContext().getString(R.string.transition_on_this_day));
        getContext().startActivity(OnThisDayActivity.newIntent(getContext(), age, isYearClicked ? getCard().year() : -1,
                getCard().wikiSite(), isYearClicked ? ON_THIS_DAY_CARD_YEAR : ON_THIS_DAY_CARD_BODY), options.toBundle());
    }

    private void updateOtdEventUI(OnThisDayCard card) {
        pagesViewPager.setVisibility(GONE);
        PageSummary chosenPage = null;
        if (card.pages() != null) {
            otdEventView.setVisibility(VISIBLE);
            for (PageSummary pageSummary : card.pages()) {
                chosenPage = pageSummary;
                if (pageSummary.getThumbnailUrl() != null) {
                    break;
                }
            }
            final PageSummary finalChosenPage = chosenPage;

            if (chosenPage != null) {
                if (chosenPage.getThumbnailUrl() == null) {
                    otdEventImage.setVisibility(GONE);
                } else {
                    otdEventImage.setVisibility(VISIBLE);
                    otdEventImage.loadImage(Uri.parse(chosenPage.getThumbnailUrl()));
                }
                otdEventDescription.setText(chosenPage.getDescription());
                otdEventDescription.setVisibility(TextUtils.isEmpty(chosenPage.getDescription()) ? View.GONE : View.VISIBLE);
                otdEventTitle.setMaxLines(TextUtils.isEmpty(chosenPage.getDescription()) ? 2 : 1);
                otdEventTitle.setText(StringUtil.fromHtml(chosenPage.getDisplayTitle()));
                otdEventView.setOnClickListener(view -> {
                    PageTitle pageTitle = finalChosenPage.getPageTitle(card.wikiSite());
                    HistoryEntry entry = new HistoryEntry(pageTitle, HistoryEntry.SOURCE_ON_THIS_DAY_CARD);

                    getContext().startActivity(PageActivity.newIntentForCurrentTab(getContext(), entry, pageTitle));
                });
                otdEventView.setOnLongClickListener(view -> {
                    PageTitle pageTitle = finalChosenPage.getPageTitle(card.wikiSite());
                    HistoryEntry entry = new HistoryEntry(pageTitle, HistoryEntry.SOURCE_ON_THIS_DAY_CARD);

                    new ReadingListBookmarkMenu(view, true, new ReadingListBookmarkMenu.Callback() {
                        @Override
                        public void onAddRequest(boolean addToDefault) {
                            if (addToDefault) {
                                ReadingListBehaviorsUtil.INSTANCE.addToDefaultList((AppCompatActivity) getContext(), entry.getTitle(), ON_THIS_DAY_CARD_BODY,
                                        readingListId ->
                                                bottomSheetPresenter.show(((AppCompatActivity) getContext()).getSupportFragmentManager(),
                                                        MoveToReadingListDialog.newInstance(readingListId, entry.getTitle(), ON_THIS_DAY_CARD_BODY)));
                            } else {
                                bottomSheetPresenter.show(((AppCompatActivity) getContext()).getSupportFragmentManager(),
                                        AddToReadingListDialog.newInstance(entry.getTitle(), ON_THIS_DAY_CARD_BODY));
                            }
                        }

                        @Override
                        public void onMoveRequest(@Nullable ReadingListPage page) {
                            bottomSheetPresenter.show(((AppCompatActivity) getContext()).getSupportFragmentManager(),
                                    MoveToReadingListDialog.newInstance(page.listId(), entry.getTitle(), ON_THIS_DAY_CARD_BODY));
                        }

                        @Override
                        public void onDeleted(@Nullable ReadingListPage page) {
                            FeedbackUtil.showMessage((AppCompatActivity) getContext(),
                                    getContext().getResources().getString(R.string.reading_list_item_deleted, entry.getTitle().getDisplayText()));
                        }

                        @Override
                        public void onShare() {
                            ShareUtil.shareText(getContext(), entry.getTitle());
                        }
                    }).show(entry.getTitle());

                    return true;
                });
            }

        } else {
            otdEventView.setVisibility(GONE);
        }
    }
}
