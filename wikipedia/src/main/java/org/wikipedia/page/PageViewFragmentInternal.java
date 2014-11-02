package org.wikipedia.page;

import org.wikipedia.NightModeHandler;
import org.wikipedia.PageTitle;
import org.wikipedia.QuickReturnHandler;
import org.wikipedia.R;
import org.wikipedia.Site;
import org.wikipedia.Utils;
import org.wikipedia.ViewAnimations;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.ConnectionIssueFunnel;
import org.wikipedia.analytics.SavedPagesFunnel;
import org.wikipedia.bridge.CommunicationBridge;
import org.wikipedia.bridge.StyleLoader;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.editing.EditHandler;
import org.wikipedia.editing.EditSectionActivity;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.interlanguage.LangLinksActivity;
import org.wikipedia.pageimages.PageImage;
import org.wikipedia.pageimages.PageImagesTask;
import org.wikipedia.savedpages.ImageUrlMap;
import org.wikipedia.savedpages.LoadSavedPageTask;
import org.wikipedia.savedpages.LoadSavedPageUrlMapTask;
import org.wikipedia.savedpages.SavePageTask;
import org.wikipedia.views.DisableableDrawerLayout;
import org.wikipedia.views.ObservableWebView;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiException;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.view.ActionMode;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import javax.net.ssl.SSLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * "Fragment" that displays a single Page (WebView plus ToC drawer).
 * Notice that this class has all the methods of a Fragment, but does not actually derive
 * from Fragment. This class is instantiated, and becomes a puppet of, the PageViewFragment
 * class, which does in fact derive from Fragment.
 *
 * This class handles all of the heavy logic of displaying and interacting with a Page, but
 * is also designed to be fully disposable when no longer needed.  This allows a large number
 * of PageViewFragments to be in the backstack of the FragmentManager, without significantly
 * impacting memory usage.
 */
public class PageViewFragmentInternal {
    private static final String TAG = "PageViewFragmentInternal";

    public static final int STATE_NO_FETCH = 1;
    public static final int STATE_INITIAL_FETCH = 2;
    public static final int STATE_COMPLETE_FETCH = 3;

    public static final int SUBSTATE_NONE = 0;
    public static final int SUBSTATE_PAGE_SAVED = 1;
    public static final int SUBSTATE_SAVED_PAGE_LOADED = 2;

    private int state = STATE_NO_FETCH;
    private int subState = SUBSTATE_NONE;

    private static final int MAX_PROGRESS_VALUE = 10000;

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

    private PageViewFragment parentFragment;

    /**
     * Our page cache, which discards the eldest entries based on access time.
     * This will allow the user to go "back" smoothly (the previous page is guaranteed
     * to be in cache), but also to go "forward" smoothly (if the user clicks on a link
     * that was already visited within a short time).
     */
    private static PageCache PAGE_CACHE;

    private PageTitle title;
    private PageTitle titleOriginal;
    private ObservableWebView webView;
    private View networkError;
    private View retryButton;
    private View pageDoesNotExistError;
    private DisableableDrawerLayout tocDrawer;
    private Page page;
    private HistoryEntry curEntry;

    private int sectionTargetFromIntent;
    private String sectionTargetFromTitle;

    private CommunicationBridge bridge;
    private LinkHandler linkHandler;
    private ReferenceDialog referenceDialog;
    private EditHandler editHandler;
    private NightModeHandler nightModeHandler;
    private ActionMode findInPageActionMode;

    private WikipediaApp app;

    private int scrollY;
    public int getScrollY() {
        return webView.getScrollY();
    }

    private SavedPagesFunnel savedPagesFunnel;
    private ConnectionIssueFunnel connectionIssueFunnel;

    public PageViewFragmentInternal(PageViewFragment parentFragment, PageTitle title, HistoryEntry historyEntry, int scrollY) {
        this.parentFragment = parentFragment;
        this.title = title;
        this.curEntry = historyEntry;
        this.scrollY = scrollY;
        if (PAGE_CACHE == null) {
            PAGE_CACHE = new PageCache();
        }
    }

