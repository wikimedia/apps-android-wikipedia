package org.wikipedia.page;

import org.wikipedia.BackPressedHandler;
import org.wikipedia.NightModeHandler;
import org.wikipedia.PageTitle;
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
import org.wikipedia.page.bottomcontent.BottomContentHandler;
import org.wikipedia.page.gallery.GalleryActivity;
import org.wikipedia.page.leadimages.LeadImagesHandler;
import org.wikipedia.pageimages.PageImage;
import org.wikipedia.pageimages.PageImagesTask;
import org.wikipedia.savedpages.ImageUrlMap;
import org.wikipedia.savedpages.LoadSavedPageTask;
import org.wikipedia.savedpages.LoadSavedPageUrlMapTask;
import org.wikipedia.savedpages.SavePageTask;
import org.wikipedia.search.SearchBarHideHandler;
import org.wikipedia.views.DisableableDrawerLayout;
import org.wikipedia.views.ObservableWebView;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiException;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import org.wikipedia.views.SwipeRefreshLayoutWithScroll;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
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
import javax.net.ssl.SSLException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
public class PageViewFragmentInternal implements BackPressedHandler {
    private static final String TAG = "PageViewFragmentInternal";

    public static final int STATE_NO_FETCH = 1;
    public static final int STATE_INITIAL_FETCH = 2;
    public static final int STATE_COMPLETE_FETCH = 3;

    public static final int SUBSTATE_NONE = 0;
    public static final int SUBSTATE_PAGE_SAVED = 1;
    public static final int SUBSTATE_SAVED_PAGE_LOADED = 2;

    private int state = STATE_NO_FETCH;
    private int subState = SUBSTATE_NONE;

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

    private PageTitle title;
    private PageTitle titleOriginal;
    private ViewGroup imagesContainer;
    private LeadImagesHandler leadImagesHandler;
    private BottomContentHandler bottomContentHandler;
    private SearchBarHideHandler searchBarHideHandler;
    private ObservableWebView webView;
    private SwipeRefreshLayoutWithScroll refreshView;
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
        try {
            JSONObject marginPayload = new JSONObject();
            int margin = (int)(getResources().getDimension(R.dimen.activity_horizontal_margin)
                    / getResources().getDisplayMetrics().density);
            marginPayload.put("marginLeft", margin);
            marginPayload.put("marginRight", margin);
            bridge.sendMessage("setMargins", marginPayload);

            JSONObject leadSectionPayload = new JSONObject();
            leadSectionPayload.put("title", page.getDisplayTitle());
            leadSectionPayload.put("section", page.getSections().get(0).toJSON());
            leadSectionPayload.put("string_page_similar_titles", getString(R.string.page_similar_titles));
            leadSectionPayload.put("string_page_issues", getString(R.string.button_page_issues));
            leadSectionPayload.put("string_table_infobox", getString(R.string.table_infobox));
            leadSectionPayload.put("string_table_other", getString(R.string.table_other));
            leadSectionPayload.put("string_table_close", getString(R.string.table_close));
            leadSectionPayload.put("string_expand_refs", getString(R.string.expand_refs));
            leadSectionPayload.put("isBeta", app.getReleaseType() != WikipediaApp.RELEASE_PROD);
            leadSectionPayload.put("isMainPage", page.getPageProperties().isMainPage());
            leadSectionPayload.put("apiLevel", Build.VERSION.SDK_INT);
            bridge.sendMessage("displayLeadSection", leadSectionPayload);

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

        if (webView.getVisibility() != View.VISIBLE) {
            webView.setVisibility(View.VISIBLE);
        }

        refreshView.setRefreshing(false);
        getActivity().updateProgressBar(true, true, 0);
    }

