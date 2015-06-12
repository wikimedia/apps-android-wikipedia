package org.wikipedia.page;

import org.acra.ACRA;
import org.wikipedia.BackPressedHandler;
import org.wikipedia.NightModeHandler;
import org.wikipedia.R;
import org.wikipedia.Site;
import org.wikipedia.Utils;
import org.wikipedia.ViewAnimations;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.ConnectionIssueFunnel;
import org.wikipedia.analytics.LinkPreviewFunnel;
import org.wikipedia.analytics.SavedPagesFunnel;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.bridge.StyleBundle;
import org.wikipedia.editing.EditHandler;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.interlanguage.LangLinksActivity;
import org.wikipedia.page.gallery.GalleryActivity;
import org.wikipedia.page.leadimages.LeadImagesHandler;
import org.wikipedia.page.linkpreview.LinkPreviewDialog;
import org.wikipedia.page.linkpreview.LinkPreviewVersion;
import org.wikipedia.page.snippet.ShareHandler;
import org.wikipedia.savedpages.ImageUrlMap;
import org.wikipedia.savedpages.LoadSavedPageUrlMapTask;
import org.wikipedia.savedpages.SavePageTask;
import org.wikipedia.search.SearchBarHideHandler;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.NetworkUtils;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.views.SwipeRefreshLayoutWithScroll;
import org.wikipedia.views.WikiDrawerLayout;

import org.mediawiki.api.json.ApiException;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.SSLException;

// TODO: USE ACRA.getErrorReporter().handleSilentException() if we move to automated crash reporting?

public class PageViewFragmentInternal extends Fragment implements BackPressedHandler {
    private static final String TAG = "PageViewFragment";

    public static final int SUBSTATE_NONE = 0;
    public static final int SUBSTATE_PAGE_SAVED = 1;
    public static final int SUBSTATE_SAVED_PAGE_LOADED = 2;

    private PageLoadStrategy pageLoadStrategy = null;
    private PageViewModel model;

    /**
     * Whether to save the full page content as soon as it's loaded.
     * Used in the following cases:
     * - Stored page content is corrupted
     * - Page bookmarks are imported from the old app.
     * In the above cases, loading of the saved page will "fail", and will
     * automatically bounce to the online version of the page. Once the online page
     * loads successfully, the content will be saved, thereby reconstructing the
     * stored version of the page.
     */
    private boolean saveOnComplete = false;

    private ViewGroup imagesContainer;
    private LeadImagesHandler leadImagesHandler;
    private SearchBarHideHandler searchBarHideHandler;
    private ObservableWebView webView;
    private SwipeRefreshLayoutWithScroll refreshView;
    private View networkError;
    private View retryButton;
    private View pageDoesNotExistError;
    private WikiDrawerLayout tocDrawer;

    private CommunicationBridge bridge;
    private LinkHandler linkHandler;
    private ReferenceDialog referenceDialog;
    private EditHandler editHandler;
    private ActionMode findInPageActionMode;

    private WikipediaApp app;

    private SavedPagesFunnel savedPagesFunnel;
    private ConnectionIssueFunnel connectionIssueFunnel;

    private ShareHandler shareHandler;

    public ObservableWebView getWebView() {
        return webView;
    }

    public PageTitle getTitle() {
        return model.getTitle();
    }

    public Page getPage() {
        return model.getPage();
    }

    public HistoryEntry getHistoryEntry() {
        return model.getCurEntry();
    }

    public PageViewFragmentInternal() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = (WikipediaApp) getActivity().getApplicationContext();
        model = new PageViewModel();
        if (Prefs.isUsingExperimentalPageLoad(app)) {
            pageLoadStrategy = new HtmlPageLoadStrategy();
        } else {
            pageLoadStrategy = new JsonPageLoadStrategy();
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_page, container, false);
        webView = (ObservableWebView) rootView.findViewById(R.id.page_web_view);
        networkError = rootView.findViewById(R.id.page_error);
        retryButton = rootView.findViewById(R.id.page_error_retry);
        pageDoesNotExistError = rootView.findViewById(R.id.page_does_not_exist);
        tocDrawer = (WikiDrawerLayout) rootView.findViewById(R.id.page_toc_drawer);
        tocDrawer.setDragEdgeWidth(getResources().getDimensionPixelSize(R.dimen.drawer_drag_margin));