    public PageActivity getActivity() {
        return (PageActivity)parentFragment.getActivity();
    }

    public boolean isAdded() {
        return parentFragment.isAdded();
    }

    private Resources getResources() {
        return parentFragment.getResources();
    }

    private String getString(int resId) {
        return parentFragment.getString(resId);
    }

    private String getString(int resId, Object... formatArgs) {
        return parentFragment.getString(resId, formatArgs);
    }


    public ObservableWebView getWebView() {
        return webView;
    }

    public PageTitle getTitle() {
        return title;
    }

    public Page getPage() {
        return page;
    }

    public HistoryEntry getHistoryEntry() {
        return curEntry;
    }

    private void displayLeadSection() {
        JSONObject leadSectionPayload = new JSONObject();
        try {
            leadSectionPayload.put("title", page.getDisplayTitle());
            leadSectionPayload.put("section", page.getSections().get(0).toJSON());
            leadSectionPayload.put("string_page_similar_titles", getString(R.string.page_similar_titles));
            leadSectionPayload.put("string_page_issues", getString(R.string.button_page_issues));
            leadSectionPayload.put("isBeta", app.getReleaseType() != WikipediaApp.RELEASE_PROD);
            bridge.sendMessage("displayLeadSection", leadSectionPayload);

            JSONObject attributionPayload = new JSONObject();
            String lastUpdatedText = getString(R.string.last_updated_text, Utils.formatDateRelative(page.getPageProperties().getLastModified()));
            attributionPayload.put("historyText", lastUpdatedText);
            attributionPayload.put("historyTarget", page.getTitle().getUriForAction("history"));
            attributionPayload.put("licenseHTML", getString(R.string.content_license_html));
            bridge.sendMessage("displayAttribution", attributionPayload);

            Utils.setupDirectionality(title.getSite().getLanguage(), Locale.getDefault().getLanguage(), bridge);

            // Hide edit pencils if anon editing is disabled by remote killswitch or if this is a file page
            JSONObject miscPayload = new JSONObject();
            boolean isAnonEditingDisabled = app.getRemoteConfig().getConfig().optBoolean("disableAnonEditing", false)
                    && !app.getUserInfoStorage().isLoggedIn();
            miscPayload.put("noedit", (isAnonEditingDisabled
                    || title.isFilePage()
                    || page.getPageProperties().isMainPage()));
            miscPayload.put("protect", !page.getPageProperties().canEdit());
            bridge.sendMessage("setPageProtected", miscPayload);
        } catch (JSONException e) {
            // This should never happen
            throw new RuntimeException(e);
        }

        if (webView.getVisibility() == View.GONE) {
            ViewAnimations.fadeIn(webView);
        }

        getActivity().updateProgressBar(true, true, 0);
    }

