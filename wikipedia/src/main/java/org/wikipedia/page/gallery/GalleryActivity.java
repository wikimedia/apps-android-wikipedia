package org.wikipedia.page.gallery;

import org.wikipedia.PageTitle;
import org.wikipedia.R;
import org.wikipedia.Site;
import org.wikipedia.ThemedActionBarActivity;
import org.wikipedia.Utils;
import org.wikipedia.ViewAnimations;
import org.wikipedia.WikipediaApp;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.LinkMovementMethodExt;
import org.wikipedia.page.PageActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import uk.co.senab.photoview.PhotoViewAttacher;
import java.util.Map;

public class GalleryActivity extends ThemedActionBarActivity {
    public static final int ACTIVITY_RESULT_FILEPAGE_SELECT = 1;

    public static final String TAG = "GalleryActivity";
    public static final String EXTRA_PAGETITLE = "pageTitle";
    public static final String EXTRA_IMAGETITLE = "imageTitle";

    private WikipediaApp app;
    private PageTitle pageTitle;

    private PageTitle currentImageTitle;
    private GalleryItem currentGalleryItem;
    private PhotoViewAttacher attacher;

    private View galleryContainer;
    private ImageView mainImage;
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

        galleryContainer = findViewById(R.id.gallery_container);
        mainImage = (ImageView) findViewById(R.id.gallery_image);
        attacher = new PhotoViewAttacher(mainImage);
        attacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
            @Override
            public void onViewTap(View view, float v, float v2) {
                setControlsShowing(!controlsShowing);
            }
        });

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
        currentImageTitle = getIntent().getParcelableExtra(EXTRA_IMAGETITLE);
        Log.d(TAG, "image title: " + currentImageTitle.getPrefixedText());

        if (savedInstanceState != null) {
            controlsShowing = savedInstanceState.getBoolean("controlsShowing");
        }
        toolbarContainer.post(new Runnable() {
            @Override
            public void run() {
                setControlsShowing(controlsShowing);
            }
        });

        updateProgressBar(false, true, 0);
        updateGalleryItem(currentImageTitle);
    }

    @Override
    protected void onStop() {
        super.onStop();
        attacher.cleanup();
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
    }

    public void updateProgressBar(boolean visible, boolean indeterminate, int value) {
        progressBar.setIndeterminate(indeterminate);
        if (!indeterminate) {
            progressBar.setProgress(value);
        }
        progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_gallery, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_gallery_more_info).setEnabled(currentGalleryItem != null);
        menu.findItem(R.id.menu_gallery_visit_page).setEnabled(currentImageTitle != null);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_gallery_more_info:
                if (currentGalleryItem != null) {
                    new DetailsDialog(this, currentGalleryItem, linkMovementMethod,
                                      getWindow().getDecorView().getHeight() / 2).show();
                }
                return true;
            case R.id.menu_gallery_visit_page:
                if (currentGalleryItem != null) {
                    finishWithPageResult(currentImageTitle);
                }
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
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
    private void finishWithPageResult(PageTitle resultTitle) {
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
     * Update all user interface elements to reflect a new gallery item. The image
     * representation of the gallery item will be loaded, and the description and associated
     * metadata will be loaded, as well.
     * @param imageTitle PageTitle of the media item to load as the new gallery item.
     */
    private void updateGalleryItem(final PageTitle imageTitle) {
        updateProgressBar(true, true, 0);
        new GalleryItemFetchTask(app.getPrimarySiteApi(), pageTitle.getSite(), imageTitle) {
            @Override
            public void onFinish(Map<PageTitle, GalleryItem> result) {
                if (result.size() > 0) {
                    currentGalleryItem = (GalleryItem)result.values().toArray()[0];

                    // it's actually OK to use the thumbUrl in all cases, and here's why:
                    // - in the case of a JPG or PNG image:
                    //     - if the image is bigger than the requested thumbnail size, then the
                    //       thumbnail will correctly be a scaled-down version of the image, so
                    //       that it won't overload the device's bitmap buffer.
                    //     - if the image is smaller than the requested thumbnail size, then the
                    //       thumbnail url will be the same as the actual image url.
                    // - in the case of an SVG file:
                    //     - we need the thumbnail image anyway, since the ImageView can't
                    //       display SVGs.
                    // - in the case of OGV videos:
                    //     - definitely need a thumbnail
                    String url = currentGalleryItem.getThumbUrl();
                    Log.d(TAG, "loading from url: " + url);

                    Picasso.with(GalleryActivity.this)
                           .load(url)
                           .into(mainImage, new Callback() {
                               @Override
                               public void onSuccess() {
                                   updateProgressBar(false, true, 0);
                                   attacher.update();
                                   scaleImageToWindow();
                               }

                               @Override
                               public void onError() {
                                   showError(getString(R.string.gallery_error_draw_failed));
                               }
                           });

                    supportInvalidateOptionsMenu();
                    layoutGalleryDescription();
                } else {
                    showError(getString(R.string.error_network_error));
                }
            }
            @Override
            public void onCatch(Throwable caught) {
                showError(getString(R.string.error_network_error));
            }
            private void showError(String errorStr) {
                updateProgressBar(false, true, 0);
                supportInvalidateOptionsMenu();
                Crouton.makeText(GalleryActivity.this, errorStr, Style.ALERT, toolbarContainer)
                       .show();
            }
        }.execute();
    }

    /**
     * If the aspect ratio of the image is *almost* the same as the aspect ratio of our window,
     * then just scale the image to fit, so that we won't see gray bars around the image upon
     * first loading it!
     */
    private void scaleImageToWindow() {
        if (currentGalleryItem.getWidth() == 0 || currentGalleryItem.getHeight() == 0) {
            return;
        }
        final float scaleThreshold = 0.3f;
        float windowAspect = (float) galleryContainer.getWidth()
                             / (float) galleryContainer.getHeight();
        float imageAspect = (float) currentGalleryItem.getWidth()
                            / (float) currentGalleryItem.getHeight();
        if (Math.abs(1.0f - imageAspect / windowAspect) < scaleThreshold) {
            if (windowAspect > imageAspect) {
                attacher.setScale(windowAspect / imageAspect);
            } else {
                attacher.setScale(imageAspect / windowAspect);
            }
        }
    }

    /**
     * Populate the description and license text fields with data from the current gallery item.
     */
    private void layoutGalleryDescription() {
        if (currentGalleryItem == null) {
            infoContainer.setVisibility(View.GONE);
            return;
        }

        CharSequence descriptionStr = "";
        if (currentGalleryItem.getMetadata().containsKey("ImageDescription")) {
            descriptionStr = Html
                    .fromHtml(currentGalleryItem.getMetadata().get("ImageDescription"));
            descriptionText.setText(Utils.trim(descriptionStr));
            descriptionText.setVisibility(View.VISIBLE);
        } else {
            descriptionText.setVisibility(View.GONE);
        }

        CharSequence licenseStr = "";
        if (currentGalleryItem.getMetadata().containsKey("LicenseShortName")) {
            licenseStr = currentGalleryItem.getMetadata().get("LicenseShortName");
        } else if (currentGalleryItem.getMetadata().containsKey("License")) {
            licenseStr = currentGalleryItem.getMetadata().get("License");
        }
        if (!TextUtils.isEmpty(licenseStr)) {
            // is there a license URL? If so, surround the string with it!
            if (currentGalleryItem.getMetadata().containsKey("LicenseUrl")) {
                licenseStr = "<a href=\"" + currentGalleryItem.getMetadata().get("LicenseUrl")
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
        if (currentGalleryItem.getMetadata().containsKey("Artist")) {
            creditStr = String.format(getString(R.string.gallery_credit_text), Utils.trim(
                    Html.fromHtml(currentGalleryItem.getMetadata().get("Artist"))));
            creditText.setText(creditStr);
            creditText.setVisibility(View.VISIBLE);
        } else {
            creditText.setVisibility(View.GONE);
        }

        infoContainer.setVisibility(View.VISIBLE);
        setControlsShowing(true);
    }
}