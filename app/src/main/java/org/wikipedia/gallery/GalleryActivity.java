package org.wikipedia.gallery;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.BaseActivity;
import org.wikipedia.analytics.GalleryFunnel;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.media.MediaHelper;
import org.wikipedia.descriptions.DescriptionEditActivity;
import org.wikipedia.feed.image.FeaturedImage;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;
import org.wikipedia.page.LinkMovementMethodExt;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.linkpreview.LinkPreviewDialog;
import org.wikipedia.suggestededits.SuggestedEditsSummary;
import org.wikipedia.theme.Theme;
import org.wikipedia.util.ClipboardUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.GradientUtil;
import org.wikipedia.util.ImageUrlUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.ViewAnimations;
import org.wikipedia.views.WikiErrorView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import butterknife.Unbinder;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static org.wikipedia.Constants.InvokeSource.LINK_PREVIEW_MENU;
import static org.wikipedia.Constants.InvokeSource.SUGGESTED_EDITS_ADD_CAPTION;
import static org.wikipedia.Constants.InvokeSource.SUGGESTED_EDITS_TRANSLATE_CAPTION;
import static org.wikipedia.Constants.PREFERRED_GALLERY_IMAGE_SIZE;
import static org.wikipedia.util.StringUtil.addUnderscores;
import static org.wikipedia.util.StringUtil.removeUnderscores;
import static org.wikipedia.util.StringUtil.strip;
import static org.wikipedia.util.UriUtil.handleExternalLink;
import static org.wikipedia.util.UriUtil.resolveProtocolRelativeUrl;

