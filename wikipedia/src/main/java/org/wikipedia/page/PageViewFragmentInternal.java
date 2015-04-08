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
import org.wikipedia.page.bottomcontent.BottomContentHandlerOld;
import org.wikipedia.page.bottomcontent.BottomContentInterface;
import org.wikipedia.page.gallery.GalleryActivity;
import org.wikipedia.page.leadimages.LeadImagesHandler;
import org.wikipedia.pageimages.PageImage;
import org.wikipedia.pageimages.PageImagesTask;
import org.wikipedia.savedpages.ImageUrlMap;
import org.wikipedia.savedpages.LoadSavedPageTask;
import org.wikipedia.savedpages.LoadSavedPageUrlMapTask;
import org.wikipedia.savedpages.SavePageTask;
import org.wikipedia.search.SearchBarHideHandler;
import org.wikipedia.util.NetworkUtils;
import org.wikipedia.views.DisableableDrawerLayout;
import org.wikipedia.views.ObservableWebView;
import org.wikipedia.views.SwipeRefreshLayoutWithScroll;
import org.mediawiki.api.json.Api;
import org.mediawiki.api.json.ApiException;
import org.mediawiki.api.json.ApiResult;
import org.mediawiki.api.json.RequestBuilder;
import org.json.JSONException;
import org.json.JSONObject;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarActivity;
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


public class PageViewFragmentInternal extends Fragment implements BackPressedHandler {
    private static final String TAG = "PageViewFragment";

    public static final int STATE_NO_FETCH = 1;
    public static final int STATE_INITIAL_FETCH = 2;
    public static final int STATE_COMPLETE_FETCH = 3;

    public static final int SUBSTATE_NONE = 0;
    public static final int SUBSTATE_PAGE_SAVED = 1;
    public static final int SUBSTATE_SAVED_PAGE_LOADED = 2;

    private int state = STATE_NO_FETCH;
    private int subState = SUBSTATE_NONE;

    /**
     * List of lightweight history items to serve as the backstack for this fragment.
     * Since the list consists of Parcelable objects, it can be saved and restored from the
     * savedInstanceState of the fragment.
     */
    private ArrayList<PageBackStackItem> backStack;

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

    /**
     * Whether to write the page contents to cache as soon as it's loaded.
     */
    private boolean cacheOnComplete = true;

    /**
     * Sequence number to maintain synchronization when loading page content asynchronously
     * between the Java and Javascript layers, as well as between async tasks and the UI thread.
     */
    private int pageSequenceNum;

    private PageTitle title;
    private PageTitle titleOriginal;
    private ViewGroup imagesContainer;
    private LeadImagesHandler leadImagesHandler;
    private BottomContentInterface bottomContentHandler;
    private SearchBarHideHandler searchBarHideHandler;
    private ObservableWebView webView;
    private SwipeRefreshLayoutWithScroll refreshView;
    private View networkError; // TODO: change this later to pageError when not under active development
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

    /**
     * The y-offset position to which the page will be scrolled once it's fully loaded
     * (or loaded to the point where it can be scrolled to the correct position).
     */
    private int stagedScrollY;

    private SavedPagesFunnel savedPagesFunnel;
    private ConnectionIssueFunnel connectionIssueFunnel;

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

    public PageViewFragmentInternal() {
        backStack = new ArrayList<>();
    }

