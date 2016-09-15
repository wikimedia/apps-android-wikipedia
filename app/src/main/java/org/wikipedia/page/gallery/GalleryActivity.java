package org.wikipedia.page.gallery;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.Site;
import org.wikipedia.ViewAnimations;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.ActivityUtil;
import org.wikipedia.activity.ThemedActionBarActivity;
import org.wikipedia.analytics.GalleryFunnel;
import org.wikipedia.feed.image.FeaturedImage;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.page.LinkMovementMethodExt;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageCache;
import org.wikipedia.page.PageTitle;
import org.wikipedia.theme.Theme;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.GradientUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.ViewUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.wikipedia.util.StringUtil.trim;
import static org.wikipedia.util.UriUtil.handleExternalLink;
import static org.wikipedia.util.UriUtil.resolveProtocolRelativeUrl;

public class GalleryActivity extends ThemedActionBarActivity {
    public static final int ACTIVITY_RESULT_FILEPAGE_SELECT = 1;

    public static final String EXTRA_PAGETITLE = "pageTitle";
    public static final String EXTRA_FILENAME = "filename";
    public static final String EXTRA_SITE = "site";
    public static final String EXTRA_SOURCE = "source";
    public static final String EXTRA_FEATURED_IMAGE = "card";

    private static final String FEED_FEATURED_IMAGE_TITLE = "FeedFeaturedImage";

    @NonNull private WikipediaApp app = WikipediaApp.getInstance();
    @Nullable private PageTitle pageTitle;
    @Nullable private Page page;
    private boolean cacheOnLoad;
    @Nullable private Site site;

    private ViewGroup toolbarContainer;
    private ViewGroup infoContainer;
    private ProgressBar progressBar;
    private TextView descriptionText;
    private ImageView licenseIcon;
    private TextView creditText;
    private boolean controlsShowing = true;

    @Nullable private GalleryFunnel funnel;
    @Nullable protected GalleryFunnel getFunnel() {
        return funnel;
    }

    /**
     * If we have an intent that tells us a specific image to jump to within the gallery,
     * then this will be non-null.
     */
    private String initialFilename;

    /**
     * If we come back from savedInstanceState, then this will be the previous pager position.
     */
    private int initialImageIndex = -1;

    private ViewPager galleryPager;
    private GalleryItemAdapter galleryAdapter;
    private MediaDownloadReceiver downloadReceiver;

    /**
     * Cache that stores GalleryItem information for each corresponding media item in
     * our gallery collection.
     */
    @Nullable private Map<PageTitle, GalleryItem> galleryCache;

    @Nullable
    public Map<PageTitle, GalleryItem> getGalleryCache() {
        return galleryCache;
    }

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

    @NonNull
    public static Intent newIntent(@NonNull Context context, @NonNull FeaturedImage image,
                                   @NonNull String filename, @NonNull Site site, int source) {
        return newIntent(context, new PageTitle(FEED_FEATURED_IMAGE_TITLE, site), filename, site,
                source).putExtra(EXTRA_FEATURED_IMAGE, GsonMarshaller.marshal(image));
    }

    @NonNull
    public static Intent newIntent(@NonNull Context context, @NonNull PageTitle pageTitle,
                                   @NonNull String filename, @NonNull Site site, int source) {
        return new Intent()
                .setClass(context, GalleryActivity.class)
                .putExtra(EXTRA_FILENAME, filename)
                .putExtra(EXTRA_SITE, site)
                .putExtra(EXTRA_PAGETITLE, pageTitle)
                .putExtra(EXTRA_SOURCE, source);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // force the theme to dark...
        setTheme(Theme.DARK.getResourceId());
        downloadReceiver = new MediaDownloadReceiver(this);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_gallery);
        initToolbar();

        toolbarContainer = (ViewGroup) findViewById(R.id.gallery_toolbar_container);
        infoContainer = (ViewGroup) findViewById(R.id.gallery_info_container);
        setBackgroundGradient(infoContainer, Gravity.BOTTOM);

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
        initialFilename = getIntent().getStringExtra(EXTRA_FILENAME);
        site = getIntent().getParcelableExtra(EXTRA_SITE);

        galleryCache = new HashMap<>();
        galleryAdapter = new GalleryItemAdapter(this);
        galleryPager = (ViewPager) findViewById(R.id.gallery_item_pager);
        galleryPager.setAdapter(galleryAdapter);
        galleryPager.setOnPageChangeListener(new GalleryPageChangeListener());

        funnel = new GalleryFunnel(app, site, getIntent().getIntExtra(EXTRA_SOURCE, 0));

