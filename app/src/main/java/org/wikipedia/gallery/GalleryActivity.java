package org.wikipedia.gallery;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Pair;
import android.view.Gravity;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager2.widget.ViewPager2;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.Constants.ImageEditType;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.BaseActivity;
import org.wikipedia.analytics.GalleryFunnel;
import org.wikipedia.auth.AccountUtil;
import org.wikipedia.bridge.JavaScriptActionHandler;
import org.wikipedia.commons.FilePageActivity;
import org.wikipedia.commons.ImageTagsProvider;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.media.MediaHelper;
import org.wikipedia.descriptions.DescriptionEditActivity;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;
import org.wikipedia.page.LinkMovementMethodExt;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.linkpreview.LinkPreviewDialog;
import org.wikipedia.suggestededits.PageSummaryForEdit;
import org.wikipedia.suggestededits.SuggestedEditsImageTagEditActivity;
import org.wikipedia.suggestededits.SuggestedEditsSnackbars;
import org.wikipedia.theme.Theme;
import org.wikipedia.util.ClipboardUtil;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.GradientUtil;
import org.wikipedia.util.ImageUrlUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.PositionAwareFragmentStateAdapter;
import org.wikipedia.views.ViewAnimations;
import org.wikipedia.views.ViewUtil;
import org.wikipedia.views.WikiErrorView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnLongClick;
import butterknife.Unbinder;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static org.wikipedia.Constants.INTENT_EXTRA_ACTION;
import static org.wikipedia.Constants.InvokeSource.GALLERY_ACTIVITY;
import static org.wikipedia.Constants.InvokeSource.LINK_PREVIEW_MENU;
import static org.wikipedia.Constants.PREFERRED_GALLERY_IMAGE_SIZE;
import static org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_CAPTION;
import static org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_IMAGE_TAGS;
import static org.wikipedia.descriptions.DescriptionEditActivity.Action.TRANSLATE_CAPTION;
import static org.wikipedia.util.StringUtil.strip;
import static org.wikipedia.util.UriUtil.handleExternalLink;
import static org.wikipedia.util.UriUtil.resolveProtocolRelativeUrl;