    private void displayLeadSection() {
        try {
            JSONObject marginPayload = new JSONObject();
            int margin = (int) (getResources().getDimension(R.dimen.activity_horizontal_margin)
                                / getResources().getDisplayMetrics().density);
            marginPayload.put("marginLeft", margin);
            marginPayload.put("marginRight", margin);
            bridge.sendMessage("setMargins", marginPayload);

            JSONObject leadSectionPayload = new JSONObject();
            leadSectionPayload.put("sequence", pageSequenceNum);
            leadSectionPayload.put("title", page.getDisplayTitle());
            leadSectionPayload.put("section", page.getSections().get(0).toJSON());
            leadSectionPayload
                    .put("string_page_similar_titles", getString(R.string.page_similar_titles));
            leadSectionPayload.put("string_page_issues", getString(R.string.button_page_issues));
            leadSectionPayload.put("string_table_infobox", getString(R.string.table_infobox));
            leadSectionPayload.put("string_table_other", getString(R.string.table_other));
            leadSectionPayload.put("string_table_close", getString(R.string.table_close));
            leadSectionPayload.put("string_expand_refs", getString(R.string.expand_refs));
            leadSectionPayload.put("isBeta", app.getReleaseType() != WikipediaApp.RELEASE_PROD);
            leadSectionPayload.put("siteLanguage", title.getSite().getLanguage());
            leadSectionPayload.put("isMainPage", page.getPageProperties().isMainPage());
            leadSectionPayload.put("apiLevel", Build.VERSION.SDK_INT);
            bridge.sendMessage("displayLeadSection", leadSectionPayload);

            Utils.setupDirectionality(title.getSite().getLanguage(),
                                      Locale.getDefault().getLanguage(), bridge);

            // Hide edit pencils if anon editing is disabled by remote killswitch or if this is a file page
            JSONObject miscPayload = new JSONObject();
            boolean isAnonEditingDisabled = app.getRemoteConfig().getConfig()
                                               .optBoolean("disableAnonEditing", false)
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
        ((PageActivity) getActivity()).updateProgressBar(true, true, 0);
    }

    private void displayNonLeadSection(int index) {
        ((PageActivity) getActivity()).updateProgressBar(true, false,
                                                         PageActivity.PROGRESS_BAR_MAX_VALUE / page
                                                                 .getSections().size() * index);

        try {
            JSONObject wrapper = new JSONObject();
            wrapper.put("sequence", pageSequenceNum);
            if (index < page.getSections().size()) {
                wrapper.put("section", page.getSections().get(index).toJSON());
                wrapper.put("index", index);
                if (sectionTargetFromIntent > 0 && sectionTargetFromIntent < page.getSections()
                                                                                 .size()) {
                    //if we have a section to scroll to (from our Intent):
                    wrapper.put("fragment",
                                page.getSections().get(sectionTargetFromIntent).getAnchor());
                } else if (sectionTargetFromTitle != null) {
                    //if we have a section to scroll to (from our PageTitle):
                    wrapper.put("fragment", sectionTargetFromTitle);
                }
            } else {
                wrapper.put("noMore", true);
            }
            //give it our expected scroll position, in case we need the page to be pre-scrolled upon loading.
            wrapper.put("scrollY",
                        (int) (stagedScrollY / getResources().getDisplayMetrics().density));
            bridge.sendMessage("displaySection", wrapper);
        } catch (JSONException e) {
            //nope
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             final Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_page, container, false);
        webView = (ObservableWebView) rootView.findViewById(R.id.page_web_view);
        networkError = rootView.findViewById(R.id.page_error);
        retryButton = rootView.findViewById(R.id.page_error_retry);
        pageDoesNotExistError = rootView.findViewById(R.id.page_does_not_exist);
        tocDrawer = (DisableableDrawerLayout) rootView.findViewById(R.id.page_toc_drawer);

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
        super.onDestroyView();
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        app = (WikipediaApp) getActivity().getApplicationContext();
        connectionIssueFunnel = new ConnectionIssueFunnel(app);

        if (savedInstanceState != null) {
            backStack = savedInstanceState.getParcelableArrayList("backStack");
        }

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
                if (!isAdded()) {
                    return;
                }
                if (referenceDialog != null && referenceDialog.isShowing()) {
                    referenceDialog.dismiss();
                }
                HistoryEntry historyEntry = new HistoryEntry(title,
                                                             HistoryEntry.SOURCE_INTERNAL_LINK);
                ((PageActivity) getActivity()).displayNewPage(title, historyEntry);
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
                return title.getSite();
            }

            @Override
            int getDialogHeight() {
                // could have scrolled up a bit but the page info links must still be visible else they couldn't have been clicked
                return webView.getHeight() + webView.getScrollY() - imagesContainer.getHeight();
            }
        };

