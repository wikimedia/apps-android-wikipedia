package org.wikipedia.feed;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.snackbar.Snackbar;

import org.wikipedia.BackPressedHandler;
import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.analytics.FeedFunnel;
import org.wikipedia.analytics.SuggestedEditsFunnel;
import org.wikipedia.descriptions.DescriptionEditActivity;
import org.wikipedia.feed.configure.ConfigureActivity;
import org.wikipedia.feed.configure.ConfigureItemLanguageDialogView;
import org.wikipedia.feed.configure.LanguageItemAdapter;
import org.wikipedia.feed.image.FeaturedImage;
import org.wikipedia.feed.image.FeaturedImageCard;
import org.wikipedia.feed.model.Card;
import org.wikipedia.feed.model.WikiSiteCard;
import org.wikipedia.feed.mostread.MostReadArticlesActivity;
import org.wikipedia.feed.mostread.MostReadListCard;
import org.wikipedia.feed.news.NewsItemCard;
import org.wikipedia.feed.random.RandomCardView;
import org.wikipedia.feed.suggestededits.SuggestedEditsCard;
import org.wikipedia.feed.suggestededits.SuggestedEditsCardView;
import org.wikipedia.feed.view.FeedAdapter;
import org.wikipedia.feed.view.FeedView;
import org.wikipedia.feed.view.HorizontalScrollingListCardItemView;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.language.LanguageSettingsInvokeSource;
import org.wikipedia.page.PageTitle;
import org.wikipedia.random.RandomActivity;
import org.wikipedia.readinglist.sync.ReadingListSyncAdapter;
import org.wikipedia.settings.Prefs;
import org.wikipedia.settings.SettingsActivity;
import org.wikipedia.settings.languages.WikipediaLanguagesActivity;
import org.wikipedia.suggestededits.SuggestedEditsFeedCardImageTagActivity;
import org.wikipedia.suggestededits.SuggestedEditsImageTagsOnboardingActivity;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;
import org.wikipedia.util.ThrowableUtil;
import org.wikipedia.util.UriUtil;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static android.app.Activity.RESULT_OK;
import static org.wikipedia.Constants.ACTIVITY_REQUEST_ADD_A_LANGUAGE;
import static org.wikipedia.Constants.ACTIVITY_REQUEST_DESCRIPTION_EDIT;
import static org.wikipedia.Constants.ACTIVITY_REQUEST_FEED_CONFIGURE;
import static org.wikipedia.Constants.ACTIVITY_REQUEST_SETTINGS;
import static org.wikipedia.Constants.ACTIVITY_REQUEST_SUGGESTED_EDITS_ONBOARDING;
import static org.wikipedia.Constants.InvokeSource.FEED;
import static org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_DESCRIPTION;
import static org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_IMAGE_TAGS;
import static org.wikipedia.descriptions.DescriptionEditActivity.Action.TRANSLATE_CAPTION;
import static org.wikipedia.descriptions.DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION;
import static org.wikipedia.language.AppLanguageLookUpTable.SIMPLIFIED_CHINESE_LANGUAGE_CODE;
import static org.wikipedia.language.AppLanguageLookUpTable.TRADITIONAL_CHINESE_LANGUAGE_CODE;