        refreshView = (SwipeRefreshLayoutWithScroll) rootView
                .findViewById(R.id.page_refresh_container);
        int swipeOffset = Utils.getActionBarSize(getActivity());
        refreshView.setProgressViewOffset(true, -swipeOffset, swipeOffset);
        refreshView.setSize(SwipeRefreshLayout.LARGE);
        // if we want to give it a custom color:
        //refreshView.setProgressBackgroundColor(R.color.swipe_refresh_circle);
        refreshView.setScrollableChild(webView);
        refreshView.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // don't refresh if it's still loading...
                if (pageLoadStrategy.isLoading()) {
                    refreshView.setRefreshing(false);
                    return;
                }
                if (model.getCurEntry().getSource() == HistoryEntry.SOURCE_SAVED_PAGE) {
                    // if it's a saved page, then refresh it and re-save!
                    refreshPage(true);
                } else {
                    // otherwise, refresh the page normally
                    refreshPage(false);
                }
            }
        });

        return rootView;
    }

    public void onDestroyView() {
        //uninitialize the bridge, so that no further JS events can have any effect.
        bridge.cleanup();
        shareHandler.onDestroy();
        super.onDestroyView();
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        connectionIssueFunnel = new ConnectionIssueFunnel(app);

        updateFontSize();

        // Explicitly set background color of the WebView (independently of CSS, because
        // the background may be shown momentarily while the WebView loads content,
        // creating a seizure-inducing effect, or at the very least, a migraine with aura).
        webView.setBackgroundColor(getResources().getColor(
                Utils.getThemedAttributeId(getActivity(), R.attr.page_background_color)));

        bridge = new CommunicationBridge(webView, "file:///android_asset/index.html");
        setupMessageHandlers();

        linkHandler = new LinkHandler(getActivity(), bridge) {
            @Override
            public void onPageLinkClicked(String anchor) {
                if (referenceDialog != null && referenceDialog.isShowing()) {
                    referenceDialog.dismiss();
                }
                JSONObject payload = new JSONObject();
                try {
                    payload.put("anchor", anchor);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                bridge.sendMessage("handleReference", payload);
            }

            @Override
            public void onInternalLinkClicked(PageTitle title) {
                handleInternalLink(title);
            }

            @Override
            public Site getSite() {
                return model.getTitle().getSite();
            }
        };

        new ReferenceHandler(bridge) {
            @Override
            protected void onReferenceClicked(String refHtml) {
                if (!isAdded()) {
                    Log.d("PageViewFragment",
                          "Detached from activity, so stopping reference click.");
                    return;
                }

                if (referenceDialog == null) {
                    referenceDialog = new ReferenceDialog(getActivity(), linkHandler);
                }
                referenceDialog.updateReference(refHtml);
                referenceDialog.show();
            }
        };

        new PageInfoHandler(((PageActivity) getActivity()), bridge) {
            @Override
            Site getSite() {
                return model.getTitle().getSite();
            }

            @Override
            int getDialogHeight() {
                // could have scrolled up a bit but the page info links must still be visible else they couldn't have been clicked
                return webView.getHeight() + webView.getScrollY() - imagesContainer.getHeight();
            }
        };

        if (!Prefs.isUsingExperimentalPageLoad(app)) {
            bridge.injectStyleBundle(StyleBundle.getAvailableBundle(StyleBundle.BUNDLE_PAGEVIEW));
        }

        // make sure styles get injected before the NightModeHandler and other handlers
        if (app.getCurrentTheme() == WikipediaApp.THEME_DARK) {
            new NightModeHandler(bridge).turnOn(true);
        }

        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retryButton.setEnabled(false);
                ViewAnimations.fadeOut(networkError, new Runnable() {
                    @Override
                    public void run() {
                        displayNewPage(model.getTitleOriginal(), model.getCurEntry(), true, false);
                        retryButton.setEnabled(true);
                    }
                });
            }
        });

        editHandler = new EditHandler(this, bridge);
        pageLoadStrategy.setEditHandler(editHandler);

        tocHandler = new ToCHandler(((PageActivity) getActivity()), tocDrawer, bridge);

        imagesContainer = (ViewGroup) getView().findViewById(R.id.page_images_container);
        leadImagesHandler = new LeadImagesHandler(getActivity(), this, bridge, webView,
                                                  imagesContainer);
        searchBarHideHandler = ((PageActivity) getActivity()).getSearchBarHideHandler();
        searchBarHideHandler.setScrollView(webView);

        shareHandler = new ShareHandler((PageActivity) getActivity(), bridge);

        pageLoadStrategy.setup(model, this, refreshView, webView, bridge, searchBarHideHandler,
                leadImagesHandler);
        pageLoadStrategy.onActivityCreated(savedInstanceState);
    }

    private void handleInternalLink(PageTitle title) {
        if (!isAdded()) {
            return;
        }
        if (referenceDialog != null && referenceDialog.isShowing()) {
            referenceDialog.dismiss();
        }
        if (app.getReleaseType() == WikipediaApp.RELEASE_PROD || LinkPreviewVersion.getVersion(app) == 0) {
            HistoryEntry historyEntry = new HistoryEntry(title,
                    HistoryEntry.SOURCE_INTERNAL_LINK);
            ((PageActivity) getActivity()).displayNewPage(title, historyEntry);
            new LinkPreviewFunnel(app, title).logNavigate();
        } else {
            // For version values 1 or 2, pass the value to the LinkPreviewDialog, which will use
            // the value to adjust its prototype layout.
            LinkPreviewDialog dialog = LinkPreviewDialog.newInstance(title, LinkPreviewVersion.getVersion(app));
            dialog.show(getActivity().getSupportFragmentManager(), "link_preview_dialog");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        pageLoadStrategy.onSaveInstanceState(outState);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("");
    }

    /**
     * Load a new page into the WebView in this fragment.
     * This shall be the single point of entry for loading content into the WebView, whether it's
     * loading an entirely new page, refreshing the current page, retrying a failed network
     * request, etc.
     * @param title Title of the new page to load.
     * @param entry HistoryEntry associated with the new page.
     * @param tryFromCache Whether to try loading the page from cache (otherwise load directly
     *                     from network).
     * @param pushBackStack Whether to push the new page onto the backstack.
     */
    public void displayNewPage(PageTitle title, HistoryEntry entry, boolean tryFromCache,
                               boolean pushBackStack) {
        displayNewPage(title, entry, tryFromCache, pushBackStack, 0);
    }

    /**
     * Load a new page into the WebView in this fragment.
     * This shall be the single point of entry for loading content into the WebView, whether it's
     * loading an entirely new page, refreshing the current page, retrying a failed network
     * request, etc.
     * @param title Title of the new page to load.
     * @param entry HistoryEntry associated with the new page.
     * @param tryFromCache Whether to try loading the page from cache (otherwise load directly
     *                     from network).
     * @param pushBackStack Whether to push the new page onto the backstack.
     */
    public void displayNewPage(PageTitle title, HistoryEntry entry, boolean tryFromCache,
                               boolean pushBackStack, int stagedScrollY) {
        // disable sliding of the ToC while sections are loading
        tocHandler.setEnabled(false);

        networkError.setVisibility(View.GONE);

        model.setTitle(title);
        model.setTitleOriginal(title);
        model.setCurEntry(entry);
        savedPagesFunnel = app.getFunnelManager().getSavedPagesFunnel(title.getSite());

        ((PageActivity) getActivity()).updateProgressBar(true, true, 0);

        pageLoadStrategy.onDisplayNewPage(pushBackStack, tryFromCache, stagedScrollY);
    }

    public Bitmap getLeadImageBitmap() {
        return leadImagesHandler.getLeadImageBitmap();
    }

    /**
     * Returns the normalized (0.0 to 1.0) vertical focus position of the lead image.
     * A value of 0.0 represents the top of the image, and 1.0 represents the bottom.
     * @return Normalized vertical focus position.
     */
    public float getLeadImageFocusY() {
        return leadImagesHandler.getLeadImageFocusY();
    }

    /**
     * Update the WebView's base font size, based on the specified font size from the app
     * preferences.
     */
    public void updateFontSize() {
        webView.getSettings().setDefaultFontSize((int) app.getFontSize(getActivity().getWindow()));
    }

    private void setupMessageHandlers() {
        bridge.addListener("ipaSpan", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                try {
                    String text = messagePayload.getString("contents");
                    final int textSize = 30;
                    TextView textView = new TextView(getActivity());
                    textView.setGravity(Gravity.CENTER);
                    textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
                    textView.setText(Html.fromHtml(text));
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setView(textView);
                    builder.show();
                } catch (JSONException e) {
                    ACRA.getErrorReporter().handleException(e);
                }
            }
        });
        bridge.addListener("imageClicked", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                try {
                    String href = Utils.decodeURL(messagePayload.getString("href"));
                    if (href.startsWith("/wiki/")) {
                        PageTitle imageTitle = model.getTitle().getSite().titleForInternalLink(href);
                        showImageGallery(imageTitle);
                    } else {
                        linkHandler.onUrlClick(href);
                    }
                } catch (JSONException e) {
                    ACRA.getErrorReporter().handleException(e);
                }
            }
        });
        bridge.addListener("mediaClicked", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                try {
                    String href = Utils.decodeURL(messagePayload.getString("href"));
                    showImageGallery(new PageTitle(href, model.getTitle().getSite()));
                } catch (JSONException e) {
                    ACRA.getErrorReporter().handleException(e);
                }
            }
        });
    }

    public void onActionModeShown(ActionMode mode) {
        shareHandler.onTextSelected(mode);
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PageActivity.ACTIVITY_REQUEST_EDIT_SECTION
            && resultCode == EditHandler.RESULT_REFRESH_PAGE) {
            pageLoadStrategy.backFromEditing(data);

            // and reload the page...
            displayNewPage(model.getTitleOriginal(), model.getCurEntry(), false, false);
        }
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!isAdded() || ((PageActivity)getActivity()).isSearching()) {
            return;
        }
        inflater.inflate(R.menu.menu_page_actions, menu);
    }

    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (!isAdded() || ((PageActivity)getActivity()).isSearching()) {
            return;
        }
        MenuItem savePageMenu = menu.findItem(R.id.menu_save_page);
        if (savePageMenu == null) {
            return;
        }

        MenuItem shareMenu = menu.findItem(R.id.menu_share_page);
        MenuItem otherLangMenu = menu.findItem(R.id.menu_other_languages);
        MenuItem findInPageMenu = menu.findItem(R.id.menu_find_in_page);
        MenuItem themeChooserMenu = menu.findItem(R.id.menu_themechooser);

        if (pageLoadStrategy.isLoading()) {
            savePageMenu.setEnabled(false);
            shareMenu.setEnabled(false);
            otherLangMenu.setEnabled(false);
            findInPageMenu.setEnabled(false);
            themeChooserMenu.setEnabled(false);
        } else {
            savePageMenu.setEnabled(true);
            shareMenu.setEnabled(true);
            // Only display "Read in other languages" if the article is in other languages
            otherLangMenu.setVisible(model.getPage() != null && model.getPage().getPageProperties().getLanguageCount() != 0);
            otherLangMenu.setEnabled(true);
            findInPageMenu.setEnabled(true);
            themeChooserMenu.setEnabled(true);
            int subState = pageLoadStrategy.getSubState();
            if (subState == SUBSTATE_PAGE_SAVED) {
                savePageMenu.setEnabled(false);
                savePageMenu.setTitle(WikipediaApp.getInstance().getString(R.string.menu_page_saved));
            } else if (subState == SUBSTATE_SAVED_PAGE_LOADED) {
                savePageMenu.setTitle(WikipediaApp.getInstance().getString(R.string.menu_refresh_saved_page));
            } else {
                savePageMenu.setTitle(WikipediaApp.getInstance().getString(R.string.menu_save_page));
            }
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.homeAsUp:
                // TODO SEARCH: add up navigation, see also http://developer.android.com/training/implementing-navigation/ancestral.html
                return true;
            case R.id.menu_toc:
                Utils.hideSoftKeyboard(getActivity());
                toggleToC(TOC_ACTION_TOGGLE);
                return true;
            case R.id.menu_save_page:
                // This means the user explicitly chose to save a new saved page
                app.getFunnelManager().getSavedPagesFunnel(model.getTitle().getSite()).logSaveNew();
                if (model.getCurEntry().getSource() == HistoryEntry.SOURCE_SAVED_PAGE) {
                    // refreshing a saved page...
                    refreshPage(true);
                } else {
                    savePage();
                }
                return true;
            case R.id.menu_share_page:
                shareHandler.shareWithoutSelection();
                return true;
            case R.id.menu_other_languages:
                Intent langIntent = new Intent();
                langIntent.setClass(getActivity(), LangLinksActivity.class);
                langIntent.setAction(LangLinksActivity.ACTION_LANGLINKS_FOR_TITLE);
                langIntent.putExtra(LangLinksActivity.EXTRA_PAGETITLE, model.getTitle());
                getActivity().startActivityForResult(langIntent, PageActivity.ACTIVITY_REQUEST_LANGLINKS);
                return true;
            case R.id.menu_find_in_page:
                showFindInPage();
                return true;
            case R.id.menu_themechooser:
                ((PageActivity) getActivity()).showThemeChooser();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void showFindInPage() {
        final PageActivity pageActivity = ((PageActivity) getActivity());
        final FindInPageActionProvider findInPageActionProvider = new FindInPageActionProvider(pageActivity);

        pageActivity.startSupportActionMode(new ActionMode.Callback() {
            private final String actionModeTag = "actionModeFindInPage";
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                findInPageActionMode = mode;
                MenuItem menuItem = menu.add(R.string.find_in_page);
                MenuItemCompat.setActionProvider(menuItem, findInPageActionProvider);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                mode.setTag(actionModeTag);
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                findInPageActionMode = null;
                webView.clearMatches();
                pageActivity.showToolbar();
                Utils.hideSoftKeyboard(pageActivity);
            }
        });
    }

    public boolean closeFindInPage() {
        if (findInPageActionMode != null) {
            findInPageActionMode.finish();
            return true;
        }
        return false;
    }

    /**
     * Scroll to a specific section in the WebView.
     * @param sectionAnchor Anchor link of the section to scroll to.
     */
    public void scrollToSection(String sectionAnchor) {
        if (!isAdded() || tocHandler == null) {
            return;
        }
        tocHandler.scrollToSection(sectionAnchor);
    }

    /**
     * Launch the image gallery activity, and start with the provided image.
     * @param imageTitle Image with which to begin the gallery.
     */
    public void showImageGallery(PageTitle imageTitle) {
        Intent galleryIntent = new Intent();
        galleryIntent.setClass(getActivity(), GalleryActivity.class);
        galleryIntent.putExtra(GalleryActivity.EXTRA_IMAGETITLE, imageTitle);
        galleryIntent.putExtra(GalleryActivity.EXTRA_PAGETITLE, model.getTitleOriginal());
        getActivity().startActivityForResult(galleryIntent, PageActivity.ACTIVITY_REQUEST_GALLERY);
    }

    public void onPageLoadComplete() {
        editHandler.setPage(model.getPage());
        if (saveOnComplete) {
            saveOnComplete = false;
            savedPagesFunnel.logUpdate();
            savePage();
        }
    }

    public PageTitle adjustPageTitleFromMobileview(PageTitle title, JSONObject mobileView)
            throws JSONException {
        if (mobileView.has("redirected")) {
            // Handle redirects properly.
            title = new PageTitle(mobileView.optString("redirected"), title.getSite(),
                    title.getThumbUrl());
        } else if (mobileView.has("normalizedtitle")) {
            // We care about the normalized title only if we were not redirected
            title = new PageTitle(mobileView.optString("normalizedtitle"), title.getSite(),
                    title.getThumbUrl());
        }
        if (mobileView.has("description")) {
            title.setDescription(Utils.capitalizeFirstChar(mobileView.getString("description")));
        }
        return title;
    }

    public void commonSectionFetchOnCatch(Throwable caught) {
        if (!isAdded()) {
            return;
        }
        // in any case, make sure the TOC drawer is closed
        tocDrawer.closeDrawers();
        ((PageActivity) getActivity()).updateProgressBar(false, true, 0);
        refreshView.setRefreshing(false);

        if (caught instanceof SectionsFetchException) {
            if (((SectionsFetchException) caught).getCode().equals("missingtitle")
                    || ((SectionsFetchException) caught).getCode().equals("invalidtitle")) {
                ViewAnimations.fadeIn(pageDoesNotExistError);
            }
        } else if (Utils.throwableContainsSpecificType(caught, SSLException.class)) {
            if (WikipediaApp.getInstance().incSslFailCount() < 2) {
                WikipediaApp.getInstance().setSslFallback(true);
                showNetworkError(null);
                try {
                    connectionIssueFunnel.logConnectionIssue("mdot", "commonSectionFetchOnCatch");
                } catch (Exception e) {
                    // meh
                }
            } else {
                showNetworkError(null);
                try {
                    connectionIssueFunnel.logConnectionIssue("desktop", "commonSectionFetchOnCatch");
                } catch (Exception e) {
                    // again, meh
                }
            }
        } else if (Utils.throwableContainsSpecificType(caught, JSONException.class)) {
            // If the server returns an numeric response code rather than an API response, it will
            // come as an integer rather than a JSON object or array, triggering a JSONException.
            Log.d(TAG, "Caught JSONException. Message: " + caught.getMessage());
            Pattern p = Pattern.compile(" \\d{3} ");
            Matcher m = p.matcher(caught.getMessage());
            if (m.find()) {
                String statusCode = m.group(0).trim();
                Log.d(TAG, "Found probable server response code " + statusCode);
                showNetworkError(statusCode);
            } else {
                showNetworkError(null);
            }
        } else if (caught instanceof ApiException) {
            showNetworkError(null);
        } else {
            throw new RuntimeException(caught);
        }
    }

    /**
     * Convenience method for hiding all the content of a page.
     */
    private void hidePageContent() {
        leadImagesHandler.hide();
        searchBarHideHandler.setFadeEnabled(false);
        pageLoadStrategy.onHidePageContent();
        webView.setVisibility(View.INVISIBLE);
    }

    private void showNetworkError(String statusCode) {
        TextView errorMessage = (TextView) networkError.findViewById(R.id.page_error_message);
        TextView statusCodeView = (TextView) networkError.findViewById(R.id.network_status_code);
        TextView statusMessageView = (TextView) networkError.findViewById(R.id.network_status_message);

        if (!NetworkUtils.isNetworkConnectionPresent(app)) {
            statusCodeView.setVisibility(View.GONE);
            statusMessageView.setVisibility(View.GONE);
            errorMessage.setText(R.string.error_network_error_try_again);
        } else {
            errorMessage.setText(R.string.generic_page_error);
            statusCodeView.setVisibility(View.VISIBLE);
            statusMessageView.setVisibility(View.VISIBLE);
            List<String> allStatusCodes = new ArrayList<>();
            allStatusCodes.addAll(Arrays.asList(app.getResources().getStringArray(R.array.status_codes)));
            // check probable returned code against list of status codes and display code/message if valid
            if (statusCode != null && allStatusCodes.contains(statusCode)) {
                statusCodeView.setText(statusCode);
                List<String> allStatusMessages = new ArrayList<>();
                allStatusMessages.addAll(Arrays.asList(app.getResources().getStringArray(R.array.status_messages)));
                statusMessageView.setText(allStatusMessages.get(allStatusCodes.indexOf(statusCode)));
            } else {
                statusMessageView.setText(R.string.status_code_unavailable);
            }
        }

        hidePageContent();
        ViewAnimations.fadeIn(networkError);
    }

    public void savePage() {
        if (model.getPage() == null) {
            return;
        }

        Toast.makeText(getActivity(), R.string.toast_saving_page, Toast.LENGTH_SHORT).show();
        new SavePageTask(getActivity(), model.getTitle(), model.getPage()) {
            @Override
            public void onFinish(Boolean success) {
                if (!isAdded()) {
                    Log.d("PageViewFragment", "Detached from activity, no toast.");
                    return;
                }

                pageLoadStrategy.setSubState(SUBSTATE_PAGE_SAVED);

                if (success) {
                    Toast.makeText(getActivity(), R.string.toast_saved_page, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getActivity(), R.string.toast_saved_page_missing_images, Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }

    /**
     * Read URL mappings from the saved page specific file
     */
    public void readUrlMappings() {
        new LoadSavedPageUrlMapTask(model.getTitle()) {
            @Override
            public void onFinish(JSONObject result) {
                // have we been unwittingly detached from our Activity?
                if (!isAdded()) {
                    Log.d("PageViewFragment", "Detached from activity, so stopping update.");
                    return;
                }

                ImageUrlMap.replaceImageSources(bridge, result);
            }

            @Override
            public void onCatch(Throwable caught) {

                /*
                If anything bad happens during loading of a saved page, then simply bounce it
                back to the online version of the page, and re-save the page contents locally when it's done.
                 */

                Log.d("LoadSavedPageTask", "Error loading saved page: " + caught.getMessage());
                caught.printStackTrace();

                refreshPage(true);
            }
        }.execute();
    }

    public void refreshPage(boolean saveOnComplete) {
        this.saveOnComplete = saveOnComplete;
        if (saveOnComplete) {
            Toast.makeText(getActivity(), R.string.toast_refresh_saved_page, Toast.LENGTH_LONG).show();
        }
        model.setCurEntry(new HistoryEntry(model.getTitle(), HistoryEntry.SOURCE_HISTORY));
        displayNewPage(model.getTitle(), model.getCurEntry(), false, false);
    }

    public static final int TOC_ACTION_SHOW = 0;
    public static final int TOC_ACTION_HIDE = 1;
    public static final int TOC_ACTION_TOGGLE = 2;

    private ToCHandler tocHandler;
    public void toggleToC(int action) {
        // tocHandler could still be null while the page is loading
        if (tocHandler == null) {
            return;
        }
        switch (action) {
            case TOC_ACTION_SHOW:
                tocHandler.show();
                break;
            case TOC_ACTION_HIDE:
                tocHandler.hide();
                break;
            case TOC_ACTION_TOGGLE:
                if (tocHandler.isVisible()) {
                    tocHandler.hide();
                } else {
                    tocHandler.show();
                }
                break;
            default:
                throw new RuntimeException("Unknown action!");
        }
    }

    public void setupToC(PageViewModel model, boolean isFirstPage) {
        tocHandler.setupToC(model.getPage(), model.getTitle().getSite(), isFirstPage);
        tocHandler.setEnabled(true);
    }

    public boolean onBackPressed() {
        if (tocHandler != null && tocHandler.isVisible()) {
            tocHandler.hide();
            return true;
        }
        if (closeFindInPage()) {
            return true;
        }
        return pageLoadStrategy.onBackPressed();
    }

    public LinkHandler getLinkHandler() {
        return linkHandler;
    }
}
