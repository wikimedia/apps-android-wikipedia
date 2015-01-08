package org.wikipedia.page.gallery;

import org.wikipedia.PageTitle;
import org.wikipedia.R;
import org.wikipedia.Site;
import org.wikipedia.ThemedActionBarActivity;
import org.wikipedia.Utils;
import org.wikipedia.ViewAnimations;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.GalleryFunnel;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.LinkMovementMethodExt;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import java.util.HashMap;
import java.util.Map;

public class GalleryActivity extends ThemedActionBarActivity {
    public static final int ACTIVITY_RESULT_FILEPAGE_SELECT = 1;

    public static final String EXTRA_PAGETITLE = "pageTitle";
    public static final String EXTRA_IMAGETITLE = "imageTitle";

    private WikipediaApp app;
    private PageTitle pageTitle;
    private Page page;

    private GalleryFunnel funnel;
    public GalleryFunnel getFunnel() {
        return funnel;
    }

    private ViewPager galleryPager;
    private GalleryItemAdapter galleryAdapter;

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
    private TextView licenseText;
    private TextView creditText;
    private boolean controlsShowing = true;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // force the theme to dark...
        setTheme(WikipediaApp.THEME_DARK);
        app = (WikipediaApp)getApplicationContext();

        // hide system bar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_gallery);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.gallery_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");

        toolbarContainer = (ViewGroup) findViewById(R.id.gallery_toolbar_container);
        infoContainer = (ViewGroup) findViewById(R.id.gallery_info_container);
        progressBar = (ProgressBar) findViewById(R.id.gallery_progressbar);

        descriptionText = (TextView) findViewById(R.id.gallery_description_text);
        descriptionText.setShadowLayer(2, 1, 1, getResources().getColor(R.color.lead_text_shadow));
        descriptionText.setMovementMethod(linkMovementMethod);

        licenseText = (TextView) findViewById(R.id.gallery_license_text);
        licenseText.setShadowLayer(2, 1, 1, getResources().getColor(R.color.lead_text_shadow));
        licenseText.setMovementMethod(linkMovementMethod);

        creditText = (TextView) findViewById(R.id.gallery_credit_text);
        creditText.setShadowLayer(2, 1, 1, getResources().getColor(R.color.lead_text_shadow));

        pageTitle = getIntent().getParcelableExtra(EXTRA_PAGETITLE);
        initialImageTitle = getIntent().getParcelableExtra(EXTRA_IMAGETITLE);

        // find our Page in the page cache...
        if (app.getPageCache().has(pageTitle)) {
            page = app.getPageCache().get(pageTitle);
        }

