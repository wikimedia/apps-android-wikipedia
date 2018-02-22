package org.wikipedia.gallery;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.BaseActivity;
import org.wikipedia.analytics.GalleryFunnel;
import org.wikipedia.concurrency.CallbackTask;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.feed.image.FeaturedImage;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;
import org.wikipedia.page.LinkMovementMethodExt;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.linkpreview.LinkPreviewDialog;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.theme.Theme;
import org.wikipedia.util.ClipboardUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.GradientUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.ViewAnimations;
import org.wikipedia.views.WikiErrorView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.wikipedia.util.StringUtil.addUnderscores;
import static org.wikipedia.util.StringUtil.strip;
import static org.wikipedia.util.UriUtil.handleExternalLink;
import static org.wikipedia.util.UriUtil.resolveProtocolRelativeUrl;

public class GalleryActivity extends BaseActivity implements LinkPreviewDialog.Callback,
        GalleryItemFragment.Callback {
    public static final int ACTIVITY_RESULT_PAGE_SELECTED = 1;

    public static final String EXTRA_PAGETITLE = "pageTitle";
    public static final String EXTRA_FILENAME = "filename";
    public static final String EXTRA_WIKI = "wiki";
    public static final String EXTRA_SOURCE = "source";
    public static final String EXTRA_FEATURED_IMAGE = "featuredImage";
    public static final String EXTRA_FEATURED_IMAGE_AGE = "featuredImageAge";

    @NonNull private WikipediaApp app = WikipediaApp.getInstance();
    @NonNull private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();
    @Nullable private PageTitle pageTitle;
    @Nullable private WikiSite wiki;
    @NonNull private GalleryCollectionClient client = new GalleryCollectionClient();

    private ViewGroup toolbarContainer;
    private ViewGroup infoContainer;
    private ProgressBar progressBar;
    private TextView descriptionText;
    private ImageView licenseIcon;
    private TextView creditText;
    private WikiErrorView errorView;

    private boolean controlsShowing = true;
    @Nullable private ViewPager.OnPageChangeListener pageChangeListener;

    @Nullable private GalleryFunnel funnel;

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
    private MediaDownloadReceiver downloadReceiver = new MediaDownloadReceiver();
    private MediaDownloadReceiverCallback downloadReceiverCallback = new MediaDownloadReceiverCallback();

    /**
     * Cache that stores GalleryItem information for each corresponding media item in
     * our gallery collection.
     */
    @Nullable private Map<PageTitle, GalleryItem> galleryCache;

    @Nullable
    public Map<PageTitle, GalleryItem> getGalleryCache() {
        return galleryCache;
    }

    private View.OnClickListener licenseShortClickListener = v -> {
        if (v.getContentDescription() == null) {
            return;
        }
        FeedbackUtil.showMessageAsPlainText((Activity) v.getContext(), v.getContentDescription());
    };

    private View.OnLongClickListener licenseLongClickListener = v -> {
        String licenseUrl = (String) v.getTag();
        if (!TextUtils.isEmpty(licenseUrl)) {
            handleExternalLink(GalleryActivity.this, Uri.parse(resolveProtocolRelativeUrl(licenseUrl)));
        }
        return true;
    };

    @NonNull
    public static Intent newIntent(@NonNull Context context, int age, @NonNull String filename,
                                   @NonNull FeaturedImage image, @NonNull WikiSite wiki, int source) {
        return newIntent(context, null, filename, wiki, source)
                .putExtra(EXTRA_FEATURED_IMAGE, GsonMarshaller.marshal(image))
                .putExtra(EXTRA_FEATURED_IMAGE_AGE, age);
    }

    @NonNull
    public static Intent newIntent(@NonNull Context context, @Nullable PageTitle pageTitle,
                                   @NonNull String filename, @NonNull WikiSite wiki, int source) {
        Intent intent = new Intent()
                .setClass(context, GalleryActivity.class)
                .putExtra(EXTRA_FILENAME, filename)
                .putExtra(EXTRA_WIKI, wiki)
                .putExtra(EXTRA_SOURCE, source);
        if (pageTitle != null) {
            intent.putExtra(EXTRA_PAGETITLE, pageTitle);
        }

        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_gallery);
        setSupportActionBar(findViewById(R.id.gallery_toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");

        toolbarContainer = findViewById(R.id.gallery_toolbar_container);
        infoContainer = findViewById(R.id.gallery_info_container);

        findViewById(R.id.gallery_toolbar_gradient)
                .setBackground(GradientUtil.getPowerGradient(R.color.black26, Gravity.TOP));
        findViewById(R.id.gallery_info_gradient)
                .setBackground(GradientUtil.getPowerGradient(R.color.black38, Gravity.BOTTOM));

        progressBar = findViewById(R.id.gallery_progressbar);

        descriptionText = findViewById(R.id.gallery_description_text);
        descriptionText.setMovementMethod(linkMovementMethod);

        licenseIcon = findViewById(R.id.gallery_license_icon);
        licenseIcon.setOnClickListener(licenseShortClickListener);
        licenseIcon.setOnLongClickListener(licenseLongClickListener);

        creditText = findViewById(R.id.gallery_credit_text);

        errorView = findViewById(R.id.view_gallery_error);
        ((ImageView) errorView.findViewById(R.id.view_wiki_error_icon))
                .setColorFilter(color(R.color.base70));
        ((TextView) errorView.findViewById(R.id.view_wiki_error_text))
                .setTextColor(color(R.color.base70));
        errorView.setBackClickListener(v -> onBackPressed());
        errorView.setRetryClickListener(v -> {
            errorView.setVisibility(View.GONE);
            loadGalleryContent();
        });

        if (getIntent().hasExtra(EXTRA_PAGETITLE)) {
            pageTitle = getIntent().getParcelableExtra(EXTRA_PAGETITLE);
        }
        initialFilename = getIntent().getStringExtra(EXTRA_FILENAME);
        wiki = getIntent().getParcelableExtra(EXTRA_WIKI);

        galleryCache = new HashMap<>();
        galleryAdapter = new GalleryItemAdapter(this);
        galleryPager = findViewById(R.id.gallery_item_pager);
        galleryPager.setAdapter(galleryAdapter);
        pageChangeListener = new GalleryPageChangeListener();
        galleryPager.addOnPageChangeListener(pageChangeListener);

        funnel = new GalleryFunnel(app, wiki, getIntent().getIntExtra(EXTRA_SOURCE, 0));

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

            FragmentManager fm = getSupportFragmentManager();
            if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                for (int i = 0; i < fm.getBackStackEntryCount(); i++) {
                    Fragment fragment = fm.findFragmentById(fm.getBackStackEntryAt(i).getId());
                    if (fragment instanceof GalleryItemFragment) {
                        ft.remove(fragment);
                    }
                }
                ft.commitAllowingStateLoss();
            }
        }
        toolbarContainer.post(() -> setControlsShowing(controlsShowing));
        loadGalleryContent();
    }

    @Override public void onDestroy() {
        galleryPager.removeOnPageChangeListener(pageChangeListener);
        pageChangeListener = null;
        super.onDestroy();
    }

    private void loadGalleryItemFor(@NonNull FeaturedImage image, int age) {
        List<GalleryItem> list = new ArrayList<>();
        list.add(new FeaturedImageGalleryItem(image, age));
        applyGalleryCollection(new GalleryCollection(list));
    }

    @Override
    public void onResume() {
        super.onResume();
        registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        downloadReceiver.setCallback(downloadReceiverCallback);
    }

    @Override
    public void onPause() {
        super.onPause();
        downloadReceiver.setCallback(null);
        unregisterReceiver(downloadReceiver);
    }

    @Override
    public void onDownload(@NonNull GalleryItem item) {
        funnel.logGallerySave(pageTitle, item.getName());
        downloadReceiver.download(this, item);
        FeedbackUtil.showMessage(this, R.string.gallery_save_progress);
    }

    @Override
    public void onShare(@NonNull GalleryItem item, @Nullable Bitmap bitmap, @NonNull String subject, @NonNull PageTitle title) {
        funnel.logGalleryShare(pageTitle, item.getName());
        if (bitmap != null) {
            ShareUtil.shareImage(this, bitmap, new File(item.getUrl()).getName(), subject,
                    title.getCanonicalUri());
        } else {
            ShareUtil.shareText(this, title);
        }
    }

    @Override
    protected void setTheme() {
        setTheme(Theme.DARK.getResourceId());
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
    public void onBackPressed() {
        // log the "gallery close" event only upon explicit closing of the activity
        // (back button, or home-as-up button in the toolbar)
        if (getCurrentItem() != null) {
            funnel.logGalleryClose(pageTitle, getCurrentItem().getName());
        }
        super.onBackPressed();
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

    public void showLinkPreview(@NonNull PageTitle title) {
        bottomSheetPresenter.show(getSupportFragmentManager(),
                LinkPreviewDialog.newInstance(title, HistoryEntry.SOURCE_GALLERY, null));
    }

    /**
     * LinkMovementMethod for handling clicking of links in the description or metadata
     * text fields. For internal links, this activity will close, and pass the page title as
     * the result. For external links, they will be bounced out to the Browser.
     */
    private LinkMovementMethodExt linkMovementMethod =
        new LinkMovementMethodExt((@NonNull String url, @Nullable String notUsed) -> {
        L.v("Link clicked was " + url);
        url = resolveProtocolRelativeUrl(url);
        if (url.startsWith("/wiki/")) {
            PageTitle title = app.getWikiSite().titleForInternalLink(url);
            showLinkPreview(title);
        } else {
            Uri uri = Uri.parse(url);
            String authority = uri.getAuthority();
            if (authority != null && WikiSite.supportedAuthority(authority)
                && uri.getPath().startsWith("/wiki/")) {
                PageTitle title = new WikiSite(uri).titleForUri(uri);
                showLinkPreview(title);
            } else {
                // if it's a /w/ URI, turn it into a full URI and go external
                if (url.startsWith("/w/")) {
                    url = String.format("%1$s://%2$s", app.getWikiSite().scheme(),
                            app.getWikiSite().authority()) + url;
                }
                handleExternalLink(GalleryActivity.this, Uri.parse(url));
            }
        }
    });

    /**
     * Close this activity, with the specified PageTitle as the activity result, to be picked up
     * by the activity that originally launched us.
     * @param resultTitle PageTitle to pass as the activity result.
     */
    public void finishWithPageResult(@NonNull PageTitle resultTitle) {
        finishWithPageResult(resultTitle, new HistoryEntry(resultTitle, HistoryEntry.SOURCE_GALLERY));
    }

    public void finishWithPageResult(@NonNull PageTitle resultTitle, @NonNull HistoryEntry historyEntry) {
        Intent intent = PageActivity.newIntentForCurrentTab(GalleryActivity.this, historyEntry, resultTitle);
        setResult(ACTIVITY_RESULT_PAGE_SELECTED, intent);
        finish();
    }

    @Override public void onLinkPreviewLoadPage(@NonNull PageTitle title, @NonNull HistoryEntry entry, boolean inNewTab) {
        finishWithPageResult(title, entry);
    }

    @Override public void onLinkPreviewCopyLink(@NonNull PageTitle title) {
        ClipboardUtil.setPlainText(this, null, title.getCanonicalUri());
        FeedbackUtil.showMessage(this, R.string.address_copied);
    }

    @Override public void onLinkPreviewAddToList(@NonNull PageTitle title) {
        bottomSheetPresenter.showAddToListDialog(getSupportFragmentManager(), title, AddToReadingListDialog.InvokeSource.LINK_PREVIEW_MENU);
    }

    @Override public void onLinkPreviewShareLink(@NonNull PageTitle title) {
        ShareUtil.shareText(this, title);
    }

    void showError(@Nullable Throwable caught, boolean backOnError) {
        // Force going back on button press if coming from a single-item featured-image gallery,
        // because re-setting the collection and calling notifyDataSetChanged() fails to compel the
        // GalleryItemFragment to attempt to reload its image.
        // TODO: Find a way to remove this workaround
        if (backOnError) {
            errorView.setRetryClickListener((v) -> onBackPressed());
        }

        errorView.setError(caught);
        errorView.setVisibility(View.VISIBLE);
    }

    /**
     * Kicks off the activity after the views are initialized in onCreate.
     */
    private void loadGalleryContent() {
        updateProgressBar(false, true, 0);
        if (getIntent().hasExtra(EXTRA_FEATURED_IMAGE)) {
            FeaturedImage featuredImage = GsonUnmarshaller.unmarshal(FeaturedImage.class,
                    getIntent().getStringExtra(EXTRA_FEATURED_IMAGE));
            int age = getIntent().getIntExtra(EXTRA_FEATURED_IMAGE_AGE, 0);
            loadGalleryItemFor(featuredImage, age);
        } else {
            fetchGalleryCollection();
        }
    }

    /**
     * Retrieve the complete list of media items for the current page.
     * When retrieved, the list will be passed to the ViewPager, and will become a
     * scrollable gallery of media.
     */
    private void fetchGalleryCollection() {
        if (pageTitle == null) {
            return;
        }
        updateProgressBar(true, true, 0);

        CallbackTask.execute(new CallbackTask.Task<Map<String, ImageInfo>>() {
            @Override public Map<String, ImageInfo> execute() throws Throwable {
                return client.request(pageTitle.getWikiSite(), pageTitle, false);
            }
        }, new CallbackTask.Callback<Map<String, ImageInfo>>() {
            @Override public void success(Map<String, ImageInfo> result) {
                updateProgressBar(false, true, 0);
                applyGalleryCollection(buildCollection(result));
            }

            @Override public void failure(Throwable caught) {
                updateProgressBar(false, true, 0);
                showError(caught, false);
            }
        });
    }

    @NonNull private GalleryCollection buildCollection(Map<String, ImageInfo> result) {
        List<GalleryItem> list = new ArrayList<>();
        for (Map.Entry<String, ImageInfo> entry : result.entrySet()) {
            if (GalleryCollection.shouldIncludeImage(entry.getValue())) {
                list.add(new GalleryItem(entry.getKey(), entry.getValue()));
            }
        }
        return new GalleryCollection(list);
    }

    /**
     * Apply a complete collection of media to our scrollable gallery.
     * @param collection GalleryCollection to apply to the ViewPager.
     */
    private void applyGalleryCollection(@NonNull GalleryCollection collection) {
        // remove the page transformer while we operate on the pager...
        galleryPager.setPageTransformer(false, null);
        // first, verify that the collection contains the item that the user
        // initially requested, if we have one...
        int initialImagePos = -1;
        if (initialFilename != null) {
            for (GalleryItem item : collection.getItemList()) {
                if (addUnderscores(item.getName()).equals(addUnderscores(initialFilename))) {
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
        if (item.getMetadata() != null && item.getMetadata().imageDescription() != null) {
            descriptionStr = StringUtil.fromHtml(item.getMetadata().imageDescription().value());
        } else if (item.getMetadata() != null && item.getMetadata().objectName() != null) {
            descriptionStr = StringUtil.fromHtml(item.getMetadata().objectName().value());
        }
        if (descriptionStr.length() > 0) {
            descriptionText.setText(strip(descriptionStr));
            descriptionText.setVisibility(View.VISIBLE);
        } else {
            descriptionText.setVisibility(View.GONE);
        }

        // determine which icon to display...
        licenseIcon.setImageResource(getLicenseIcon(item));
        // Set the icon's content description to the UsageTerms property.
        // (if UsageTerms is not present, then default to Fair Use)
        String usageTerms = (item.getMetadata() == null || item.getMetadata().usageTerms() == null)
                ? null : item.getMetadata().usageTerms().value();
        if (TextUtils.isEmpty(usageTerms)) {
            usageTerms = getString(R.string.gallery_fair_use_license);
        }
        licenseIcon.setContentDescription(usageTerms);
        // Give the license URL to the icon, to be received by the click handler (may be null).
        licenseIcon.setTag(item.getLicenseUrl());

        String creditStr = "";
        if (item.getMetadata() != null && item.getMetadata().artist() != null) {
            // todo: is it intentional to convert to a String?
            creditStr = StringUtil.fromHtml(item.getMetadata().artist().value()).toString().trim();
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

        GalleryItemAdapter(AppCompatActivity activity) {
            super(activity.getSupportFragmentManager());
            fragmentArray = new SparseArray<>();
        }

        public void setCollection(@NonNull GalleryCollection collection) {
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
                        .newInstance(pageTitle, wiki, galleryCollection.getItemList().get(position)));
            }
            return fragmentArray.get(position);
        }
    }

    private class MediaDownloadReceiverCallback implements MediaDownloadReceiver.Callback {
        @Override
        public void onSuccess() {
            FeedbackUtil.showMessage(GalleryActivity.this, R.string.gallery_save_success);
        }
    }

    @ColorInt private int color(@ColorRes int id) {
        return ContextCompat.getColor(this, id);
    }
}