    private void displayNonLeadSection(int index) {
        getActivity().updateProgressBar(true, false, MAX_PROGRESS_VALUE / page.getSections().size() * index);

        try {
            JSONObject wrapper = new JSONObject();
            if (index < page.getSections().size()) {
                wrapper.put("section", page.getSections().get(index).toJSON());
                wrapper.put("index", index);
                if (sectionTargetFromIntent > 0) {
                    //if we have a section to scroll to (from our Intent):
                    wrapper.put("fragment", page.getSections().get(sectionTargetFromIntent).getAnchor());
                } else if (sectionTargetFromTitle != null) {
                    //if we have a section to scroll to (from our PageTitle):
                    wrapper.put("fragment", sectionTargetFromTitle);
                }
            } else {
                wrapper.put("noMore", true);
            }
            //give it our expected scroll position, in case we need the page to be pre-scrolled upon loading.
            wrapper.put("scrollY", (int)(scrollY / getResources().getDisplayMetrics().density));
            bridge.sendMessage("displaySection", wrapper);
        } catch (JSONException e) {
            //nope
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, final Bundle savedInstanceState) {
        View rootView =  inflater.inflate(R.layout.fragment_page, container, false);
        webView = (ObservableWebView) rootView.findViewById(R.id.page_web_view);
        networkError = rootView.findViewById(R.id.page_error);
        retryButton = rootView.findViewById(R.id.page_error_retry);
        pageDoesNotExistError = rootView.findViewById(R.id.page_does_not_exist);
        tocDrawer = (DisableableDrawerLayout) rootView.findViewById(R.id.page_toc_drawer);

        return rootView;
    }

    public void onDestroyView() {
        //uninitialize the bridge, so that no further JS events can have any effect.
        bridge.cleanup();
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        if (title == null) {
            throw new RuntimeException("No PageTitle passed in to constructor or in instanceState");
        }
        titleOriginal = title;

        //save any section-specific link target from the title, since the title may be
        //replaced (normalized)
        sectionTargetFromTitle = title.getFragment();

        app = (WikipediaApp)getActivity().getApplicationContext();

        // disable TOC drawer until the page is loaded
        tocDrawer.setSlidingEnabled(false);

        savedPagesFunnel = app.getFunnelManager().getSavedPagesFunnel(title.getSite());

        connectionIssueFunnel = new ConnectionIssueFunnel(app);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Enable Pinch-Zoom
            webView.getSettings().setBuiltInZoomControls(true);
            webView.getSettings().setDisplayZoomControls(false);
        }

        updateFontSize();

        // Explicitly set background color of the WebView (independently of CSS, because
        // the background may be shown momentarily while the WebView loads content,
        // creating a seizure-inducing effect, or at the very least, a migraine with aura).
        webView.setBackgroundColor(getResources().getColor(Utils.getThemedAttributeId(getActivity(), R.attr.page_background_color)));

        bridge = new CommunicationBridge(webView, "file:///android_asset/index.html");
        setupMessageHandlers();

        Utils.setupDirectionality(title.getSite().getLanguage(), Locale.getDefault().getLanguage(), bridge);
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
                if (referenceDialog != null && referenceDialog.isShowing()) {
                    referenceDialog.dismiss();
                }
                HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_INTERNAL_LINK);
                getActivity().displayNewPage(title, historyEntry);
            }

            @Override
            public Site getSite() {
                return title.getSite();
            }
        };

        new ReferenceHandler(bridge) {
            @Override
            protected void onReferenceClicked(String refHtml) {
                if (!isAdded()) {
                    Log.d("PageViewFragment", "Detached from activity, so stopping reference click.");
                    return;
                }

                if (referenceDialog == null) {
                    referenceDialog = new ReferenceDialog(getActivity(), linkHandler);
                }
                referenceDialog.updateReference(refHtml);
                referenceDialog.show();
            }
        };

        new IssuesHandler(getActivity(), bridge);

        new DisambigHandler(getActivity(), bridge){
            @Override
            public LinkHandler getLinkHandler() {
                return linkHandler;
            }
        };

        bridge.injectStyleBundle(app.getStyleLoader().getAvailableBundle(StyleLoader.BUNDLE_PAGEVIEW));

        if (app.getCurrentTheme() == WikipediaApp.THEME_DARK) {
            nightModeHandler = new NightModeHandler(bridge);
            nightModeHandler.turnOn(true);
        }

        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                retryButton.setEnabled(false);
                ViewAnimations.fadeOut(networkError, new Runnable() {
                    @Override
                    public void run() {
                        performActionForState(state);
                        retryButton.setEnabled(true);
                    }
                });
            }
        });

        editHandler = new EditHandler(parentFragment, bridge);

        new QuickReturnHandler(webView, getActivity());

        //is this page in cache??
        if (PAGE_CACHE.has(titleOriginal)) {
            Log.d(TAG, "Using page from cache: " + titleOriginal.getDisplayText());
            page = PAGE_CACHE.get(titleOriginal);
            //make the webview immediately visible
            webView.setVisibility(View.VISIBLE);
            state = STATE_COMPLETE_FETCH;
        }

        setState(state);
        performActionForState(state);
    }

    /**
     * Update the WebView's base font size, based on the specified font size from the app preferences.
     */
    public void updateFontSize() {
        webView.getSettings().setDefaultFontSize((int) app.getFontSize(getActivity().getWindow()));
    }

    private void setupMessageHandlers() {
        bridge.addListener("requestSection", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                if (!isAdded()) {
                    return;
                }
                try {
                    displayNonLeadSection(messagePayload.getInt("index"));
                } catch (JSONException e) {
                    //nope
                }
            }
        });
        bridge.addListener("pageLoadComplete", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                if (!isAdded()) {
                    return;
                }
                // Do any other stuff that should happen upon page load completion...
                getActivity().updateProgressBar(false, true, 0);
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PageActivity.ACTIVITY_REQUEST_EDIT_SECTION
                && resultCode == EditHandler.RESULT_REFRESH_PAGE) {
            //Retrieve section ID from intent, and find correct section, so where know where to scroll to
            sectionTargetFromIntent = data.getIntExtra(EditSectionActivity.EXTRA_SECTION_ID, 0);
            //reset our scroll offset, since we have a section scroll target
            scrollY = 0;

            // immediately hide the webview
            webView.setVisibility(View.GONE);
            // and reload the page...
            setState(STATE_NO_FETCH);
            performActionForState(state);
        }
    }

    private void performActionForState(int forState) {
        switch (forState) {
            case STATE_NO_FETCH:
                getActivity().updateProgressBar(true, true, 0);
                bridge.sendMessage("clearContents", new JSONObject());
                if (curEntry.getSource() == HistoryEntry.SOURCE_SAVED_PAGE) {
                    loadSavedPage();
                } else {
                    new LeadSectionFetchTask().execute();
                }
                break;
            case STATE_INITIAL_FETCH:
                new RestSectionsFetchTask().execute();
                break;
            case STATE_COMPLETE_FETCH:
                editHandler.setPage(page);
                displayLeadSection();
                displayNonLeadSection(1);
                break;
            default:
                // This should never happen
                throw new RuntimeException("Unknown state encountered " + state);
        }
    }

    private void setState(int state) {
        setState(state, SUBSTATE_NONE);
    }

    private void setState(int state, int subState) {
        this.state = state;
        this.subState = subState;
        getActivity().supportInvalidateOptionsMenu();

        // FIXME: Move this out into a PageComplete event of sorts
        if (state == STATE_COMPLETE_FETCH) {
            if (tocHandler == null) {
                tocHandler = new ToCHandler(getActivity(),
                                            tocDrawer,
                                            bridge);
            }
            tocHandler.setupToC(page);

            getActivity().supportInvalidateOptionsMenu();

            //add the page to cache!
            PAGE_CACHE.put(titleOriginal, page);
            if (!titleOriginal.equals(title)) {
                PAGE_CACHE.put(title, page);
            }
        }
    }

    public void onPrepareOptionsMenu(Menu menu) {
        if (tocDrawer == null) {
            // on GB onPrepareOptionsMenu is called before onCreateView, and multiple times afterwards
            return;
        }

        app.adjustDrawableToTheme(getResources().getDrawable(R.drawable.toc_collapsed));
        app.adjustDrawableToTheme(getResources().getDrawable(R.drawable.toc_expanded));

        MenuItem tocMenuItem = menu.findItem(R.id.menu_toc);
        tocMenuItem.setVisible(tocDrawer.getSlidingEnabled(Gravity.END));
        tocMenuItem.setIcon(tocDrawer.isDrawerOpen(Gravity.END) ? R.drawable.toc_expanded : R.drawable.toc_collapsed);

        switch (state) {
            case PageViewFragmentInternal.STATE_NO_FETCH:
            case PageViewFragmentInternal.STATE_INITIAL_FETCH:
                menu.findItem(R.id.menu_save_page).setEnabled(false);
                menu.findItem(R.id.menu_share_page).setEnabled(false);
                menu.findItem(R.id.menu_other_languages).setEnabled(false);
                menu.findItem(R.id.menu_find_in_page).setEnabled(false);
                menu.findItem(R.id.menu_themechooser).setEnabled(false);
                break;
            case PageViewFragmentInternal.STATE_COMPLETE_FETCH:
                menu.findItem(R.id.menu_save_page).setEnabled(true);
                menu.findItem(R.id.menu_share_page).setEnabled(true);
                menu.findItem(R.id.menu_other_languages).setEnabled(true);
                menu.findItem(R.id.menu_find_in_page).setEnabled(true);
                menu.findItem(R.id.menu_themechooser).setEnabled(true);
                if (subState == PageViewFragmentInternal.SUBSTATE_PAGE_SAVED) {
                    menu.findItem(R.id.menu_save_page).setEnabled(false);
                    menu.findItem(R.id.menu_save_page).setTitle(WikipediaApp.getInstance().getString(R.string.menu_page_saved));
                } else if (subState == PageViewFragmentInternal.SUBSTATE_SAVED_PAGE_LOADED) {
                    menu.findItem(R.id.menu_save_page).setTitle(WikipediaApp.getInstance().getString(R.string.menu_refresh_saved_page));
                } else {
                    menu.findItem(R.id.menu_save_page).setTitle(WikipediaApp.getInstance().getString(R.string.menu_save_page));
                }
                break;
            default:
                // How can this happen?!
                throw new RuntimeException("This can't happen");
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
                app.getFunnelManager().getSavedPagesFunnel(title.getSite()).logSaveNew();
                if (curEntry.getSource() == HistoryEntry.SOURCE_SAVED_PAGE) {
                    // refreshing a saved page...
                    refreshPage(true);
                } else {
                    savePage();
                }
                return true;
            case R.id.menu_share_page:
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.putExtra(Intent.EXTRA_TEXT, title.getCanonicalUri());
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, title.getDisplayText());
                shareIntent.setType("text/plain");
                Intent chooser = Intent.createChooser(shareIntent, getResources().getString(R.string.share_via));
                parentFragment.startActivity(chooser);
                return true;
            case R.id.menu_other_languages:
                Intent langIntent = new Intent();
                langIntent.setClass(getActivity(), LangLinksActivity.class);
                langIntent.setAction(LangLinksActivity.ACTION_LANGLINKS_FOR_TITLE);
                langIntent.putExtra(LangLinksActivity.EXTRA_PAGETITLE, title);
                getActivity().startActivityForResult(langIntent, PageActivity.ACTIVITY_REQUEST_LANGLINKS);
                return true;
            case R.id.menu_find_in_page:
                showFindInPage();
                return true;
            case R.id.menu_themechooser:
                getActivity().showThemeChooser();
                return true;
            default:
                return false;
        }
    }

    public void showFindInPage() {
        final PageActivity pageActivity = getActivity();
        final FindInPageActionProvider findInPageActionProvider = new FindInPageActionProvider(pageActivity);

        pageActivity.startSupportActionMode(new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                findInPageActionMode = mode;
                MenuItem menuItem = menu.add(R.string.find_in_page);
                MenuItemCompat.setActionProvider(menuItem, findInPageActionProvider);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
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
                if (!pageActivity.getSupportActionBar().isShowing()) {
                    pageActivity.getSupportActionBar().show();
                }
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
     * Save the history entry for the specified page.
     */
    private class SaveHistoryTask extends SaneAsyncTask<Void> {
        private final HistoryEntry entry;
        public SaveHistoryTask(HistoryEntry entry) {
            super(SINGLE_THREAD);
            this.entry = entry;
        }

        @Override
        public Void performTask() throws Throwable {
            app.getPersister(HistoryEntry.class).persist(entry);
            return null;
        }

        @Override
        public void onCatch(Throwable caught) {
            Log.w("SaveHistoryTask", "Caught " + caught.getMessage(), caught);
        }
    }

    private class LeadSectionFetchTask extends SectionsFetchTask {
        public LeadSectionFetchTask() {
            super(getActivity(), title, "0");
        }

        @Override
        public RequestBuilder buildRequest(Api api) {
            RequestBuilder builder =  super.buildRequest(api);
            builder.param("prop", builder.getParams().get("prop") + "|" + Page.API_REQUEST_PROPS);
            builder.param("appInstallID", app.getAppInstallID());
            return builder;
        }

        private PageProperties pageProperties;

        @Override
        public List<Section> processResult(ApiResult result) throws Throwable {
            JSONObject mobileView = result.asObject().optJSONObject("mobileview");
            if (mobileView != null) {
                pageProperties = PageProperties.parseJSON(mobileView);
                if (mobileView.has("redirected")) {
                    // Handle redirects properly.
                    title = new PageTitle(mobileView.optString("redirected"), title.getSite(), title.getThumbUrl());
                } else if (mobileView.has("normalizedtitle")) {
                    // We care about the normalized title only if we were not redirected
                    title = new PageTitle(mobileView.optString("normalizedtitle"), title.getSite(), title.getThumbUrl());
                }
            }
            return super.processResult(result);
        }

        @Override
        public void onFinish(List<Section> result) {
            // have we been unwittingly detached from our Activity?
            if (!isAdded()) {
                Log.d("PageViewFragment", "Detached from activity, so stopping update.");
                return;
            }

            page = new Page(title, (ArrayList<Section>) result, pageProperties);
            editHandler.setPage(page);
            displayLeadSection();
            setState(STATE_INITIAL_FETCH);
            new RestSectionsFetchTask().execute();

            // Update our history entry, in case the Title was changed (i.e. normalized)
            curEntry = new HistoryEntry(title, curEntry.getTimestamp(), curEntry.getSource());

            // Save history entry and page image url
            new SaveHistoryTask(curEntry).execute();

            // Fetch larger thumbnail URL for the page, to be shown in History and Saved Pages
            (new PageImagesTask(app.getAPIForSite(title.getSite()), title.getSite(),
                                Arrays.asList(new PageTitle[] {title}), WikipediaApp.PREFERRED_THUMB_SIZE) {
                @Override
                public void onFinish(Map<PageTitle, String> result) {
                    if (result.containsKey(title)) {
                        title.setThumbUrl(result.get(title));
                        PageImage pi = new PageImage(title, result.get(title));
                        app.getPersister(PageImage.class).upsert(pi);
                    }
                }

                @Override
                public void onCatch(Throwable caught) {
                    // Thumbnails are expendable
                    Log.w("SaveThumbnailTask", "Caught " + caught.getMessage(), caught);
                }
            }).execute();

        }

        @Override
        public void onCatch(Throwable caught) {
            commonSectionFetchOnCatch(caught);
        }
    }

    private class RestSectionsFetchTask extends SectionsFetchTask {
        public RestSectionsFetchTask() {
            super(getActivity(), title, "1-");
        }

        @Override
        public void onFinish(List<Section> result) {
            // have we been unwittingly detached from our Activity?
            if (!isAdded()) {
                Log.d("PageViewFragment", "Detached from activity, so stopping update.");
                return;
            }
            ArrayList<Section> newSections = (ArrayList<Section>) page.getSections().clone();
            newSections.addAll(result);
            page = new Page(page.getTitle(), newSections, page.getPageProperties());
            editHandler.setPage(page);
            displayNonLeadSection(1);
            setState(STATE_COMPLETE_FETCH);

            if (saveOnComplete) {
                saveOnComplete = false;
                savedPagesFunnel.logUpdate();
                savePage();
            }
        }

        @Override
        public void onCatch(Throwable caught) {
            commonSectionFetchOnCatch(caught);
        }
    }

    private void commonSectionFetchOnCatch(Throwable caught) {
        if (!isAdded()) {
            return;
        }
        // in any case, make sure the TOC drawer is closed and disabled
        tocDrawer.setSlidingEnabled(false);
        getActivity().updateProgressBar(false, true, 0);

        if (caught instanceof SectionsFetchException) {
            if (((SectionsFetchException) caught).getCode().equals("missingtitle")
                    || ((SectionsFetchException) caught).getCode().equals("invalidtitle")) {
                ViewAnimations.fadeIn(pageDoesNotExistError);
            }
        } else if (Utils.throwableContainsSpecificType(caught, SSLException.class)) {
            if (WikipediaApp.getInstance().incSslFailCount() < 2) {
                WikipediaApp.getInstance().setSslFallback(true);
                showNetworkError();
                try {
                    connectionIssueFunnel.logConnectionIssue("mdot", "commonSectionFetchOnCatch");
                } catch (Exception e) {
                    // meh
                }
            } else {
                showNetworkError();
                try {
                    connectionIssueFunnel.logConnectionIssue("desktop", "commonSectionFetchOnCatch");
                } catch (Exception e) {
                    // again, meh
                }
            }
        } else if (caught instanceof ApiException) {
            showNetworkError();
        } else {
            throw new RuntimeException(caught);
        }
    }

    private void showNetworkError() {
        // Check for the source of the error and have different things turn up
        ViewAnimations.fadeIn(networkError);
        // Not sure why this is required, but without it tapping retry hides networkError
        // FIXME: INVESTIGATE WHY THIS HAPPENS!
        networkError.setVisibility(View.VISIBLE);
    }

    public void savePage() {
        if (page == null) {
            return;
        }

        Toast.makeText(getActivity(), R.string.toast_saving_page, Toast.LENGTH_SHORT).show();
        new SavePageTask(getActivity(), title, page) {
            @Override
            public void onFinish(Boolean success) {
                if (!isAdded()) {
                    Log.d("PageViewFragment", "Detached from activity, no toast.");
                    return;
                }

                setState(state, SUBSTATE_PAGE_SAVED);

                if (success) {
                    Toast.makeText(getActivity(), R.string.toast_saved_page, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getActivity(), R.string.toast_saved_page_missing_images, Toast.LENGTH_LONG).show();
                }
            }
        }.execute();
    }

    public void loadSavedPage() {
        new LoadSavedPageTask(title) {
            @Override
            public void onFinish(Page result) {
                // have we been unwittingly detached from our Activity?
                if (!isAdded()) {
                    Log.d("PageViewFragment", "Detached from activity, so stopping update.");
                    return;
                }

                // Save history entry and page image url
                new SaveHistoryTask(curEntry).execute();

                page = result;
                editHandler.setPage(page);
                displayLeadSection();
                displayNonLeadSection(1);
                setState(STATE_COMPLETE_FETCH, SUBSTATE_SAVED_PAGE_LOADED);

                readUrlMappings();
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

    /** Read URL mappings from the saved page specific file */
    private void readUrlMappings() {
        new LoadSavedPageUrlMapTask(title) {
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
        curEntry = new HistoryEntry(title, HistoryEntry.SOURCE_HISTORY);
        setState(STATE_NO_FETCH);
        performActionForState(state);
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

    public boolean handleBackPressed() {
        if (tocHandler != null && tocHandler.isVisible()) {
            tocHandler.hide();
            return true;
        }
        if (closeFindInPage()) {
            return true;
        }
        return false;
    }
}