        bridge.injectStyleBundle(
                app.getStyleLoader().getAvailableBundle(StyleLoader.BUNDLE_PAGEVIEW));

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
                        displayNewPage(titleOriginal, curEntry, true, false);
                        retryButton.setEnabled(true);
                    }
                });
            }
        });

        editHandler = new EditHandler(this, bridge);
        tocHandler = new ToCHandler(((PageActivity) getActivity()), tocDrawer, bridge);

        imagesContainer = (ViewGroup) getView().findViewById(R.id.page_images_container);
        leadImagesHandler = new LeadImagesHandler(getActivity(), this, bridge, webView,
                                                  imagesContainer);
        searchBarHideHandler = ((PageActivity) getActivity()).getSearchBarHideHandler();
        searchBarHideHandler.setScrollView(webView);

        // TODO: remove this A/B toggle when we know which one we want to keep.
        // (and when ready to release to production)
        if (BottomContentHandler.useNewBottomContent(app)) {
            bottomContentHandler = new BottomContentHandler(this, bridge, webView, linkHandler,
                                                            (ViewGroup) getView().findViewById(
                                                                    R.id.bottom_content_container));
        } else {
            bottomContentHandler = new BottomContentHandlerOld(this, bridge, webView, linkHandler,
                                                               (ViewGroup) getView().findViewById(
                                                                       R.id.bottom_content_container));
        }

        pageSequenceNum = 0;

        // if we already have pages in the backstack (whether it's from savedInstanceState, or
        // from being stored in the activity's fragment backstack), then load the topmost page
        // on the backstack.
        if (backStack.size() > 0) {
            loadPageFromBackStack();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // update the topmost entry in the backstack
        updateBackStackItem();
        outState.putParcelableArrayList("backStack", backStack);
    }

    @Override
    public void onResume() {
        super.onResume();
        ((ActionBarActivity) getActivity()).getSupportActionBar().setTitle("");
    }

    /**
     * Pop the topmost entry from the backstack.
     * Does NOT automatically load the next topmost page on the backstack.
     */
    private void popBackStack() {
        if (backStack.size() == 0) {
            return;
        }
        backStack.remove(backStack.size() - 1);
    }

    /**
     * Push the current page title onto the backstack.
     */
    private void pushBackStack() {
        PageBackStackItem item = new PageBackStackItem(titleOriginal, curEntry);
        backStack.add(item);
    }

    /**
     * Update the current topmost backstack item, based on the currently displayed page.
     * (Things like the last y-offset position should be updated here)
     * Should be done right before loading a new page.
     */
    private void updateBackStackItem() {
        if (backStack.size() == 0) {
            return;
        }
        PageBackStackItem item = backStack.get(backStack.size() - 1);
        item.setScrollY(webView.getScrollY());
    }

    private void loadPageFromBackStack() {
        if (backStack.size() == 0) {
            return;
        }
        PageBackStackItem item = backStack.get(backStack.size() - 1);
        // display the page based on the backstack item, stage the scrollY position based on
        // the backstack item.
        displayNewPage(item.getTitle(), item.getHistoryEntry(), true, false, item.getScrollY());
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
        if (pushBackStack) {
            // update the topmost entry in the backstack, before we start overwriting things.
            updateBackStackItem();
        }

        networkError.setVisibility(View.GONE);

        state = STATE_NO_FETCH;
        subState = SUBSTATE_NONE;

        this.title = title;
        this.curEntry = entry;
        titleOriginal = title;
        savedPagesFunnel = app.getFunnelManager().getSavedPagesFunnel(title.getSite());

        if (pushBackStack) {
            pushBackStack();
        }

        // increment our sequence number, so that any async tasks that depend on the sequence
        // will invalidate themselves upon completion.
        pageSequenceNum++;

        // kick off an event to the WebView that will cause it to clear its contents,
        // and then report back to us when the clearing is complete, so that we can synchronize
        // the transitions of our native components to the new page content.
        // The callback event from the WebView will then call the loadPageOnWebViewReady()
        // function, which will continue the loading process.
        ((PageActivity) getActivity()).updateProgressBar(true, true, 0);
        try {
            JSONObject wrapper = new JSONObject();
            // whatever we pass to this event will be passed back to us by the WebView!
            wrapper.put("sequence", pageSequenceNum);
            wrapper.put("tryFromCache", tryFromCache);
            wrapper.put("stagedScrollY", stagedScrollY);
            bridge.sendMessage("beginNewPage", wrapper);
        } catch (JSONException e) {
            //nope
        }
    }

    private void loadPageOnWebViewReady(boolean tryFromCache) {
        // stage any section-specific link target from the title, since the title may be
        // replaced (normalized)
        sectionTargetFromTitle = title.getFragment();

        Utils.setupDirectionality(title.getSite().getLanguage(), Locale.getDefault().getLanguage(),
                                  bridge);

        // hide the native top and bottom components...
        leadImagesHandler.hide();
        bottomContentHandler.hide();
        bottomContentHandler.setTitle(title);

        if (curEntry.getSource() == HistoryEntry.SOURCE_SAVED_PAGE) {
            state = STATE_NO_FETCH;
            loadSavedPage();
        } else if (tryFromCache) {
            //is this page in cache??
            app.getPageCache()
               .get(titleOriginal, pageSequenceNum, new PageCache.CacheGetListener() {
                   @Override
                   public void onGetComplete(Page page, int sequence) {
                       if (sequence != pageSequenceNum) {
                           return;
                       }
                       if (page != null) {
                           Log.d(TAG, "Using page from cache: " + titleOriginal.getDisplayText());
                           PageViewFragmentInternal.this.page = page;
                           PageViewFragmentInternal.this.title = page.getTitle();
                           // Save history entry...
                           new SaveHistoryTask(curEntry).execute();
                           // don't re-cache the page after loading.
                           cacheOnComplete = false;
                           state = STATE_COMPLETE_FETCH;
                           setState(state);
                           performActionForState(state);
                       } else {
                           // page isn't in cache, so fetch it from the network...
                           loadPageFromNetwork();
                       }
                   }

                   @Override
                   public void onGetError(Throwable e, int sequence) {
                       Log.e(TAG, "Failed to get page from cache.", e);
                       if (sequence != pageSequenceNum) {
                           return;
                       }
                       // something failed when loading it from cache, so fetch it from network...
                       loadPageFromNetwork();
                   }
               });
        } else {
            loadPageFromNetwork();
        }
    }

    private void loadPageFromNetwork() {
        state = STATE_NO_FETCH;
        // and make sure to write it to cache when it's loaded.
        cacheOnComplete = true;
        setState(state);
        performActionForState(state);
    }

    private boolean isFirstPage() {
        return backStack.size() <= 1 && !webView.canGoBack();
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
     * Update the WebView's base font size, based on the specified font size from the app preferences.
     */
    public void updateFontSize() {
        webView.getSettings().setDefaultFontSize((int) app.getFontSize(getActivity().getWindow()));
    }

    private void setupMessageHandlers() {
        bridge.addListener("onBeginNewPage", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                if (!isAdded()) {
                    return;
                }
                try {
                    if (messagePayload.getInt("sequence") != pageSequenceNum) {
                        return;
                    }
                    stagedScrollY = messagePayload.getInt("stagedScrollY");
                    loadPageOnWebViewReady(messagePayload.getBoolean("tryFromCache"));
                } catch (JSONException e) {
                    //nope
                }
            }
        });
        bridge.addListener("requestSection", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                if (!isAdded()) {
                    return;
                }
                try {
                    if (messagePayload.getInt("sequence") != pageSequenceNum) {
                        return;
                    }
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
                try {
                    if (messagePayload.getInt("sequence") != pageSequenceNum) {
                        return;
                    }
                } catch (JSONException e) {
                    // nope
                }
                // Do any other stuff that should happen upon page load completion...
                ((PageActivity) getActivity()).updateProgressBar(false, true, 0);

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
        bridge.addListener("mediaClicked", new CommunicationBridge.JSEventListener() {
            @Override
            public void onMessage(String messageType, JSONObject messagePayload) {
                try {
                    String href = URLDecoder.decode(messagePayload.getString("href"), "UTF-8");
                    showImageGallery(new PageTitle(href, title.getSite()));
                } catch (JSONException e) {
                    //nope
                } catch (UnsupportedEncodingException e) {
                    //nope
                }
            }
        });
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PageActivity.ACTIVITY_REQUEST_EDIT_SECTION
            && resultCode == EditHandler.RESULT_REFRESH_PAGE) {
            //Retrieve section ID from intent, and find correct section, so where know where to scroll to
            sectionTargetFromIntent = data.getIntExtra(EditSectionActivity.EXTRA_SECTION_ID, 0);
            //reset our scroll offset, since we have a section scroll target
            stagedScrollY = 0;

            // and reload the page...
            displayNewPage(titleOriginal, curEntry, false, false);
        }
    }

    private void performActionForState(int forState) {
        if (!isAdded()) {
            return;
        }
        switch (forState) {
            case STATE_NO_FETCH:
                ((PageActivity) getActivity()).updateProgressBar(true, true, 0);
                // hide the lead image...
                leadImagesHandler.hide();
                bottomContentHandler.hide();
                ((PageActivity) getActivity()).getSearchBarHideHandler().setFadeEnabled(false);
                new LeadSectionFetchTask(pageSequenceNum).execute();
                break;
            case STATE_INITIAL_FETCH:
                new RestSectionsFetchTask(pageSequenceNum).execute();
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
            tocHandler.setupToC(page, title.getSite(), isFirstPage());

            //add the page to cache!
            if (cacheOnComplete) {
                app.getPageCache().put(titleOriginal, page, new PageCache.CachePutListener() {
                    @Override
                    public void onPutComplete() {
                    }

                    @Override
                    public void onPutError(Throwable e) {
                        Log.e(TAG, "Failed to add page to cache.", e);
                    }
                });
            }
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

        switch (state) {
            case STATE_NO_FETCH:
            case STATE_INITIAL_FETCH:
                savePageMenu.setEnabled(false);
                shareMenu.setEnabled(false);
                otherLangMenu.setEnabled(false);
                findInPageMenu.setEnabled(false);
                themeChooserMenu.setEnabled(false);
                break;
            case STATE_COMPLETE_FETCH:
                savePageMenu.setEnabled(true);
                shareMenu.setEnabled(true);
                otherLangMenu.setEnabled(true);
                findInPageMenu.setEnabled(true);
                themeChooserMenu.setEnabled(true);
                if (subState == SUBSTATE_PAGE_SAVED) {
                    savePageMenu.setEnabled(false);
                    savePageMenu.setTitle(WikipediaApp.getInstance().getString(R.string.menu_page_saved));
                } else if (subState == SUBSTATE_SAVED_PAGE_LOADED) {
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
                ((PageActivity) getActivity()).share();
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
        galleryIntent.putExtra(GalleryActivity.EXTRA_PAGETITLE, titleOriginal);
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
        public LeadSectionFetchTask(int sequenceNum) {
            super(getActivity(), title, "0");
            this.sequenceNum = sequenceNum;
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

        private final int sequenceNum;
        private PageProperties pageProperties;

        @Override
        public List<Section> processResult(ApiResult result) throws Throwable {
            if (sequenceNum != pageSequenceNum) {
                return super.processResult(result);
            }
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
            if (!isAdded() || sequenceNum != pageSequenceNum) {
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
            commonSectionFetchOnCatch(caught, sequenceNum);
        }
    }

    private class RestSectionsFetchTask extends SectionsFetchTask {
        private final int sequenceNum;

        public RestSectionsFetchTask(int sequenceNum) {
            super(getActivity(), title, "1-");
            this.sequenceNum = sequenceNum;
        }

        @Override
        public void onFinish(List<Section> result) {
            if (!isAdded() || sequenceNum != pageSequenceNum) {
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
            commonSectionFetchOnCatch(caught, sequenceNum);
        }
    }

    private void commonSectionFetchOnCatch(Throwable caught, int sequenceNum) {
        if (!isAdded() || sequenceNum != pageSequenceNum) {
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
        // TODO: Check for the source of the error and have different things turn up
        // TODO: Change the name of this function to showPageError when the file is not under active development
        TextView errorMessage = (TextView) networkError.findViewById(R.id.page_error_message);

        if (!NetworkUtils.isNetworkConnectionPresent(app)) {
            errorMessage.setText(R.string.error_network_error);
        } else {
            errorMessage.setText(R.string.generic_page_error);
        }

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
        curEntry = new HistoryEntry(title, HistoryEntry.SOURCE_HISTORY);
        displayNewPage(title, curEntry, false, false);
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
        if (backStack.size() > 1) {
            popBackStack();
            loadPageFromBackStack();
            return true;
        }
        return false;
    }
}