public class GalleryActivity extends BaseActivity implements LinkPreviewDialog.Callback,
        GalleryItemFragment.Callback {
    public static final int ACTIVITY_RESULT_PAGE_SELECTED = 1;
    private static final int ACTIVITY_REQUEST_DESCRIPTION_EDIT = 2;
    public static final int ACTIVITY_RESULT_IMAGE_CAPTION_ADDED = 3;
    public static final int ACTIVITY_REQUEST_ADD_IMAGE_TAGS = 4;

    public static final String EXTRA_PAGETITLE = "pageTitle";
    public static final String EXTRA_FILENAME = "filename";
    public static final String EXTRA_WIKI = "wiki";
    public static final String EXTRA_REVISION = "revision";
    public static final String EXTRA_SOURCE = "source";

    private static JavaScriptActionHandler.ImageHitInfo TRANSITION_INFO;

    @NonNull private WikipediaApp app = WikipediaApp.getInstance();
    @NonNull private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();
    @Nullable private PageTitle pageTitle;

    @BindView(R.id.gallery_transition_receiver) ImageView transitionReceiver;
    @BindView(R.id.gallery_toolbar_container) ViewGroup toolbarContainer;
    @BindView(R.id.gallery_toolbar) Toolbar toolbar;
    @BindView(R.id.gallery_toolbar_gradient) View toolbarGradient;
    @BindView(R.id.gallery_info_container) ViewGroup infoContainer;
    @BindView(R.id.gallery_info_gradient) View infoGradient;
    @BindView(R.id.gallery_progressbar) ProgressBar progressBar;
    @BindView(R.id.gallery_description_container) View galleryDescriptionContainer;
    @BindView(R.id.gallery_description_text) TextView descriptionText;
    @BindView(R.id.gallery_license_container) View licenseContainer;
    @BindView(R.id.gallery_license_icon) ImageView licenseIcon;
    @BindView(R.id.gallery_license_icon_by) ImageView byIcon;
    @BindView(R.id.gallery_license_icon_sa) ImageView saIcon;
    @BindView(R.id.gallery_credit_text) TextView creditText;
    @BindView(R.id.gallery_item_pager) ViewPager2 galleryPager;
    @BindView(R.id.view_gallery_error) WikiErrorView errorView;
    @BindView(R.id.gallery_caption_edit_button) ImageView captionEditButton;
    @BindView(R.id.gallery_cta_container) View ctaContainer;
    @BindView(R.id.gallery_cta_button_text) TextView ctaButtonText;
    @Nullable private Unbinder unbinder;
    @Nullable private ImageEditType imageEditType;
    private CompositeDisposable disposables = new CompositeDisposable();
    private Disposable imageCaptionDisposable;
    private long revision;
    private WikiSite sourceWiki;

    private boolean controlsShowing = true;
    private GalleryPageChangeListener pageChangeListener = new GalleryPageChangeListener();

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
    public static Intent newIntent(@NonNull Context context, @Nullable PageTitle pageTitle,
                                   @NonNull String filename, @NonNull WikiSite wiki, long revision, int source) {
        Intent intent = new Intent()
                .setClass(context, GalleryActivity.class)
                .putExtra(EXTRA_FILENAME, filename)
                .putExtra(EXTRA_WIKI, wiki)
                .putExtra(EXTRA_REVISION, revision)
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
        setNavigationBarColor(Color.BLACK);

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
        revision = getIntent().getLongExtra(EXTRA_REVISION, 0);
        sourceWiki = getIntent().getParcelableExtra(EXTRA_WIKI);

        galleryAdapter = new GalleryItemAdapter(GalleryActivity.this);
        galleryPager.setAdapter(galleryAdapter);
        galleryPager.registerOnPageChangeCallback(pageChangeListener);
        galleryPager.setOffscreenPageLimit(2);

        funnel = new GalleryFunnel(app, getIntent().getParcelableExtra(EXTRA_WIKI),
                getIntent().getIntExtra(EXTRA_SOURCE, 0));

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
        toolbarContainer.post(() -> {
            if (isDestroyed()) {
                return;
            }
            setControlsShowing(controlsShowing);
        });

        if (TRANSITION_INFO != null
                && TRANSITION_INFO.getWidth() > 0 && TRANSITION_INFO.getHeight() > 0) {

            float aspect = TRANSITION_INFO.getHeight() / TRANSITION_INFO.getWidth();

            FrameLayout.LayoutParams params = DimenUtil.getDisplayWidthPx() < DimenUtil.getDisplayHeightPx()
                    ? new FrameLayout.LayoutParams(DimenUtil.getDisplayWidthPx(), (int)(DimenUtil.getDisplayWidthPx() * aspect))
                    : new FrameLayout.LayoutParams((int)(DimenUtil.getDisplayHeightPx() / aspect), DimenUtil.getDisplayHeightPx());
            params.gravity = Gravity.CENTER;
            transitionReceiver.setLayoutParams(params);

            transitionReceiver.setVisibility(View.VISIBLE);
            ViewUtil.loadImage(transitionReceiver, TRANSITION_INFO.getSrc(), TRANSITION_INFO.getCenterCrop(), false, false, null);

            final int transitionMillis = 500;
            transitionReceiver.postDelayed(() -> {
                if (isDestroyed()) {
                    return;
                }
                loadGalleryContent();
            }, transitionMillis);

        } else {

            TRANSITION_INFO = null;
            transitionReceiver.setVisibility(View.GONE);
            loadGalleryContent();

        }
    }

    @Override public void onDestroy() {
        disposables.clear();
        disposeImageCaptionDisposable();
        galleryPager.unregisterOnPageChangeCallback(pageChangeListener);
        pageChangeListener = null;

        if (unbinder != null) {
            unbinder.unbind();
        }

        TRANSITION_INFO = null;
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
        if (funnel != null && item.getImageTitle() != null) {
            funnel.logGallerySave(pageTitle, item.getImageTitle().getDisplayText());
        }
        if (item.getImageTitle() != null && item.getMediaInfo() != null) {
            downloadReceiver.download(this, item.getImageTitle(), item.getMediaInfo());
            FeedbackUtil.showMessage(this, R.string.gallery_save_progress);
        } else {
            FeedbackUtil.showMessage(this, R.string.err_cannot_save_file);
        }
    }

    @Override
    public void onShare(@NonNull GalleryItemFragment item, @Nullable Bitmap bitmap, @NonNull String subject, @NonNull PageTitle title) {
        if (funnel != null && item.getImageTitle() != null) {
            funnel.logGalleryShare(pageTitle, item.getImageTitle().getDisplayText());
        }
        if (bitmap != null && item.getMediaInfo() != null) {
            ShareUtil.shareImage(this, bitmap, new File(ImageUrlUtil.getUrlForPreferredSize(item.getMediaInfo().getThumbUrl(),
                    PREFERRED_GALLERY_IMAGE_SIZE)).getName(), subject, title.getUri());
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
        if ((requestCode == ACTIVITY_REQUEST_DESCRIPTION_EDIT || requestCode == ACTIVITY_REQUEST_ADD_IMAGE_TAGS) && resultCode == RESULT_OK) {

            DescriptionEditActivity.Action action = (data != null && data.hasExtra(INTENT_EXTRA_ACTION)) ? (DescriptionEditActivity.Action) data.getSerializableExtra(INTENT_EXTRA_ACTION)
                    : (requestCode == ACTIVITY_REQUEST_ADD_IMAGE_TAGS) ? ADD_IMAGE_TAGS : null;
            SuggestedEditsSnackbars.show(this, action, true, targetLanguageCode, action == ADD_IMAGE_TAGS, () -> {
                if (action == ADD_IMAGE_TAGS && getCurrentItem() != null && getCurrentItem().getImageTitle() != null) {
                    startActivity(FilePageActivity.newIntent(GalleryActivity.this, getCurrentItem().getImageTitle()));
                }
            });
            layOutGalleryDescription();
            setResult((requestCode == ACTIVITY_REQUEST_DESCRIPTION_EDIT) ? ACTIVITY_RESULT_IMAGE_CAPTION_ADDED : ACTIVITY_REQUEST_ADD_IMAGE_TAGS);
        }
    }

    @OnClick(R.id.gallery_caption_edit_button) void onEditClick(View v) {
        GalleryItemFragment item = getCurrentItem();
        if (item == null || item.getImageTitle() == null || item.getMediaInfo() == null || item.getMediaInfo().getMetadata() == null) {
            return;
        }

        boolean isProtected = v.getTag() != null && (Boolean) v.getTag();
        if (isProtected) {
            new AlertDialog.Builder(this)
                    .setCancelable(false)
                    .setTitle(R.string.page_protected_can_not_edit_title)
                    .setMessage(R.string.page_protected_can_not_edit)
                    .setPositiveButton(R.string.protected_page_warning_dialog_ok_button_text, null)
                    .show();
            return;
        }
        startCaptionEdit(item);
    }

    public static void setTransitionInfo(@NonNull JavaScriptActionHandler.ImageHitInfo hitInfo) {
        TRANSITION_INFO = hitInfo;
    }

    private void startCaptionEdit(GalleryItemFragment item) {
        PageTitle title = new PageTitle(item.getImageTitle().getPrefixedText(), new WikiSite(Service.COMMONS_URL, sourceWiki.languageCode()));
        String currentCaption = item.getMediaInfo().getCaptions().get(sourceWiki.languageCode());
        title.setDescription(currentCaption);

        PageSummaryForEdit summary = new PageSummaryForEdit(title.getPrefixedText(), sourceWiki.languageCode(), title,
                title.getDisplayText(), StringUtils.defaultIfBlank(StringUtil.fromHtml(item.getMediaInfo().getMetadata().imageDescription()).toString(), null),
                item.getMediaInfo().getThumbUrl());

        startActivityForResult(DescriptionEditActivity.newIntent(this, title, null, summary, null, ADD_CAPTION, GALLERY_ACTIVITY),
                ACTIVITY_REQUEST_DESCRIPTION_EDIT);
    }

    @OnClick(R.id.gallery_cta_button) void onTranslateClick(View v) {
        GalleryItemFragment item = getCurrentItem();
        if (item == null || item.getImageTitle() == null || item.getMediaInfo() == null || item.getMediaInfo().getMetadata() == null || imageEditType == null) {
            return;
        }
        switch (imageEditType) {
            case ADD_TAGS:
                startTagsEdit(item);
                break;
            case ADD_CAPTION_TRANSLATION:
                startCaptionTranslation(item);
                break;
            default:
                startCaptionEdit(item);
        }
    }

    private void startTagsEdit(GalleryItemFragment item) {
        startActivityForResult(SuggestedEditsImageTagEditActivity.newIntent(this, item.getMediaPage(), GALLERY_ACTIVITY), ACTIVITY_REQUEST_ADD_IMAGE_TAGS);
    }

    private void startCaptionTranslation(GalleryItemFragment item) {
        PageTitle sourceTitle = new PageTitle(item.getImageTitle().getPrefixedText(), new WikiSite(Service.COMMONS_URL, sourceWiki.languageCode()));
        PageTitle targetTitle = new PageTitle(item.getImageTitle().getPrefixedText(), new WikiSite(Service.COMMONS_URL, StringUtils.defaultString(targetLanguageCode, app.language().getAppLanguageCodes().get(1))));
        String currentCaption = item.getMediaInfo().getCaptions().get(sourceWiki.languageCode());
        if (TextUtils.isEmpty(currentCaption)) {
            currentCaption = StringUtil.fromHtml(item.getMediaInfo().getMetadata().imageDescription()).toString();
        }

        PageSummaryForEdit sourceSummary = new PageSummaryForEdit(sourceTitle.getPrefixedText(), sourceTitle.getWikiSite().languageCode(), sourceTitle,
                sourceTitle.getDisplayText(), currentCaption, item.getMediaInfo().getThumbUrl());

        PageSummaryForEdit targetSummary = new PageSummaryForEdit(targetTitle.getPrefixedText(), targetTitle.getWikiSite().languageCode(), targetTitle,
                targetTitle.getDisplayText(), null, item.getMediaInfo().getThumbUrl());

        startActivityForResult(DescriptionEditActivity.newIntent(this, targetTitle, null, sourceSummary, targetSummary,
                (sourceSummary.getLang().equals(targetSummary.getLang())) ? ADD_CAPTION : TRANSLATE_CAPTION, GALLERY_ACTIVITY),
                ACTIVITY_REQUEST_DESCRIPTION_EDIT);
    }

    @OnClick(R.id.gallery_license_container) void onClick(View v) {
        if (licenseIcon.getContentDescription() == null) {
            return;
        }
        FeedbackUtil.showMessageAsPlainText((Activity) licenseIcon.getContext(), licenseIcon.getContentDescription());
    }

    @OnLongClick(R.id.gallery_license_container) boolean onLongClick(View v) {
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

    private class GalleryPageChangeListener extends ViewPager2.OnPageChangeCallback {
        private int currentPosition = -1;
        @Override
        public void onPageSelected(int position) {
            // the pager has settled on a new position
            layOutGalleryDescription();
            GalleryItemFragment item = getCurrentItem();
            if (currentPosition != -1 && item != null && item.getImageTitle() != null && funnel != null) {
                if (position < currentPosition) {
                    funnel.logGallerySwipeLeft(pageTitle, item.getImageTitle().getDisplayText());
                } else if (position > currentPosition) {
                    funnel.logGallerySwipeRight(pageTitle, item.getImageTitle().getDisplayText());
                }
            }
            currentPosition = position;
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            hideTransitionReceiver(false);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
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
        GalleryItemFragment item = getCurrentItem();
        if (item != null && item.getImageTitle() != null && funnel != null) {
            funnel.logGalleryClose(pageTitle, item.getImageTitle().getDisplayText());
        }

        if (TRANSITION_INFO != null) {
            showTransitionReceiver();
        }

        super.onBackPressed();
    }


    public void onMediaLoaded() {
        hideTransitionReceiver(true);
    }

    private void showTransitionReceiver() {
        transitionReceiver.setVisibility(View.VISIBLE);
    }

    private void hideTransitionReceiver(boolean delay) {
        if (transitionReceiver.getVisibility() == View.GONE) {
            return;
        }
        if (delay) {
            final int hideDelayMillis = 250;
            transitionReceiver.postDelayed(() -> {
                if (isDestroyed() || transitionReceiver == null) {
                    return;
                }
                transitionReceiver.setVisibility(View.GONE);
            }, hideDelayMillis);
        } else {
            transitionReceiver.setVisibility(View.GONE);
        }
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

    public void setViewPagerEnabled(boolean enabled) {
        galleryPager.setUserInputEnabled(enabled);
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
        ClipboardUtil.setPlainText(this, null, title.getUri());
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

        disposables.add(ServiceFactory.getRest(pageTitle.getWikiSite()).getMediaList(pageTitle.getPrefixedText(), revision)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaList -> {
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
        fetchGalleryItems();
    }

    private void applyGalleryList(@NonNull List<MediaListItem> list) {
        // first, verify that the collection contains the item that the user
        // initially requested, if we have one...
        int initialImagePos = -1;
        if (initialFilename != null) {
            for (MediaListItem item : list) {
                // the namespace of a file could be in a different language than English.
                if (StringUtil.removeNamespace(item.getTitle()).equals(StringUtil.removeNamespace(initialFilename))) {
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
        } else if (initialImageIndex >= 0 && initialImageIndex < galleryAdapter.getItemCount()) {
            // if we have a target image index to jump to, then do it!
            galleryPager.setCurrentItem(initialImageIndex, false);
        }
    }

    @Nullable
    private GalleryItemFragment getCurrentItem() {
        return (GalleryItemFragment) galleryAdapter.getFragmentAt(galleryPager.getCurrentItem());
    }

    /**
     * Populate the description and license text fields with data from the current gallery item.
     */
    public void layOutGalleryDescription() {
        GalleryItemFragment item = getCurrentItem();
        if (item == null || item.getImageTitle() == null || item.getMediaInfo() == null || item.getMediaInfo().getMetadata() == null) {
            infoContainer.setVisibility(View.GONE);
            return;
        }
        updateProgressBar(true);
        disposeImageCaptionDisposable();
        imageCaptionDisposable = Observable.zip(MediaHelper.INSTANCE.getImageCaptions(item.getImageTitle().getPrefixedText()),
                ServiceFactory.get(new WikiSite(Service.COMMONS_URL)).getProtectionInfo(item.getImageTitle().getPrefixedText()),
                ImageTagsProvider.getImageTagsObservable(getCurrentItem().getMediaPage().pageId(), sourceWiki.languageCode()), (captions, protectionInfoRsp, imageTags) -> {
                    item.getMediaInfo().setCaptions(captions);
                    return new Pair<>(protectionInfoRsp.query().isEditProtected(), imageTags.size());
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pair -> updateGalleryDescription(pair.first, pair.second), L::e);
    }

    public void updateGalleryDescription(boolean isProtected, int tagsCount) {
        updateProgressBar(false);

        GalleryItemFragment item = getCurrentItem();
        if (item == null || item.getImageTitle() == null || item.getMediaInfo() == null || item.getMediaInfo().getMetadata() == null) {
            infoContainer.setVisibility(View.GONE);
            return;
        }

        displayApplicableDescription(item);

        // Display the Caption Edit button based on whether the image is hosted on Commons,
        // and not the local Wikipedia.
        boolean captionEditable = AccountUtil.isLoggedIn() && item.getMediaInfo().getThumbUrl().contains(Service.URL_FRAGMENT_FROM_COMMONS);
        captionEditButton.setVisibility(captionEditable ? View.VISIBLE : View.GONE);
        captionEditButton.setImageResource(R.drawable.ic_mode_edit_white_24dp);
        captionEditButton.setTag(isProtected);
        if (isProtected) {
            captionEditButton.setImageResource(R.drawable.ic_edit_pencil_locked);
            captionEditable = false;
        }
        if (captionEditable) {
            ctaContainer.setVisibility(View.VISIBLE);
            decideImageEditType(item, tagsCount);
        } else {
            ctaContainer.setVisibility(View.GONE);
        }
        setLicenseInfo(item);
    }

    private void decideImageEditType(@NonNull GalleryItemFragment item, int tagsCount) {
        imageEditType = null;
        if (!item.getMediaInfo().getCaptions().containsKey(sourceWiki.languageCode())) {
            imageEditType = ImageEditType.ADD_CAPTION;
            targetLanguageCode = sourceWiki.languageCode();
            ctaButtonText.setText(getString(R.string.gallery_add_image_caption_button));
            return;
        }

        if (tagsCount == 0) {
            imageEditType = ImageEditType.ADD_TAGS;
            ctaButtonText.setText(getString(R.string.suggested_edits_feed_card_add_image_tags));
            return;
        }

        // and if we have another language in which the caption doesn't exist, then offer
        // it to be translatable.
        if (app.language().getAppLanguageCodes().size() > 1) {
                for (String lang : app.language().getAppLanguageCodes()) {
                    if (!item.getMediaInfo().getCaptions().containsKey(lang)) {
                        targetLanguageCode = lang;
                        imageEditType = ImageEditType.ADD_CAPTION_TRANSLATION;
                        ctaButtonText.setText(getString(R.string.gallery_add_image_caption_in_language_button, app.language().getAppLanguageLocalizedName(targetLanguageCode)));
                        break;
                    }
                }
        }
        ctaContainer.setVisibility(imageEditType == null ? View.GONE : View.VISIBLE);
    }

    private void displayApplicableDescription(@NonNull GalleryItemFragment item) {
        // If we have a structured caption in our current language, then display that instead
        // of the unstructured description, and make it editable.
        CharSequence descriptionStr;
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
    }

    private void setLicenseInfo(@NonNull GalleryItemFragment item) {
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
        DeviceUtil.setContextClickAsLongClick(licenseContainer);

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
    private class GalleryItemAdapter extends PositionAwareFragmentStateAdapter {
        private List<MediaListItem> list = new ArrayList<>();

        GalleryItemAdapter(AppCompatActivity activity) {
            super(activity);
        }

        public void setList(@NonNull List<MediaListItem> list) {
            this.list.clear();
            this.list.addAll(list);
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        @Override
        @NonNull
        public Fragment createFragment(int position) {
            return GalleryItemFragment.newInstance(pageTitle, list.get(position));
        }
    }

    private class MediaDownloadReceiverCallback implements MediaDownloadReceiver.Callback {
        @Override
        public void onSuccess() {
            FeedbackUtil.showMessage(GalleryActivity.this, R.string.gallery_save_success);
        }
    }
}