    private void displayNonLeadSection(int index) {
        getActivity().updateProgressBar(true, false, PageActivity.PROGRESS_BAR_MAX_VALUE
                / page.getSections().size() * index);

        try {
            JSONObject wrapper = new JSONObject();
            if (index < page.getSections().size()) {
                wrapper.put("section", page.getSections().get(index).toJSON());
                wrapper.put("index", index);
                if (sectionTargetFromIntent > 0 && sectionTargetFromIntent < page.getSections().size()) {
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

        refreshView = (SwipeRefreshLayoutWithScroll) rootView.findViewById(R.id.page_refresh_container);
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
                if (state != STATE_COMPLETE_FETCH) {
                    refreshView.setRefreshing(false);
                    return;
                }
                if (curEntry.getSource() == HistoryEntry.SOURCE_SAVED_PAGE) {
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

        savedPagesFunnel = app.getFunnelManager().getSavedPagesFunnel(title.getSite());

        connectionIssueFunnel = new ConnectionIssueFunnel(app);

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
                if (!isAdded()) {
                    return;
                }
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

        new PageInfoHandler(getActivity(), bridge, title.getSite()) {
            @Override
            int getDialogHeight() {
                // could have scrolled up a bit but the page info links must still be visible else they couldn't have been clicked
                return webView.getHeight() + webView.getScrollY() - imagesContainer.getHeight();
            }
        };

        bridge.injectStyleBundle(app.getStyleLoader().getAvailableBundle(StyleLoader.BUNDLE_PAGEVIEW));

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
                        performActionForState(state);
                        retryButton.setEnabled(true);
                    }
                });
            }
        });

        editHandler = new EditHandler(parentFragment, bridge);

        imagesContainer = (ViewGroup) parentFragment.getView().findViewById(R.id.page_images_container);
        leadImagesHandler = new LeadImagesHandler(getActivity(), this, bridge, webView, imagesContainer);
        searchBarHideHandler = getActivity().getSearchBarHideHandler();
        searchBarHideHandler.setScrollView(webView);

        bottomContentHandler = new BottomContentHandler(this, bridge,
                webView, linkHandler,
                (ViewGroup) parentFragment.getView().findViewById(R.id.bottom_content_container),
                title);

        //is this page in cache??
        if (app.getPageCache().has(titleOriginal)) {
            Log.d(TAG, "Using page from cache: " + titleOriginal.getDisplayText());
            page = app.getPageCache().get(titleOriginal);
            title = page.getTitle();
            state = STATE_COMPLETE_FETCH;
        }

        if (tocHandler == null) {
            tocHandler = new ToCHandler(getActivity(),
                    tocDrawer,
                    bridge,
                    title.getSite(),
                    isFirstPage());
        }

        setState(state);
        performActionForState(state);
    }

    private boolean isFirstPage() {
        return parentFragment.getFragmentManager().getBackStackEntryCount() == 0
                && !webView.canGoBack();
    }

    public Bitmap getLeadImageBitmap() {
        return leadImagesHandler.getLeadImageBitmap();
    }

    public int getImageBaseYOffset() {
        return leadImagesHandler.getImageBaseYOffset();
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

                // trigger layout of the bottom content
                // Check to see if the page title has changed (e.g. due to following a redirect),
                // because if it has then the handler needs the new title to make sure it doesn't
                // accidentally display the current article as a "read more" suggestion
                if (!bottomContentHandler.getTitle().equals(title)) {
                    bottomContentHandler.setTitle(title);
                }
                bottomContentHandler.beginLayout();
            }
        });
        bridge.addListener("imageClicked", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                try {
                    String href = URLDecoder.decode(messagePayload.getString("href"), "UTF-8");
                    if (href.startsWith("/wiki/")) {
                        PageTitle imageTitle = title.getSite().titleForInternalLink(href);
                        showImageGallery(imageTitle);
                    } else {
                        linkHandler.onUrlClick(href);
                    }
                } catch (JSONException e) {
                    //nope
                } catch (UnsupportedEncodingException e) {
                    //nope
                }
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

            hidePageContent();

            // and reload the page...
            setState(STATE_NO_FETCH);
            performActionForState(state);
        }
    }

    private void performActionForState(int forState) {
        if (!isAdded()) {
            return;
        }
        switch (forState) {
            case STATE_NO_FETCH:
                getActivity().updateProgressBar(true, true, 0);
                bridge.sendMessage("clearContents", new JSONObject());

                // hide the lead image...
                leadImagesHandler.hide();
                getActivity().getSearchBarHideHandler().setFadeEnabled(false);

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
                // kick off the lead image layout
                leadImagesHandler.beginLayout(new LeadImagesHandler.OnLeadImageLayoutListener() {
                    @Override
                    public void onLayoutComplete() {
                        if (!isAdded()) {
                            return;
                        }
                        searchBarHideHandler.setFadeEnabled(leadImagesHandler.isLeadImageEnabled());
                        // when the lead image layout is complete, load the lead section and
                        // the other sections into the webview.
                        displayLeadSection();
                        displayNonLeadSection(1);
                    }
                });
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
        if (!isAdded()) {
            return;
        }
        this.state = state;
        this.subState = subState;
        getActivity().supportInvalidateOptionsMenu();

        // FIXME: Move this out into a PageComplete event of sorts
        if (state == STATE_COMPLETE_FETCH) {
            tocHandler.setupToC(page);

            //add the page to cache!
            app.getPageCache().put(titleOriginal, page);
            if (!titleOriginal.equals(title)) {
                app.getPageCache().put(title, page);
            }
        }
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        app.adjustDrawableToTheme(menu.findItem(R.id.menu_toc).getIcon());
    }

    public void onPrepareOptionsMenu(Menu menu) {
        MenuItem savePageMenu = menu.findItem(R.id.menu_save_page);
        if (savePageMenu == null) {
            return;
        }

        MenuItem shareMenu = menu.findItem(R.id.menu_share_page);
        MenuItem otherLangMenu = menu.findItem(R.id.menu_other_languages);
        MenuItem findInPageMenu = menu.findItem(R.id.menu_find_in_page);
        MenuItem themeChooserMenu = menu.findItem(R.id.menu_themechooser);

        switch (state) {
            case PageViewFragmentInternal.STATE_NO_FETCH:
            case PageViewFragmentInternal.STATE_INITIAL_FETCH:
                savePageMenu.setEnabled(false);
                shareMenu.setEnabled(false);
                otherLangMenu.setEnabled(false);
                findInPageMenu.setEnabled(false);
                themeChooserMenu.setEnabled(false);
                break;
            case PageViewFragmentInternal.STATE_COMPLETE_FETCH:
                savePageMenu.setEnabled(true);
                shareMenu.setEnabled(true);
                otherLangMenu.setEnabled(true);
                findInPageMenu.setEnabled(true);
                themeChooserMenu.setEnabled(true);
                if (subState == PageViewFragmentInternal.SUBSTATE_PAGE_SAVED) {
                    savePageMenu.setEnabled(false);
                    savePageMenu.setTitle(WikipediaApp.getInstance().getString(R.string.menu_page_saved));
                } else if (subState == PageViewFragmentInternal.SUBSTATE_SAVED_PAGE_LOADED) {
                    savePageMenu.setTitle(WikipediaApp.getInstance().getString(R.string.menu_refresh_saved_page));
                } else {
                    savePageMenu.setTitle(WikipediaApp.getInstance().getString(R.string.menu_save_page));
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
                getActivity().share(title);
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
        galleryIntent.putExtra(GalleryActivity.EXTRA_PAGETITLE, title);
        getActivity().startActivityForResult(galleryIntent, PageActivity.ACTIVITY_REQUEST_GALLERY);
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
            // Instead of "upserting" the history entry, we'll delete and re-persist it.
            // This is because upserting will update all previous instances of the history entry,
            // and won't collapse them into a single entry at the top. Deleting it will ensure
            // that all previous instances will be deleted, and then only the most recent instance
            // will be placed at the top.
            app.getPersister(HistoryEntry.class).delete(entry);
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
            builder.param("prop", builder.getParams().get("prop") + "|thumb|image|id|revision|description|"
                    + Page.API_REQUEST_PROPS);
            builder.param("thumbsize", Integer.toString((int)(getResources().getDimension(R.dimen.leadImageWidth)
                    / getResources().getDisplayMetrics().density)));
            builder.param("appInstallID", app.getAppInstallID());
            return builder;
        }

        private PageProperties pageProperties;

        @Override
        public List<Section> processResult(ApiResult result) throws Throwable {
            JSONObject mobileView = result.asObject().optJSONObject("mobileview");
            if (mobileView != null) {
                pageProperties = new PageProperties(mobileView);
                if (mobileView.has("redirected")) {
                    // Handle redirects properly.
                    title = new PageTitle(mobileView.optString("redirected"), title.getSite(), title.getThumbUrl());
                } else if (mobileView.has("normalizedtitle")) {
                    // We care about the normalized title only if we were not redirected
                    title = new PageTitle(mobileView.optString("normalizedtitle"), title.getSite(), title.getThumbUrl());
                }
                if (mobileView.has("description")) {
                    title.setDescription(Utils.capitalizeFirstChar(mobileView.getString("description")));
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

            // kick off the lead image layout
            leadImagesHandler.beginLayout(new LeadImagesHandler.OnLeadImageLayoutListener() {
                @Override
                public void onLayoutComplete() {
                    searchBarHideHandler.setFadeEnabled(leadImagesHandler.isLeadImageEnabled());
                    // when the lead image is laid out, display the lead section in the webview,
                    // and start loading the rest of the sections.
                    displayLeadSection();
                    setState(STATE_INITIAL_FETCH);
                    performActionForState(state);
                }
            });

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
        // in any case, make sure the TOC drawer is closed
        tocDrawer.closeDrawers();
        getActivity().updateProgressBar(false, true, 0);
        refreshView.setRefreshing(false);

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

    /**
     * Convenience method for hiding all the content of a page.
     */
    private void hidePageContent() {
        leadImagesHandler.hide();
        searchBarHideHandler.setFadeEnabled(false);
        bottomContentHandler.hide();
        webView.setVisibility(View.INVISIBLE);
    }

    private void showNetworkError() {
        // Check for the source of the error and have different things turn up
        hidePageContent();
        ViewAnimations.fadeIn(networkError);
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

                // kick off the lead image layout
                leadImagesHandler.beginLayout(new LeadImagesHandler.OnLeadImageLayoutListener() {
                    @Override
                    public void onLayoutComplete() {
                        if (!isAdded()) {
                            return;
                        }
                        searchBarHideHandler.setFadeEnabled(leadImagesHandler.isLeadImageEnabled());
                        // when the lead image is laid out, load the lead section and the rest
                        // of the sections into the webview.
                        displayLeadSection();
                        displayNonLeadSection(1);
                        setState(STATE_COMPLETE_FETCH, SUBSTATE_SAVED_PAGE_LOADED);
                        // rewrite the image URLs in the webview, so that they're loaded from
                        // local storage.
                        readUrlMappings();
                    }
                });
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
        hidePageContent();
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

    public boolean onBackPressed() {
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