        if (savedInstanceState == null) {
            if (initialFilename != null) {
                funnel.logGalleryOpen(pageTitle, initialFilename);
            }
        } else {
            controlsShowing = savedInstanceState.getBoolean("controlsShowing");
            initialImageIndex = savedInstanceState.getInt("pagerIndex");
            // if we have a savedInstanceState, then the initial index overrides
            // the initial Title from our intent.
            initialFilename = null;
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

        if (pageTitle == null) {
            throw new IllegalStateException("pageTitle should not be null");
        } else if (FEED_FEATURED_IMAGE_TITLE.equals(pageTitle.getText())) {
            FeaturedImage featuredImage = GsonUnmarshaller.unmarshal(FeaturedImage.class,
                    getIntent().getStringExtra(EXTRA_FEATURED_IMAGE));
            if (featuredImage != null) {
                loadGalleryItemFor(featuredImage);
            }
        } else {
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
                    L.e("Failed to get page from cache.", e);
                    fetchGalleryCollection();
                    cacheOnLoad = true;
                }
            });
        }
    }

    private void loadGalleryItemFor(FeaturedImage image) {
        List<GalleryItem> list = new ArrayList<>();
        list.add(new GalleryItem(image));
        applyGalleryCollection(new GalleryCollection(list));
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

    private class GalleryPageChangeListener extends ViewPager.SimpleOnPageChangeListener {
        private int currentPosition = -1;
        @Override
        public void onPageSelected(int position) {
            // the pager has settled on a new position
            layOutGalleryDescription();
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
            L.v("Link clicked was " + url);
            url = resolveProtocolRelativeUrl(url);
            Site appSite = app.getSite();
            if (url.startsWith("/wiki/")) {
                PageTitle title = appSite.titleForInternalLink(url);
                finishWithPageResult(title);
            } else {
                Uri uri = Uri.parse(url);
                String authority = uri.getAuthority();
                if (authority != null && Site.supportedAuthority(authority)
                    && uri.getPath().startsWith("/wiki/")) {
                    PageTitle title = appSite.titleForUri(uri);
                    finishWithPageResult(title);
                } else {
                    // if it's a /w/ URI, turn it into a full URI and go external
                    if (url.startsWith("/w/")) {
                        url = String.format("%1$s://%2$s", appSite.scheme(), appSite.authority()) + url;
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
        Intent intent = PageActivity.newIntent(GalleryActivity.this, historyEntry, resultTitle, false);
        setResult(ACTIVITY_RESULT_FILEPAGE_SELECT, intent);
        finish();
    }

    /**
     * Retrieve the complete list of media items for the current page.
     * When retrieved, the list will be passed to the ViewPager, and will become a
     * scrollable gallery of media.
     *
     * WARNING: This may be useful if/when we start loading multiple images into the gallery from
     * the feed, but it won't work with the current dummy PageTitle we're using when coming from
     * the feed.
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
                            L.e("Failed to add page to cache.", e);
                        }
                    });
                }
                applyGalleryCollection(result);
            }
            @Override
            public void onCatch(Throwable caught) {
                L.e("Failed to fetch gallery collection.", caught);
                updateProgressBar(false, true, 0);
                FeedbackUtil.showError(GalleryActivity.this, caught);
            }
        }.execute();
    }

    /**
     * Apply a complete collection of media to our scrollable gallery.
     * @param collection GalleryCollection to apply to the ViewPager.
     */
    protected void applyGalleryCollection(GalleryCollection collection) {
        // remove the page transformer while we operate on the pager...
        galleryPager.setPageTransformer(false, null);
        // first, verify that the collection contains the item that the user
        // initially requested, if we have one...
        int initialImagePos = -1;
        if (initialFilename != null) {
            for (GalleryItem item : collection.getItemList()) {
                if (item.getName().equals(initialFilename)) {
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
                        new GalleryItem(initialFilename));
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
    public void layOutGalleryDescription() {
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

    private void setBackgroundGradient(View view, int gravity) {
        ViewUtil.setBackgroundDrawable(view, GradientUtil.getCubicGradient(
                getResources().getColor(R.color.lead_gradient_start), gravity));
    }

    private void initToolbar() {
        final Toolbar toolbar = (Toolbar) findViewById(R.id.gallery_toolbar);
        setBackgroundGradient(toolbar, Gravity.TOP);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");
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
                        .newInstance(pageTitle, site, galleryCollection.getItemList().get(position)));
            }
            return fragmentArray.get(position);
        }
    }

}
