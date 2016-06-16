package org.wikipedia.page.gallery;

import org.wikipedia.activity.ActivityUtil;
import org.wikipedia.MainActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.R;
import org.wikipedia.Site;
import org.wikipedia.activity.ThemedActionBarActivity;
import org.wikipedia.ViewAnimations;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.GalleryFunnel;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.LinkMovementMethodExt;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageCache;
import org.wikipedia.theme.Theme;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.GradientUtil;
import org.wikipedia.views.ViewUtil;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.util.HashMap;
import java.util.Map;

import static org.wikipedia.util.UriUtil.resolveProtocolRelativeUrl;
import static org.wikipedia.util.UriUtil.handleExternalLink;
import static org.wikipedia.util.StringUtil.trim;

public class GalleryActivity extends ThemedActionBarActivity {
    private static final String TAG = "GalleryActivity";
    public static final int ACTIVITY_RESULT_FILEPAGE_SELECT = 1;

    public static final String EXTRA_PAGETITLE = "pageTitle";
    public static final String EXTRA_IMAGETITLE = "imageTitle";
    public static final String EXTRA_SOURCE = "source";

    private WikipediaApp app;
    private PageTitle pageTitle;
    private Page page;
    private boolean cacheOnLoad;

    private GalleryFunnel funnel;
    public GalleryFunnel getFunnel() {
        return funnel;
    }

    private ViewPager galleryPager;
    private GalleryItemAdapter galleryAdapter;
    private MediaDownloadReceiver downloadReceiver;

    /**
     * Cache that stores GalleryItem information for each corresponding media item in
     * our gallery collection.
     */
    private Map<PageTitle, GalleryItem> galleryCache;
    public Map<PageTitle, GalleryItem> getGalleryCache() {
        return galleryCache;
    }

    /**
     * If we have an intent that tells us a specific image to jump to within the gallery,
     * then this will be non-null.
     */
    private PageTitle initialImageTitle;

    /**
     * If we come back from savedInstanceState, then this will be the previous pager position.
     */
    private int initialImageIndex = -1;

    private ViewGroup toolbarContainer;
    private ViewGroup infoContainer;
    private ProgressBar progressBar;
    private TextView descriptionText;
    private ImageView licenseIcon;
    private TextView creditText;
    private boolean controlsShowing = true;