        galleryCache = new HashMap<>();
        galleryPager = (ViewPager) findViewById(R.id.gallery_item_pager);
        galleryAdapter = new GalleryItemAdapter(this);
        galleryPager.setAdapter(galleryAdapter);
        galleryPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            private int currentPosition = -1;
            @Override
            public void onPageScrolled(int position, float positionOffset,
                                       int positionOffsetPixels) {
            }
            @Override
            public void onPageSelected(int position) {
                // the pager has settled on a new position
                layoutGalleryDescription();
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
        });

        funnel = new GalleryFunnel(app, pageTitle.getSite());

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

        // if our page already has a prepopulated gallery collection, then use it!
        if (page != null && page.getGalleryCollection() != null) {
            applyGalleryCollection(page.getGalleryCollection());
        } else {
            // otherwise, fetch it!
            fetchGalleryCollection();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Crouton.cancelAllCroutons();
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
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
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

    /**
     * Show a "details" dialog that displays miscellaneous information about a
     * specific gallery item
     * @param item GalleryItem for which to show detailed information.
     */
    public void showDetailsDialog(GalleryItem item) {
        new DetailsDialog(this, item, linkMovementMethod,
                          getWindow().getDecorView().getHeight() / 2).show();
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
            if (url.startsWith("//")) {
                // That's a protocol specific link! Make it https!
                url = "https:" + url;
            }
            Log.d("Wikipedia", "Link clicked was " + url);
            Site site = app.getPrimarySite();
            if (url.startsWith("/wiki/")) {
                PageTitle title = site.titleForInternalLink(url);
                finishWithPageResult(title);
            } else {
                Uri uri = Uri.parse(url);
                String authority = uri.getAuthority();
                if (authority != null && Site.isSupportedSite(authority)
                    && uri.getPath().startsWith("/wiki/")) {
                    PageTitle title = site.titleForUri(uri);
                    finishWithPageResult(title);
                } else {
                    // if it's a /w/ URI, turn it into a full URI and go external
                    if (url.startsWith("/w/")) {
                        url = String.format("%1$s://%2$s", WikipediaApp.getInstance().getNetworkProtocol(), site.getDomain()) + url;
                    }
                    Utils.handleExternalLink(GalleryActivity.this, Uri.parse(url));
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
        intent.setClass(GalleryActivity.this, PageActivity.class);
        intent.setAction(PageActivity.ACTION_PAGE_FOR_TITLE);
        intent.putExtra(PageActivity.EXTRA_PAGETITLE, resultTitle);
        intent.putExtra(PageActivity.EXTRA_HISTORYENTRY, historyEntry);
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
        new GalleryCollectionFetchTask(app.getPrimarySiteApi(), pageTitle.getSite(), pageTitle) {
            @Override
            public void onGalleryResult(GalleryCollection result) {
                updateProgressBar(false, true, 0);
                // save it to our current page, for later use
                if (page != null) {
                    page.setGalleryCollection(result);
                }
                applyGalleryCollection(result);
            }
            @Override
            public void onCatch(Throwable caught) {
                updateProgressBar(false, true, 0);
                showError(getString(R.string.error_network_error));
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
        // pass the collection to the adapter!
        galleryAdapter.setCollection(collection);
        if (initialImageTitle != null) {
            // if we have a target image to jump to,
            // then find its position in the collection
            for (GalleryItem item : collection.getItemList()) {
                if (item.getName().equals(initialImageTitle.getDisplayText())) {
                    galleryPager.setCurrentItem(collection.getItemList().indexOf(item), false);
                    break;
                }
            }
        } else if (initialImageIndex >= 0
                   && initialImageIndex < galleryAdapter.getCount()) {
            // if we have a target image index to jump to, then do it!
            galleryPager.setCurrentItem(initialImageIndex, false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Sorry 2.3, you won't get custom transforms between pages.
            galleryPager.setPageTransformer(false, new GalleryPagerTransformer());
        }
    }

    /**
     * Display an error message in the form of a Crouton.
     * @param errorStr Error message to show.
     */
    public void showError(String errorStr) {
        supportInvalidateOptionsMenu();
        Crouton.makeText(GalleryActivity.this, errorStr, Style.ALERT, toolbarContainer)
               .show();
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

        CharSequence descriptionStr = "";
        if (item.getMetadata().containsKey("ImageDescription")) {
            descriptionStr = Html
                    .fromHtml(item.getMetadata().get("ImageDescription"));
        } else if (item.getMetadata().containsKey("ObjectName")) {
            descriptionStr = Html
                    .fromHtml(item.getMetadata().get("ObjectName"));
        }
        if (descriptionStr.length() > 0) {
            descriptionText.setText(Utils.trim(descriptionStr));
            descriptionText.setVisibility(View.VISIBLE);
        } else {
            descriptionText.setVisibility(View.GONE);
        }

        CharSequence licenseStr = "";
        if (item.getMetadata().containsKey("LicenseShortName")) {
            licenseStr = item.getMetadata().get("LicenseShortName");
        } else if (item.getMetadata().containsKey("License")) {
            licenseStr = item.getMetadata().get("License");
        }
        if (!TextUtils.isEmpty(licenseStr)) {
            // is there a license URL? If so, surround the string with it!
            if (item.getMetadata().containsKey("LicenseUrl")) {
                licenseStr = "<a href=\"" + item.getMetadata().get("LicenseUrl")
                             + "\">" + Utils.trim(licenseStr) + "</a>";
            }
            licenseStr = Html
                    .fromHtml(String.format(getString(R.string.gallery_license_text), licenseStr));
            licenseText.setText(licenseStr);
            licenseText.setVisibility(View.VISIBLE);
        } else {
            licenseText.setVisibility(View.GONE);
        }

        CharSequence creditStr = "";
        if (item.getMetadata().containsKey("Artist")) {
            creditStr = String.format(getString(R.string.gallery_credit_text), Utils.trim(
                    Html.fromHtml(item.getMetadata().get("Artist"))));
            creditText.setText(creditStr);
            creditText.setVisibility(View.VISIBLE);
        } else {
            creditText.setVisibility(View.GONE);
        }
        infoContainer.setVisibility(View.VISIBLE);
    }

    /**
     * Adapter that will provide the contents for the ViewPager.
     * Each media item will be represented by a GalleryItemFragment, which will be instantiated
     * lazily, and then cached for future use.
     */
    private class GalleryItemAdapter extends FragmentPagerAdapter {
        private GalleryCollection galleryCollection;
        private SparseArray<GalleryItemFragment> fragmentArray;

        public GalleryItemAdapter(ThemedActionBarActivity activity) {
            super(activity.getSupportFragmentManager());
            fragmentArray = new SparseArray<>();
        }

        public void setCollection(GalleryCollection collection) {
            galleryCollection = collection;
            notifyDataSetChanged();
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
            if (galleryCollection == null) {
                return null;
            }
            // instantiate a new fragment if it doesn't exist
            if (fragmentArray.get(position) == null) {
                fragmentArray.put(position, GalleryItemFragment
                        .newInstance(pageTitle, new PageTitle(galleryCollection.getItemList().get(
                                position).getName(), pageTitle.getSite())));
            }
            return fragmentArray.get(position);
        }
    }

}