public class GalleryActivity extends BaseActivity implements LinkPreviewDialog.Callback,
        GalleryItemFragment.Callback {
    public static final int ACTIVITY_RESULT_PAGE_SELECTED = 1;
    private static final int ACTIVITY_REQUEST_DESCRIPTION_EDIT = 2;
    public static final int ACTIVITY_RESULT_IMAGE_CAPTION_ADDED = 3;

    public static final String EXTRA_PAGETITLE = "pageTitle";
    public static final String EXTRA_FILENAME = "filename";
    public static final String EXTRA_WIKI = "wiki";
    public static final String EXTRA_SOURCE = "source";
    public static final String EXTRA_FEATURED_IMAGE = "featuredImage";
    public static final String EXTRA_FEATURED_IMAGE_AGE = "featuredImageAge";

    @NonNull private WikipediaApp app = WikipediaApp.getInstance();
    @NonNull private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();
    @Nullable private PageTitle pageTitle;

    @BindView(R.id.gallery_toolbar_container) ViewGroup toolbarContainer;
    @BindView(R.id.gallery_toolbar) Toolbar toolbar;
    @BindView(R.id.gallery_toolbar_gradient) View toolbarGradient;
    @BindView(R.id.gallery_info_container) ViewGroup infoContainer;
    @BindView(R.id.gallery_info_gradient) View infoGradient;
    @BindView(R.id.gallery_progressbar) ProgressBar progressBar;
    @BindView(R.id.gallery_description_container) View galleryDescriptionContainer;
    @BindView(R.id.gallery_description_text) TextView descriptionText;
    @BindView(R.id.gallery_license_icon) ImageView licenseIcon;
    @BindView(R.id.gallery_license_icon_by) ImageView byIcon;
    @BindView(R.id.gallery_license_icon_sa) ImageView saIcon;
    @BindView(R.id.gallery_credit_text) TextView creditText;
    @BindView(R.id.gallery_item_pager) ViewPager galleryPager;
    @BindView(R.id.view_gallery_error) WikiErrorView errorView;
    @BindView(R.id.gallery_caption_edit_button) View captionEditButton;
    @BindView(R.id.gallery_caption_translate_container) View captionTranslateContainer;
    @BindView(R.id.gallery_caption_translate_button_text) TextView captionTranslateButtonText;
    @Nullable private Unbinder unbinder;
    private CompositeDisposable disposables = new CompositeDisposable();
    private Disposable imageCaptionDisposable;
    private WikiSite sourceWiki;

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

    private GalleryItemAdapter galleryAdapter;
    private MediaDownloadReceiver downloadReceiver = new MediaDownloadReceiver();
    private MediaDownloadReceiverCallback downloadReceiverCallback = new MediaDownloadReceiverCallback();
    @Nullable private String targetLanguageCode;

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
        unbinder = ButterKnife.bind(this);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("");

        toolbarGradient.setBackground(GradientUtil.getPowerGradient(R.color.black26, Gravity.TOP));
        infoGradient.setBackground(GradientUtil.getPowerGradient(R.color.black38, Gravity.BOTTOM));
        descriptionText.setMovementMethod(linkMovementMethod);
        creditText.setMovementMethod(linkMovementMethod);

        ((ImageView) errorView.findViewById(R.id.view_wiki_error_icon))
                .setColorFilter(ContextCompat.getColor(this, R.color.base70));
        ((TextView) errorView.findViewById(R.id.view_wiki_error_text))
                .setTextColor(ContextCompat.getColor(this, R.color.base70));

        errorView.setBackClickListener(v -> onBackPressed());
        errorView.setRetryClickListener(v -> {
            errorView.setVisibility(View.GONE);
            loadGalleryContent();
        });

        if (getIntent().hasExtra(EXTRA_PAGETITLE)) {
            pageTitle = getIntent().getParcelableExtra(EXTRA_PAGETITLE);
        }
        initialFilename = getIntent().getStringExtra(EXTRA_FILENAME);
        sourceWiki = getIntent().getParcelableExtra(EXTRA_WIKI);

        galleryAdapter = new GalleryItemAdapter(GalleryActivity.this);
        galleryPager.setAdapter(galleryAdapter);
        pageChangeListener = new GalleryPageChangeListener();
        galleryPager.addOnPageChangeListener(pageChangeListener);

        funnel = new GalleryFunnel(app, getIntent().getParcelableExtra(EXTRA_WIKI),
                getIntent().getIntExtra(EXTRA_SOURCE, 0));

        if (savedInstanceState == null) {
            if (initialFilename != null) {
                funnel.logGalleryOpen(pageTitle, removeUnderscores(initialFilename));
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
        disposables.clear();
        disposeImageCaptionDisposable();
        galleryPager.removeOnPageChangeListener(pageChangeListener);
        pageChangeListener = null;

        if (unbinder != null) {
            unbinder.unbind();
        }

        super.onDestroy();
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_gallery, menu);
        return true;
    }

    @Override
    public void onDownload(@NonNull GalleryItemFragment item) {
        funnel.logGallerySave(pageTitle, item.getImageTitle().getDisplayText());
        downloadReceiver.download(this, item.getImageTitle(), item.getMediaInfo());
        FeedbackUtil.showMessage(this, R.string.gallery_save_progress);
    }

    @Override
    public void onShare(@NonNull GalleryItemFragment item, @Nullable Bitmap bitmap, @NonNull String subject, @NonNull PageTitle title) {
        funnel.logGalleryShare(pageTitle, item.getImageTitle().getDisplayText());
        if (bitmap != null) {
            ShareUtil.shareImage(this, bitmap, new File(ImageUrlUtil.getUrlForPreferredSize(item.getMediaInfo().getThumbUrl(), PREFERRED_GALLERY_IMAGE_SIZE)).getName(),
                    subject, title.getCanonicalUri());
        } else {
            ShareUtil.shareText(this, title);
        }
    }

    @Override
    protected void setTheme() {
        setTheme(Theme.DARK.getResourceId());
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVITY_REQUEST_DESCRIPTION_EDIT && resultCode == RESULT_OK) {
            FeedbackUtil.showMessage(this, getString(R.string.description_edit_success_saved_image_caption_in_lang_snackbar,
                    app.language().getAppLanguageLocalizedName(StringUtils.defaultString(targetLanguageCode, app.language().getAppLanguageCode()))));
            layOutGalleryDescription();
            setResult(ACTIVITY_RESULT_IMAGE_CAPTION_ADDED);
        }
    }

    @OnClick(R.id.gallery_caption_edit_button) void onEditClick(View v) {
        GalleryItemFragment item = getCurrentItem();
        PageTitle title = new PageTitle(item.getImageTitle().getPrefixedText(), new WikiSite(Service.COMMONS_URL, sourceWiki.languageCode()));
        String currentCaption = item.getMediaInfo().getCaptions().get(sourceWiki.languageCode());
        title.setDescription(currentCaption);

        SuggestedEditsSummary summary = new SuggestedEditsSummary(title.getPrefixedText(), sourceWiki.languageCode(), title,
                title.getDisplayText(), title.getDisplayText(), StringUtils.defaultIfBlank(StringUtil.fromHtml(item.getMediaInfo().getMetadata().imageDescription()).toString(), null),
                item.getMediaInfo().getThumbUrl(), null, null, null, null);

        startActivityForResult(DescriptionEditActivity.newIntent(this, title, null, summary, null, SUGGESTED_EDITS_ADD_CAPTION),
                ACTIVITY_REQUEST_DESCRIPTION_EDIT);
    }

    @OnClick(R.id.gallery_caption_translate_button) void onTranslateClick(View v) {
        if (app.language().getAppLanguageCodes().size() < 2) {
            return;
        }
        GalleryItemFragment item = getCurrentItem();
        PageTitle sourceTitle = new PageTitle(item.getImageTitle().getPrefixedText(), new WikiSite(Service.COMMONS_URL, sourceWiki.languageCode()));
        PageTitle targetTitle = new PageTitle(item.getImageTitle().getPrefixedText(), new WikiSite(Service.COMMONS_URL, StringUtils.defaultString(targetLanguageCode, app.language().getAppLanguageCodes().get(1))));
        String currentCaption = item.getMediaInfo().getCaptions().get(app.getAppOrSystemLanguageCode());
        if (TextUtils.isEmpty(currentCaption)) {
            currentCaption = StringUtil.fromHtml(item.getMediaInfo().getMetadata().imageDescription()).toString();
        }

        SuggestedEditsSummary sourceSummary = new SuggestedEditsSummary(sourceTitle.getPrefixedText(), sourceTitle.getWikiSite().languageCode(), sourceTitle,
                sourceTitle.getDisplayText(), sourceTitle.getDisplayText(), currentCaption, item.getMediaInfo().getThumbUrl(),
                null, null, null, null);

        SuggestedEditsSummary targetSummary = new SuggestedEditsSummary(targetTitle.getPrefixedText(), targetTitle.getWikiSite().languageCode(), targetTitle,
                targetTitle.getDisplayText(), targetTitle.getDisplayText(), null, item.getMediaInfo().getThumbUrl(),
                null, null, null, null);

        startActivityForResult(DescriptionEditActivity.newIntent(this, targetTitle, null, sourceSummary, targetSummary,
                (sourceSummary.getLang().equals(targetSummary.getLang())) ? SUGGESTED_EDITS_ADD_CAPTION : SUGGESTED_EDITS_TRANSLATE_CAPTION),
                ACTIVITY_REQUEST_DESCRIPTION_EDIT);
    }

    @OnClick(R.id.license_container) void onClick(View v) {
        if (licenseIcon.getContentDescription() == null) {
            return;
        }
        FeedbackUtil.showMessageAsPlainText((Activity) licenseIcon.getContext(), licenseIcon.getContentDescription());
    }

    @OnLongClick(R.id.license_container) boolean onLongClick(View v) {
        String licenseUrl = (String) licenseIcon.getTag();
        if (!TextUtils.isEmpty(licenseUrl)) {
            handleExternalLink(GalleryActivity.this, Uri.parse(resolveProtocolRelativeUrl(licenseUrl)));
        }
        return true;
    }

    private void disposeImageCaptionDisposable() {
        if (imageCaptionDisposable != null && !imageCaptionDisposable.isDisposed()) {
            imageCaptionDisposable.dispose();
        }
    }

    private class GalleryPageChangeListener extends ViewPager.SimpleOnPageChangeListener {
        private int currentPosition = -1;
        @Override
        public void onPageSelected(int position) {
            // the pager has settled on a new position
            layOutGalleryDescription();
            if (currentPosition != -1 && getCurrentItem() != null && getCurrentItem().getImageTitle() != null) {
                if (position < currentPosition) {
                    funnel.logGallerySwipeLeft(pageTitle, getCurrentItem().getImageTitle().getDisplayText());
                } else if (position > currentPosition) {
                    funnel.logGallerySwipeRight(pageTitle, getCurrentItem().getImageTitle().getDisplayText());
                }
            }
            currentPosition = position;
        }
        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == ViewPager.SCROLL_STATE_IDLE) {
                galleryAdapter.purgeFragments();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("controlsShowing", controlsShowing);
        outState.putInt("pagerIndex", galleryPager.getCurrentItem());
    }

    private void updateProgressBar(boolean visible) {
        progressBar.setIndeterminate(true);
        progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onBackPressed() {
        // log the "gallery close" event only upon explicit closing of the activity
        // (back button, or home-as-up button in the toolbar)
        if (getCurrentItem() != null && getCurrentItem().getImageTitle() != null) {
            funnel.logGalleryClose(pageTitle, getCurrentItem().getImageTitle().getDisplayText());
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
                LinkPreviewDialog.newInstance(new HistoryEntry(title, HistoryEntry.SOURCE_GALLERY), null));
    }

    /**
     * LinkMovementMethod for handling clicking of links in the description or metadata
     * text fields. For internal links, this activity will close, and pass the page title as
     * the result. For external links, they will be bounced out to the Browser.
     */
    private LinkMovementMethodExt linkMovementMethod =
        new LinkMovementMethodExt((@NonNull String url) -> {
        L.v("Link clicked was " + url);
        url = resolveProtocolRelativeUrl(url);
        if (url.startsWith("/wiki/")) {
            PageTitle title = app.getWikiSite().titleForInternalLink(url);
            showLinkPreview(title);
        } else {
            Uri uri = Uri.parse(url);
            String authority = uri.getAuthority();
            if (authority != null && WikiSite.supportedAuthority(authority)
                && uri.getPath() != null && uri.getPath().startsWith("/wiki/")) {
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
        Intent intent = PageActivity.newIntentForCurrentTab(GalleryActivity.this, historyEntry, resultTitle, false);
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
        bottomSheetPresenter.showAddToListDialog(getSupportFragmentManager(), title, LINK_PREVIEW_MENU);
    }

    @Override public void onLinkPreviewShareLink(@NonNull PageTitle title) {
        ShareUtil.shareText(this, title);
    }

    void showError(@Nullable Throwable caught) {
        errorView.setError(caught);
        errorView.setVisibility(View.VISIBLE);
    }

    private void fetchGalleryItems() {
        if (pageTitle == null) {
            return;
        }
        updateProgressBar(true);

        disposables.add(ServiceFactory.getRest(pageTitle.getWikiSite()).getMediaList(pageTitle.getConvertedText())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaList -> {
                    updateProgressBar(false);
                    applyGalleryList(mediaList.getItems("image", "video"));
                }, caught -> {
                    updateProgressBar(false);
                    showError(caught);
                }));
    }

    /**
     * Kicks off the activity after the views are initialized in onCreate.
     */
    private void loadGalleryContent() {
        updateProgressBar(false);
        if (getIntent().hasExtra(EXTRA_FEATURED_IMAGE)) {
            FeaturedImage featuredImage = GsonUnmarshaller.unmarshal(FeaturedImage.class,
                    getIntent().getStringExtra(EXTRA_FEATURED_IMAGE));
            featuredImage.setAge(getIntent().getIntExtra(EXTRA_FEATURED_IMAGE_AGE, 0));
            updateProgressBar(false);
            applyGalleryList(Collections.singletonList(new MediaListItem(featuredImage.title())));
        } else {
            fetchGalleryItems();
        }
    }

    private void applyGalleryList(@NonNull List<MediaListItem> list) {
        // remove the page transformer while we operate on the pager...
        galleryPager.setPageTransformer(false, null);
        // first, verify that the collection contains the item that the user
        // initially requested, if we have one...
        int initialImagePos = -1;
        if (initialFilename != null) {
            for (MediaListItem item : list) {
                // sometimes the namespace of a file would be in different languages rather than English.
                String title = StringUtil.removeNamespace(item.getTitle());
                String titleFromPage = StringUtil.removeNamespace(addUnderscores(initialFilename));
                if (title.equals(titleFromPage)) {
                    initialImagePos = list.indexOf(item);
                    break;
                }
            }
            if (initialImagePos == -1) {
                // the requested image is not present in the gallery collection, so add it manually.
                // (this can happen if the user clicked on an SVG file, since we hide SVGs
                // by default in the gallery; or lead image in the PageHeader or in the info box)
                initialImagePos = 0;
                list = new ArrayList<>(list);
                list.add(initialImagePos, new MediaListItem(initialFilename));
            }
        }

        // pass the collection to the adapter!
        galleryAdapter.setList(list);
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

    private GalleryItemFragment getCurrentItem() {
        if (galleryAdapter.getItem(galleryPager.getCurrentItem()) == null) {
            return null;
        }
        return ((GalleryItemFragment) galleryAdapter.getItem(galleryPager.getCurrentItem()));
    }

    /**
     * Populate the description and license text fields with data from the current gallery item.
     */
    public void layOutGalleryDescription() {
        GalleryItemFragment item = getCurrentItem();
        if (item == null || item.getImageTitle() == null) {
            infoContainer.setVisibility(View.GONE);
            return;
        }
        updateProgressBar(true);
        disposeImageCaptionDisposable();
        imageCaptionDisposable = MediaHelper.INSTANCE.getImageCaptions(item.getImageTitle().getPrefixedText())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate(this::updateGalleryDescription)
                .subscribe(captions -> getCurrentItem().getMediaInfo().setCaptions(captions),
                        L::e);
    }

    public void updateGalleryDescription() {
        updateProgressBar(false);
        GalleryItemFragment item = getCurrentItem();
        if (item == null) {
            infoContainer.setVisibility(View.GONE);
            return;
        }
        galleryAdapter.notifyFragments(galleryPager.getCurrentItem());

        CharSequence descriptionStr;

        // Display the Caption Edit button based on whether the image is hosted on Commons,
        // and not the local Wikipedia.
        boolean captionEditable = AccountUtil.isLoggedIn() && item.getMediaInfo().getThumbUrl().contains(Service.URL_FRAGMENT_FROM_COMMONS);
        captionEditButton.setVisibility(captionEditable ? View.VISIBLE : View.GONE);

        boolean allowTranslate = false;
        // and if we have another language in which the caption doesn't exist, then offer
        // it to be translatable.
        if (app.language().getAppLanguageCodes().size() > 1 && captionEditable) {
            if (!item.getMediaInfo().getCaptions().containsKey(sourceWiki.languageCode())) {
                allowTranslate = true;
                targetLanguageCode = sourceWiki.languageCode();
            } else {
                for (String lang : app.language().getAppLanguageCodes()) {
                    if (!item.getMediaInfo().getCaptions().containsKey(lang)) {
                        allowTranslate = true;
                        targetLanguageCode = lang;
                        break;
                    }
                }
            }
        }

        // If we have a structured caption in our current language, then display that instead
        // of the unstructured description, and make it editable.

        if (item.getMediaInfo().getCaptions().containsKey(sourceWiki.languageCode())) {
            descriptionStr = item.getMediaInfo().getCaptions().get(sourceWiki.languageCode());
        } else {
            descriptionStr = StringUtil.fromHtml(item.getMediaInfo().getMetadata().imageDescription());
        }
        if (descriptionStr != null && descriptionStr.length() > 0) {
            galleryDescriptionContainer.setVisibility(View.VISIBLE);
            descriptionText.setText(strip(descriptionStr));
        } else {
            galleryDescriptionContainer.setVisibility(View.GONE);
        }
        captionTranslateContainer.setVisibility(allowTranslate && AccountUtil.isLoggedIn() ? View.VISIBLE : View.GONE);
        captionTranslateButtonText.setText(getString(R.string.gallery_add_image_caption_in_language_button,
                app.language().getAppLanguageLocalizedName(targetLanguageCode)));

        ImageLicense license = new ImageLicense(item.getMediaInfo().getMetadata().license(), item.getMediaInfo().getMetadata().licenseShortName(), item.getMediaInfo().getMetadata().licenseUrl());

        // determine which icon to display...
        if (license.getLicenseIcon() == R.drawable.ic_license_by) {
            licenseIcon.setImageResource(R.drawable.ic_license_cc);
            byIcon.setImageResource(R.drawable.ic_license_by);
            byIcon.setVisibility(View.VISIBLE);
            saIcon.setImageResource(R.drawable.ic_license_sharealike);
            saIcon.setVisibility(View.VISIBLE);
        } else {
            licenseIcon.setImageResource(license.getLicenseIcon());
            byIcon.setVisibility(View.GONE);
            saIcon.setVisibility(View.GONE);
        }

        // Set the icon's content description to the UsageTerms property.
        // (if UsageTerms is not present, then default to Fair Use)
        licenseIcon.setContentDescription(StringUtils.defaultIfBlank(item.getMediaInfo().getMetadata().licenseShortName(), getString(R.string.gallery_fair_use_license)));
        // Give the license URL to the icon, to be received by the click handler (may be null).
        licenseIcon.setTag(item.getMediaInfo().getMetadata().licenseUrl());

        String creditStr = !TextUtils.isEmpty(item.getMediaInfo().getMetadata().artist()) ? item.getMediaInfo().getMetadata().artist() : item.getMediaInfo().getMetadata().credit();

        // if we couldn't find a attribution string, then default to unknown
        creditText.setText(StringUtil.fromHtml(StringUtils.defaultIfBlank(creditStr, getString(R.string.gallery_uploader_unknown))));

        infoContainer.setVisibility(View.VISIBLE);
    }

    /**
     * Adapter that will provide the contents for the ViewPager.
     * Each media item will be represented by a GalleryItemFragment, which will be instantiated
     * lazily, and then cached for future use.
     */
    private class GalleryItemAdapter extends FragmentPagerAdapter {
        private List<MediaListItem> list = new ArrayList<>();
        private SparseArray<GalleryItemFragment> fragmentArray;

        GalleryItemAdapter(AppCompatActivity activity) {
            super(activity.getSupportFragmentManager());
            fragmentArray = new SparseArray<>();
        }

        public void setList(@NonNull List<MediaListItem> list) {
            this.list.clear();
            this.list.addAll(list);
            notifyDataSetChanged();
        }

        void notifyFragments(int currentPosition) {
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
        void purgeFragments() {
            int position = galleryPager.getCurrentItem();
            FragmentTransaction trans = getSupportFragmentManager().beginTransaction();
            for (int i = 0; i < getCount(); i++) {
                if (Math.abs(position - i) < 2) {
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
            return list.size();
        }

        @Override
        public Fragment getItem(int position) {
            if (list.size() <= position || position < 0) {
                return null;
            }
            // instantiate a new fragment if it doesn't exist
            if (fragmentArray.get(position) == null) {
                fragmentArray.put(position, GalleryItemFragment
                        .newInstance(pageTitle, list.get(position)));
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
}
