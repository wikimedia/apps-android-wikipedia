package org.wikipedia.page.gallery;

import org.wikipedia.PageTitle;
import org.wikipedia.R;
import org.wikipedia.ThemedActionBarActivity;
import org.wikipedia.ViewAnimations;
import org.wikipedia.WikipediaApp;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;
import java.util.Map;

public class GalleryActivity extends ThemedActionBarActivity {
    public static final String TAG = "GalleryActivity";
    public static final String EXTRA_PAGETITLE = "pageTitle";
    public static final String EXTRA_IMAGETITLE = "imageTitle";

    private WikipediaApp app;
    private PageTitle pageTitle;

    private ImageView mainImage;
    private ViewGroup toolbarContainer;
    private ViewGroup infoContainer;
    private ProgressBar progressBar;

    boolean controlsShowing = true;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // force the theme to dark...
        setTheme(WikipediaApp.THEME_DARK);
        app = (WikipediaApp)getApplicationContext();

        setContentView(R.layout.activity_gallery);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.gallery_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("");

        mainImage = (ImageView) findViewById(R.id.gallery_image);
        toolbarContainer = (ViewGroup) findViewById(R.id.gallery_toolbar_container);
        infoContainer = (ViewGroup) findViewById(R.id.gallery_info_container);
        progressBar = (ProgressBar) findViewById(R.id.gallery_progressbar);

        pageTitle = getIntent().getParcelableExtra(EXTRA_PAGETITLE);
        PageTitle imageTitle = getIntent().getParcelableExtra(EXTRA_IMAGETITLE);
        Log.d(TAG, "image title: " + imageTitle.getPrefixedText());

        if (savedInstanceState != null) {
            controlsShowing = savedInstanceState.getBoolean("controlsShowing");
        }
        toolbarContainer.post(new Runnable() {
            @Override
            public void run() {
                setControlsShowing(controlsShowing);
            }
        });

        mainImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setControlsShowing(!controlsShowing);
            }
        });

        updateProgressBar(false, true, 0);
        updateGalleryItem(imageTitle);
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
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_gallery_more_info:
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

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

    private void updateGalleryItem(final PageTitle imageTitle) {
        updateProgressBar(true, true, 0);
        new GalleryItemFetchTask(app.getPrimarySiteApi(), pageTitle.getSite(), imageTitle) {
            @Override
            public void onFinish(Map<PageTitle, GalleryItem> result) {
                if (result.size() > 0) {
                    GalleryItem item = (GalleryItem)result.values().toArray()[0];

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
                    String url = item.getThumbUrl();
                    Log.d(TAG, "loading from url: " + url);

                    Picasso.with(GalleryActivity.this)
                           .load(url)
                           .into(mainImage, new Callback() {
                               @Override
                               public void onSuccess() {
                                   updateProgressBar(false, true, 0);
                               }
                               @Override
                               public void onError() {
                                   showError(getString(R.string.gallery_error_draw_failed));
                               }
                           });

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
                Crouton.makeText(GalleryActivity.this, errorStr, Style.ALERT, toolbarContainer)
                       .show();
            }
        }.execute();
    }
}