    private View.OnClickListener licenseShortClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (v.getContentDescription() == null) {
                return;
            }
            FeedbackUtil.showMessageAsPlainText((Activity) v.getContext(), v.getContentDescription());
        }
    };

    private View.OnLongClickListener licenseLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View v) {
            String licenseUrl = (String) v.getTag();
            if (!TextUtils.isEmpty(licenseUrl)) {
                handleExternalLink(GalleryActivity.this, Uri.parse(resolveProtocolRelativeUrl(licenseUrl)));
            }
            return true;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // force the theme to dark...
        setTheme(Theme.DARK.getResourceId());
        app = (WikipediaApp)getApplicationContext();
        downloadReceiver = new MediaDownloadReceiver(this);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_gallery);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.gallery_toolbar);
        // give it a gradient background
        ViewUtil.setBackgroundDrawable(toolbar, GradientUtil.getCubicGradient(
                getResources().getColor(R.color.lead_gradient_start), Gravity.TOP));
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");

        toolbarContainer = (ViewGroup) findViewById(R.id.gallery_toolbar_container);
        infoContainer = (ViewGroup) findViewById(R.id.gallery_info_container);
        // give it a gradient background
        ViewUtil.setBackgroundDrawable(infoContainer, GradientUtil.getCubicGradient(
                getResources().getColor(R.color.lead_gradient_start), Gravity.BOTTOM));

        progressBar = (ProgressBar) findViewById(R.id.gallery_progressbar);

        descriptionText = (TextView) findViewById(R.id.gallery_description_text);
        descriptionText.setShadowLayer(2, 1, 1, getResources().getColor(R.color.lead_text_shadow));
        descriptionText.setMovementMethod(linkMovementMethod);

        licenseIcon = (ImageView) findViewById(R.id.gallery_license_icon);
        licenseIcon.setOnClickListener(licenseShortClickListener);
        licenseIcon.setOnLongClickListener(licenseLongClickListener);

        creditText = (TextView) findViewById(R.id.gallery_credit_text);
        creditText.setShadowLayer(2, 1, 1, getResources().getColor(R.color.lead_text_shadow));

        pageTitle = getIntent().getParcelableExtra(EXTRA_PAGETITLE);
        initialImageTitle = getIntent().getParcelableExtra(EXTRA_IMAGETITLE);

        galleryCache = new HashMap<>();
        galleryPager = (ViewPager) findViewById(R.id.gallery_item_pager);
        galleryAdapter = new GalleryItemAdapter(this);
        galleryPager.setAdapter(galleryAdapter);
        galleryPager.setOnPageChangeListener(new GalleryPageChangeListener());

        funnel = new GalleryFunnel(app, pageTitle.getSite(),
                                   getIntent().getIntExtra(EXTRA_SOURCE, 0));

        if (savedInstanceState == null) {
            if (initialImageTitle != null) {
                funnel.logGalleryOpen(pageTitle, initialImageTitle.getDisplayText());
            }
        } else {
            controlsShowing = savedInstanceState.getBoolean("controlsShowing");
            initialImageIndex = savedInstanceState.getInt("pagerIndex");
            // if we have a savedInstanceState, then the initial index overrides
            // the initial Title from our intent.
            initialImageTitle = null;
            if (getSupportFragmentManager().getFragments() != null) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                for (Fragment f : getSupportFragmentManager().getFragments()) {
                    if (f instanceof GalleryItemFragment) {
                        ft.remove(f);
                    }
                }
                ft.commitAllowingStateLoss();
            }
        }
        toolbarContainer.post(new Runnable() {
            @Override
            public void run() {
                setControlsShowing(controlsShowing);
            }
        });

        updateProgressBar(false, true, 0);

        // find our Page in the page cache...
        app.getPageCache().get(pageTitle, 0, new PageCache.CacheGetListener() {
            @Override
            public void onGetComplete(Page page, int sequence) {
                GalleryActivity.this.page = page;
                if (page != null && page.getGalleryCollection() != null
                    && page.getGalleryCollection().getItemList().size() > 0) {
                    applyGalleryCollection(page.getGalleryCollection());
                    cacheOnLoad = false;
                } else {
                    // fetch the gallery from the network...
                    fetchGalleryCollection();
                    cacheOnLoad = true;
                }
            }

            @Override
            public void onGetError(Throwable e, int sequence) {
                Log.e(TAG, "Failed to get page from cache.", e);
                fetchGalleryCollection();
                cacheOnLoad = true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(downloadReceiver);
    }

    /**
     * Launch the image gallery activity, and start with the provided image.
     * @param imageTitle Image with which to begin the gallery.
     */
    public static void showGallery(Activity activity, PageTitle pageTitle, PageTitle imageTitle,
                                   int source) {
        Intent galleryIntent = new Intent();
        galleryIntent.setClass(activity, GalleryActivity.class);
        galleryIntent.putExtra(EXTRA_IMAGETITLE, imageTitle);
        galleryIntent.putExtra(EXTRA_PAGETITLE, pageTitle);
        galleryIntent.putExtra(EXTRA_SOURCE, source);
        activity.startActivityForResult(galleryIntent, MainActivity.ACTIVITY_REQUEST_GALLERY);
    }

    private class GalleryPageChangeListener implements ViewPager.OnPageChangeListener {
        private int currentPosition = -1;
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }
        @Override
        public void onPageSelected(int position) {
            // the pager has settled on a new position
            layoutGalleryDescription();
            galleryAdapter.notifyFragments(position);
            if (currentPosition != -1 && getCurrentItem() != null) {
                if (position < currentPosition) {
                    funnel.logGallerySwipeLeft(pageTitle, getCurrentItem().getName());
                } else if (position > currentPosition) {
                    funnel.logGallerySwipeRight(pageTitle, getCurrentItem().getName());
                }
            }
            currentPosition = position;
        }
        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == ViewPager.SCROLL_STATE_IDLE) {
                galleryAdapter.purgeFragments(false);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("controlsShowing", controlsShowing);
        outState.putInt("pagerIndex", galleryPager.getCurrentItem());
    }

    private void updateProgressBar(boolean visible, boolean indeterminate, int value) {
        progressBar.setIndeterminate(indeterminate);
        if (!indeterminate) {
            progressBar.setProgress(value);
        }
        progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return ActivityUtil.defaultOnOptionsItemSelected(this, item)
                || super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // log the "gallery close" event only upon explicit closing of the activity
        // (back button, or home-as-up button in the toolbar)
        if (getCurrentItem() != null) {
            funnel.logGalleryClose(pageTitle, getCurrentItem().getName());
        }
        super.onBackPressed();
    }

    public MediaDownloadReceiver getDownloadReceiver() {
        return downloadReceiver;
    }

    /**
     * Show or hide all the UI controls in this activity (slide them out or in).
     * @param showing Whether to show or hide the controls.
     */
    private void setControlsShowing(boolean showing) {
        controlsShowing = showing;
        if (controlsShowing) {
            ViewAnimations.ensureTranslationY(toolbarContainer, 0);
            ViewAnimations.ensureTranslationY(infoContainer, 0);
        } else {
            ViewAnimations.ensureTranslationY(toolbarContainer, -toolbarContainer.getHeight());
            ViewAnimations.ensureTranslationY(infoContainer, infoContainer.getHeight());
        }
    }

    /**
     * Toggle showing or hiding of all the UI controls.
     */
    public void toggleControls() {
        setControlsShowing(!controlsShowing);
    }

    /**
     * LinkMovementMethod for handling clicking of links in the description or metadata
     * text fields. For internal links, this activity will close, and pass the page title as
     * the result. For external links, they will be bounced out to the Browser.
     */
    private LinkMovementMethodExt linkMovementMethod =
            new LinkMovementMethodExt(new LinkMovementMethodExt.UrlHandler() {
        @Override
        public void onUrlClick(String url) {
            Log.v(TAG, "Link clicked was " + url);
            url = resolveProtocolRelativeUrl(url);
            Site site = app.getSite();
            if (url.startsWith("/wiki/")) {
                PageTitle title = site.titleForInternalLink(url);
                finishWithPageResult(title);
            } else {
                Uri uri = Uri.parse(url);
                String authority = uri.getAuthority();
                if (authority != null && Site.supportedAuthority(authority)
                    && uri.getPath().startsWith("/wiki/")) {
                    PageTitle title = site.titleForUri(uri);
                    finishWithPageResult(title);
                } else {
                    // if it's a /w/ URI, turn it into a full URI and go external
                    if (url.startsWith("/w/")) {
                        url = String.format("%1$s://%2$s", site.scheme(), site.authority()) + url;
                    }
                    handleExternalLink(GalleryActivity.this, Uri.parse(url));
                }
            }
        }
    });

    /**
     * Close this activity, with the specified PageTitle as the activity result, to be picked up
     * by the activity that originally launched us.
     * @param resultTitle PageTitle to pass as the activity result.
     */
    public void finishWithPageResult(PageTitle resultTitle) {
        HistoryEntry historyEntry = new HistoryEntry(resultTitle,
                                                     HistoryEntry.SOURCE_INTERNAL_LINK);
        Intent intent = new Intent();
        intent.setClass(GalleryActivity.this, MainActivity.class);
        intent.setAction(MainActivity.ACTION_PAGE_FOR_TITLE);
        intent.putExtra(MainActivity.EXTRA_PAGETITLE, resultTitle);
        intent.putExtra(MainActivity.EXTRA_HISTORYENTRY, historyEntry);
        setResult(ACTIVITY_RESULT_FILEPAGE_SELECT, intent);
        finish();
    }

    /**
     * Retrieve the complete list of media items for the current page.
     * When retrieved, the list will be passed to the ViewPager, and will become a
     * scrollable gallery of media.
     */
    private void fetchGalleryCollection() {
        updateProgressBar(true, true, 0);
        new GalleryCollectionFetchTask(app.getAPIForSite(pageTitle.getSite()),
                pageTitle.getSite(), pageTitle) {
            @Override
            public void onGalleryResult(GalleryCollection result) {
                updateProgressBar(false, true, 0);
                // save it to our current page, for later use
                if (cacheOnLoad && page != null) {
                    page.setGalleryCollection(result);
                    app.getPageCache().put(pageTitle, page, new PageCache.CachePutListener() {
                        @Override
                        public void onPutComplete() {
                        }

                        @Override
                        public void onPutError(Throwable e) {
                            Log.e(TAG, "Failed to add page to cache.", e);
                        }
                    });
                }
                applyGalleryCollection(result);
            }
            @Override
            public void onCatch(Throwable caught) {
                Log.e(TAG, "Failed to fetch gallery collection.", caught);
                updateProgressBar(false, true, 0);
                FeedbackUtil.showError(GalleryActivity.this, caught);
            }
        }.execute();
    }

    /**
     * Apply a complete collection of media to our scrollable gallery.
     * @param collection GalleryCollection to apply to the ViewPager.
     */
    private void applyGalleryCollection(GalleryCollection collection) {
        // remove the page transformer while we operate on the pager...
        galleryPager.setPageTransformer(false, null);
        // first, verify that the collection contains the item that the user
        // initially requested, if we have one...
        int initialImagePos = -1;
        if (initialImageTitle != null) {
            for (GalleryItem item : collection.getItemList()) {
                if (item.getName().equals(initialImageTitle.getDisplayText())) {
                    initialImagePos = collection.getItemList().indexOf(item);
                    break;
                }
            }
            if (initialImagePos == -1) {
                // the requested image is not present in the gallery collection, so
                // add it manually.
                // (this can happen if the user clicked on an SVG file, since we hide SVGs
                // by default in the gallery)
                initialImagePos = 0;
                collection.getItemList().add(initialImagePos,
                                             new GalleryItem(initialImageTitle.getDisplayText()));
            }
        }

        // pass the collection to the adapter!
        galleryAdapter.setCollection(collection);
        if (initialImagePos != -1) {
            // if we have a target image to jump to, then do it!
            galleryPager.setCurrentItem(initialImagePos, false);
        } else if (initialImageIndex >= 0
                   && initialImageIndex < galleryAdapter.getCount()) {
            // if we have a target image index to jump to, then do it!
            galleryPager.setCurrentItem(initialImageIndex, false);
        }
        galleryPager.setPageTransformer(false, new GalleryPagerTransformer());
    }

    private GalleryItem getCurrentItem() {
        if (galleryAdapter.getItem(galleryPager.getCurrentItem()) == null) {
            return null;
        }
        return ((GalleryItemFragment) galleryAdapter.getItem(galleryPager.getCurrentItem()))
                .getGalleryItem();
    }

    /**
     * Populate the description and license text fields with data from the current gallery item.
     */
    public void layoutGalleryDescription() {
        GalleryItem item = getCurrentItem();
        if (item == null) {
            infoContainer.setVisibility(View.GONE);
            return;
        }
        galleryAdapter.notifyFragments(galleryPager.getCurrentItem());

        CharSequence descriptionStr = "";
        if (item.getMetadata().containsKey("ImageDescription")) {
            descriptionStr = Html
                    .fromHtml(item.getMetadata().get("ImageDescription"));
        } else if (item.getMetadata().containsKey("ObjectName")) {
            descriptionStr = Html
                    .fromHtml(item.getMetadata().get("ObjectName"));
        }
        if (descriptionStr.length() > 0) {
            descriptionText.setText(trim(descriptionStr));
            descriptionText.setVisibility(View.VISIBLE);
        } else {
            descriptionText.setVisibility(View.GONE);
        }

        // determine which icon to display...
        licenseIcon.setImageResource(getLicenseIcon(item));
        // Set the icon's content description to the UsageTerms property.
        // (if UsageTerms is not present, then default to Fair Use)
        String usageTerms = item.getMetadata().get("UsageTerms");
        if (TextUtils.isEmpty(usageTerms)) {
            usageTerms = getString(R.string.gallery_fair_use_license);
        }
        licenseIcon.setContentDescription(usageTerms);
        // Give the license URL to the icon, to be received by the click handler (may be null).
        licenseIcon.setTag(item.getLicenseUrl());

        String creditStr = "";
        if (item.getMetadata().containsKey("Artist")) {
            creditStr = Html.fromHtml(item.getMetadata().get("Artist")).toString().trim();
        }
        // if we couldn't find a attribution string, then default to unknown
        if (TextUtils.isEmpty(creditStr)) {
            creditStr = getString(R.string.gallery_uploader_unknown);
        }
        creditText.setText(creditStr);

        infoContainer.setVisibility(View.VISIBLE);
    }

    /**
     * Return an icon (drawable resource id) that corresponds to the type of license
     * under which the specified Gallery item is provided.
     * @param item Gallery item for which to give a license icon.
     * @return Resource ID of the icon to display, or 0 if no license is available.
     */
    private static int getLicenseIcon(GalleryItem item) {
        return item.getLicense().getLicenseIcon();
    }

    /**
     * Adapter that will provide the contents for the ViewPager.
     * Each media item will be represented by a GalleryItemFragment, which will be instantiated
     * lazily, and then cached for future use.
     */
    private class GalleryItemAdapter extends FragmentPagerAdapter {
        private GalleryCollection galleryCollection;
        private SparseArray<GalleryItemFragment> fragmentArray;

        GalleryItemAdapter(ThemedActionBarActivity activity) {
            super(activity.getSupportFragmentManager());
            fragmentArray = new SparseArray<>();
        }

        public void setCollection(GalleryCollection collection) {
            galleryCollection = collection;
            notifyDataSetChanged();
        }

        public void notifyFragments(int currentPosition) {
            for (int i = 0; i < getCount(); i++) {
                if (fragmentArray.get(i) != null) {
                    fragmentArray.get(i).onUpdatePosition(i, currentPosition);
                }
            }
        }

        /**
         * Remove any active fragments to the left+1 and right+1 of the current
         * fragment, to reduce memory usage.
         */
        public void purgeFragments(boolean removeAll) {
            int position = galleryPager.getCurrentItem();
            FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
            for (int i = 0; i < getCount(); i++) {
                if (!removeAll && Math.abs(position - i) < 2) {
                    continue;
                }
                if (fragmentArray.get(i) != null) {
                    trans.remove(fragmentArray.get(i));
                    fragmentArray.remove(i);
                    fragmentArray.put(i, null);
                }
            }
            trans.commitAllowingStateLoss();
        }

        @Override
        public int getCount() {
            return galleryCollection == null ? 0 : galleryCollection.getItemList().size();
        }

        @Override
        public Fragment getItem(int position) {
            if (galleryCollection == null || galleryCollection.getItemList().size() <= position) {
                return null;
            }
            // instantiate a new fragment if it doesn't exist
            if (fragmentArray.get(position) == null) {
                fragmentArray.put(position, GalleryItemFragment
                        .newInstance(pageTitle, galleryCollection.getItemList().get(position)));
            }
            return fragmentArray.get(position);
        }
    }

}