public class FeedFragment extends Fragment implements BackPressedHandler {
    @BindView(R.id.feed_swipe_refresh_layout) SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.fragment_feed_feed) FeedView feedView;
    @BindView(R.id.fragment_feed_empty_container) View emptyContainer;
    private Unbinder unbinder;
    private FeedAdapter<?> feedAdapter;
    private WikipediaApp app;
    private FeedCoordinator coordinator;
    private FeedFunnel funnel;
    private final FeedAdapter.Callback feedCallback = new FeedCallback();
    private FeedScrollListener feedScrollListener = new FeedScrollListener();
    private boolean searchIconVisible;
    @Nullable private SuggestedEditsCardView suggestedEditsCardView;

    public interface Callback {
        void onFeedSearchRequested();
        void onFeedVoiceSearchRequested();
        void onFeedSelectPage(HistoryEntry entry);
        void onFeedSelectPageFromExistingTab(HistoryEntry entry);
        void onFeedAddPageToList(HistoryEntry entry);
        void onFeedRemovePageFromList(HistoryEntry entry);
        void onFeedSharePage(HistoryEntry entry);
        void onFeedNewsItemSelected(NewsItemCard card, HorizontalScrollingListCardItemView view);
        void onFeedShareImage(FeaturedImageCard card);
        void onFeedDownloadImage(FeaturedImage image);
        void onFeaturedImageSelected(FeaturedImageCard card);
        void onLoginRequested();
        void updateToolbarElevation(boolean elevate);
    }

    @NonNull public static FeedFragment newInstance() {
        FeedFragment fragment = new FeedFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = WikipediaApp.getInstance();
        coordinator = new FeedCoordinator(app);
        coordinator.more(app.getWikiSite());
        funnel = new FeedFunnel(app);
    }

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,
                                                 @Nullable ViewGroup container,
                                                 @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_feed, container, false);

        unbinder = ButterKnife.bind(this, view);
        feedAdapter = new FeedAdapter<>(coordinator, feedCallback);
        feedView.setAdapter(feedAdapter);
        feedView.addOnScrollListener(feedScrollListener);

        swipeRefreshLayout.setColorSchemeResources(ResourceUtil.getThemedAttributeId(requireContext(), R.attr.colorAccent));
        swipeRefreshLayout.setOnRefreshListener(this::refresh);

        coordinator.setFeedUpdateListener(new FeedCoordinator.FeedUpdateListener() {
            @Override public void insert(Card card, int pos) {
                if (isAdded()) {
                    swipeRefreshLayout.setRefreshing(false);
                    if (feedView != null && feedAdapter != null) {
                        feedAdapter.notifyItemInserted(pos);
                    }
                }
            }

            @Override public void remove(Card card, int pos) {
                if (isAdded()) {
                    swipeRefreshLayout.setRefreshing(false);
                    if (feedView != null && feedAdapter != null) {
                        feedAdapter.notifyItemRemoved(pos);
                    }
                }
            }

            @Override
            public void finished(boolean shouldUpdatePreviousCard) {
                if (!isAdded()) {
                    return;
                }
                if (feedAdapter.getItemCount() < 2) {
                    emptyContainer.setVisibility(View.VISIBLE);
                } else {
                    if (shouldUpdatePreviousCard) {
                        feedAdapter.notifyItemChanged(feedAdapter.getItemCount() - 1);
                    }
                }
            }
        });

        if (getCallback() != null) {
            getCallback().updateToolbarElevation(shouldElevateToolbar());
        }

        ReadingListSyncAdapter.manualSync();

        return view;
    }

    private void showRemoveChineseVariantPrompt() {
        if (app.language().getAppLanguageCodes().contains(TRADITIONAL_CHINESE_LANGUAGE_CODE)
                && app.language().getAppLanguageCodes().contains(SIMPLIFIED_CHINESE_LANGUAGE_CODE)
                && Prefs.shouldShowRemoveChineseVariantPrompt()) {
            new AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.dialog_of_remove_chinese_variants_from_app_lang_title)
                    .setMessage(R.string.dialog_of_remove_chinese_variants_from_app_lang_text)
                    .setPositiveButton(R.string.dialog_of_remove_chinese_variants_from_app_lang_edit, (dialog, which)
                            -> showLanguagesActivity(LanguageSettingsInvokeSource.CHINESE_VARIANT_REMOVAL.text()))
                    .setNegativeButton(R.string.dialog_of_remove_chinese_variants_from_app_lang_no, null)
                    .show();
        }
        Prefs.shouldShowRemoveChineseVariantPrompt(false);
    }

    public boolean shouldElevateToolbar() {
        return searchIconVisible;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        showRemoveChineseVariantPrompt();
        funnel.enter();

    }

    @Override
    public void onPause() {
        super.onPause();
        funnel.exit();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_REQUEST_FEED_CONFIGURE
                && resultCode == ConfigureActivity.CONFIGURATION_CHANGED_RESULT) {
            coordinator.updateHiddenCards();
            refresh();
        } else if ((requestCode == ACTIVITY_REQUEST_SETTINGS
                && resultCode == SettingsActivity.ACTIVITY_RESULT_LANGUAGE_CHANGED)
                || requestCode == ACTIVITY_REQUEST_ADD_A_LANGUAGE) {
            refresh();
        } else if (requestCode == ACTIVITY_REQUEST_DESCRIPTION_EDIT) {
            SuggestedEditsFunnel.get().log();
            SuggestedEditsFunnel.reset();
            if (resultCode == RESULT_OK) {
                boolean isTranslation;
                if (suggestedEditsCardView != null && suggestedEditsCardView.getCard() != null) {
                    suggestedEditsCardView.refreshCardContent();
                    isTranslation = suggestedEditsCardView.isTranslation();
                    FeedbackUtil.showMessage(this, isTranslation && app.language().getAppLanguageCodes().size() > 1
                            ? getString(suggestedEditsCardView.getCard().getAction() == TRANSLATE_DESCRIPTION ? R.string.description_edit_success_saved_in_lang_snackbar : R.string.description_edit_success_saved_image_caption_in_lang_snackbar, app.language().getAppLanguageLocalizedName(app.language().getAppLanguageCodes().get(1)))
                            : getString(suggestedEditsCardView.getCard().getAction() == ADD_DESCRIPTION ? R.string.description_edit_success_saved_snackbar : (suggestedEditsCardView.getCard().getAction() == ADD_IMAGE_TAGS) ? R.string.description_edit_success_saved_image_tags_snackbar : R.string.description_edit_success_saved_image_caption_snackbar));
                }
            }
        } else if (requestCode == ACTIVITY_REQUEST_SUGGESTED_EDITS_ONBOARDING && resultCode == RESULT_OK) {
            startDescriptionEditScreen();
        }
    }

    private void startDescriptionEditScreen() {
        if (suggestedEditsCardView == null) {
            return;
        }
        DescriptionEditActivity.Action action = suggestedEditsCardView.getCard().getAction();
        if (action == ADD_IMAGE_TAGS) {
            startActivityForResult(SuggestedEditsFeedCardImageTagActivity.Companion.newIntent(requireActivity(), suggestedEditsCardView.getCard().getPage()), ACTIVITY_REQUEST_DESCRIPTION_EDIT);
            return;
        }
        PageTitle pageTitle = (action == TRANSLATE_DESCRIPTION || action == TRANSLATE_CAPTION)
                ? suggestedEditsCardView.getCard().getTargetSummary().getPageTitle()
                : suggestedEditsCardView.getCard().getSourceSummary().getPageTitle();
        startActivityForResult(DescriptionEditActivity.newIntent(requireContext(), pageTitle, null,
                suggestedEditsCardView.getCard().getSourceSummary(), suggestedEditsCardView.getCard().getTargetSummary(),
                action, FEED),
                ACTIVITY_REQUEST_DESCRIPTION_EDIT);
    }

    @Override
    public void onDestroyView() {
        coordinator.setFeedUpdateListener(null);
        swipeRefreshLayout.setOnRefreshListener(null);
        feedView.removeOnScrollListener(feedScrollListener);
        feedView.setAdapter(null);
        feedAdapter = null;
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        coordinator.reset();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_feed, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem searchItem = menu.findItem(R.id.menu_feed_search);
        if (searchItem != null) {
            searchItem.setVisible(searchIconVisible);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_feed_search:
                if (getCallback() != null) {
                    getCallback().onFeedSearchRequested();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onBackPressed() {
        return false;
    }

    @OnClick(R.id.fragment_feed_customize_button) void onCustomizeClick() {
        showConfigureActivity(-1);
    }

    public void scrollToTop() {
        feedView.smoothScrollToPosition(0);
    }

    public void onGoOffline() {
        feedAdapter.notifyDataSetChanged();
        coordinator.requestOfflineCard();
    }

    public void onGoOnline() {
        feedAdapter.notifyDataSetChanged();
        coordinator.removeOfflineCard();
        coordinator.incrementAge();
        coordinator.more(app.getWikiSite());
    }

    public void refresh() {
        funnel.refresh(coordinator.getAge());
        emptyContainer.setVisibility(View.GONE);
        coordinator.reset();
        feedAdapter.notifyDataSetChanged();
        coordinator.more(app.getWikiSite());
    }

    @Nullable private Callback getCallback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }

    private class FeedCallback implements FeedAdapter.Callback {
        @Override
        public void onShowCard(@Nullable Card card) {
            if (card != null) {
                funnel.cardShown(card.type(), getCardLanguageCode(card));

                if (card instanceof SuggestedEditsCard) {
                    ((SuggestedEditsCard) card).logImpression();
                }
            }
        }

        @Override
        public void onRequestMore() {
            funnel.requestMore(coordinator.getAge());
            feedView.post(() -> {
                if (isAdded()) {
                    coordinator.incrementAge();
                    coordinator.more(app.getWikiSite());
                }
            });
        }

        @Override
        public void onRetryFromOffline() {
            refresh();
        }

        @Override
        public void onError(@NonNull Throwable t) {
            FeedbackUtil.showError(requireActivity(), t);
        }

        @Override
        public void onSelectPage(@NonNull Card card, @NonNull HistoryEntry entry) {
            if (getCallback() != null) {
                getCallback().onFeedSelectPage(entry);
                funnel.cardClicked(card.type(), getCardLanguageCode(card));
            }
        }

        @Override
        public void onSelectPageFromExistingTab(@NonNull Card card, @NonNull HistoryEntry entry) {
            if (getCallback() != null) {
                getCallback().onFeedSelectPageFromExistingTab(entry);
                funnel.cardClicked(card.type(), getCardLanguageCode(card));
            }
        }

        @Override
        public void onAddPageToList(@NonNull HistoryEntry entry) {
            if (getCallback() != null) {
                getCallback().onFeedAddPageToList(entry);
            }
        }

        @Override
        public void onRemovePageFromList(@NonNull HistoryEntry entry) {
            if (getCallback() != null) {
                getCallback().onFeedRemovePageFromList(entry);
            }
        }

        @Override
        public void onSharePage(@NonNull HistoryEntry entry) {
            if (getCallback() != null) {
                getCallback().onFeedSharePage(entry);
            }
        }

        @Override
        public void onSearchRequested() {
            if (getCallback() != null) {
                getCallback().onFeedSearchRequested();
            }
        }

        @Override
        public void onVoiceSearchRequested() {
            if (getCallback() != null) {
                getCallback().onFeedVoiceSearchRequested();
            }
        }

        @Override
        public boolean onRequestDismissCard(@NonNull Card card) {
            int position = coordinator.dismissCard(card);
            if (position < 0) {
                return false;
            }
            funnel.dismissCard(card.type(), position);
            showDismissCardUndoSnackbar(card, position);
            return true;
        }

        @Override
        public void onRequestEditCardLanguages(@NonNull Card card) {
            showCardLangSelectDialog(card);
        }

        @Override
        public void onRequestCustomize(@NonNull Card card) {
            showConfigureActivity(card.type().code());
        }

        @Override
        public void onNewsItemSelected(@NonNull NewsItemCard card, @NonNull HorizontalScrollingListCardItemView view) {
            if (getCallback() != null) {
                funnel.cardClicked(card.type(), card.wikiSite().languageCode());
                getCallback().onFeedNewsItemSelected(card, view);
            }
        }

        @Override
        public void onShareImage(@NonNull FeaturedImageCard card) {
            if (getCallback() != null) {
                getCallback().onFeedShareImage(card);
            }
        }

        @Override
        public void onDownloadImage(@NonNull FeaturedImage image) {
            if (getCallback() != null) {
                getCallback().onFeedDownloadImage(image);
            }
        }

        @Override
        public void onFeaturedImageSelected(@NonNull FeaturedImageCard card) {
            if (getCallback() != null) {
                funnel.cardClicked(card.type(), null);
                getCallback().onFeaturedImageSelected(card);
            }
        }

        @Override
        public void onAnnouncementPositiveAction(@NonNull Card card, @NonNull Uri uri) {
            funnel.cardClicked(card.type(), getCardLanguageCode(card));
            if (uri.toString().equals(UriUtil.LOCAL_URL_LOGIN)) {
                if (getCallback() != null) {
                    getCallback().onLoginRequested();
                }
            } else if (uri.toString().equals(UriUtil.LOCAL_URL_SETTINGS)) {
                startActivityForResult(SettingsActivity.newIntent(requireContext()), ACTIVITY_REQUEST_SETTINGS);
            } else if (uri.toString().equals(UriUtil.LOCAL_URL_CUSTOMIZE_FEED)) {
                showConfigureActivity(card.type().code());
            } else if (uri.toString().equals(UriUtil.LOCAL_URL_LANGUAGES)) {
                showLanguagesActivity(LanguageSettingsInvokeSource.ANNOUNCEMENT.text());
            } else {
                UriUtil.handleExternalLink(requireContext(), uri);
            }
        }

        @Override
        public void onAnnouncementNegativeAction(@NonNull Card card) {
            onRequestDismissCard(card);
        }

        @Override
        public void onRandomClick(@NonNull RandomCardView view) {
            if (!app.isOnline()) {
                view.getRandomPage();
            } else {
                ActivityOptionsCompat options = ActivityOptionsCompat.
                        makeSceneTransitionAnimation(requireActivity(), view, ViewCompat.getTransitionName(view));
                startActivity(RandomActivity.newIntent(requireActivity(), FEED), options.toBundle());
            }
        }

        @Override
        public void onGetRandomError(@NonNull Throwable t, @NonNull final RandomCardView view) {
            Snackbar snackbar = FeedbackUtil.makeSnackbar(requireActivity(), ThrowableUtil.isOffline(t)
                    ? getString(R.string.view_wiki_error_message_offline) : t.getMessage(),
                    FeedbackUtil.LENGTH_DEFAULT);
            snackbar.setAction(R.string.page_error_retry, (v) -> view.getRandomPage());
            snackbar.show();
        }

        @Override
        public void onMoreContentSelected(@NonNull Card card) {
            startActivity(MostReadArticlesActivity.newIntent(requireContext(), (MostReadListCard) card));
        }

        @Override
        public void onSuggestedEditsCardClick(@NonNull SuggestedEditsCardView view) {
            funnel.cardClicked(view.getCard().type(), getCardLanguageCode(view.getCard()));
            suggestedEditsCardView = view;
            if (view.getCard().getAction() == ADD_IMAGE_TAGS && Prefs.shouldShowImageTagsOnboarding()) {
                Prefs.setShowImageTagsOnboarding(false);
                startActivityForResult(SuggestedEditsImageTagsOnboardingActivity.Companion.newIntent(requireContext()),
                        ACTIVITY_REQUEST_SUGGESTED_EDITS_ONBOARDING);
            } else {
                startDescriptionEditScreen();
            }
        }
    }

    private class FeedScrollListener extends RecyclerView.OnScrollListener {
        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            boolean shouldShowSearchIcon = feedView.getFirstVisibleItemPosition() != 0;
            if (shouldShowSearchIcon != searchIconVisible) {
                searchIconVisible = shouldShowSearchIcon;
                requireActivity().invalidateOptionsMenu();
                if (getCallback() != null) {
                    getCallback().updateToolbarElevation(shouldElevateToolbar());
                }
            }
        }
    }

    private void showDismissCardUndoSnackbar(final Card card, final int position) {
        Snackbar snackbar = FeedbackUtil.makeSnackbar(requireActivity(),
                getString(R.string.menu_feed_card_dismissed),
                FeedbackUtil.LENGTH_DEFAULT);
        snackbar.setAction(R.string.feed_undo_dismiss_card, (v) -> coordinator.undoDismissCard(card, position));
        snackbar.show();
    }

    private void showCardLangSelectDialog(@NonNull Card card) {
        FeedContentType contentType = card.type().contentType();
        if (contentType.isPerLanguage()) {
            LanguageItemAdapter adapter = new LanguageItemAdapter(requireContext(), contentType);
            ConfigureItemLanguageDialogView view = new ConfigureItemLanguageDialogView(requireContext());
            List<String> tempDisabledList = new ArrayList<>(contentType.getLangCodesDisabled());
            view.setContentType(adapter.getLangList(), tempDisabledList);
            new AlertDialog.Builder(requireContext())
                    .setView(view)
                    .setTitle(contentType.titleId())
                    .setPositiveButton(R.string.feed_lang_selection_dialog_ok_button_text, (dialog, which) -> {
                        contentType.getLangCodesDisabled().clear();
                        contentType.getLangCodesDisabled().addAll(tempDisabledList);
                        refresh();
                    })
                    .setNegativeButton(R.string.feed_lang_selection_dialog_cancel_button_text, null)
                    .create()
                    .show();
        }
    }

    public void showConfigureActivity(int invokeSource) {
        startActivityForResult(ConfigureActivity.newIntent(requireActivity(), invokeSource),
                Constants.ACTIVITY_REQUEST_FEED_CONFIGURE);
    }

    private void showLanguagesActivity(@NonNull String invokeSource) {
        Intent intent = WikipediaLanguagesActivity.newIntent(requireActivity(), invokeSource);
        startActivityForResult(intent, ACTIVITY_REQUEST_ADD_A_LANGUAGE);
    }

    @Nullable
    public String getCardLanguageCode(@Nullable Card card) {
        return (card instanceof WikiSiteCard) ? ((WikiSiteCard) card).wikiSite().languageCode() : null;
    }
}
