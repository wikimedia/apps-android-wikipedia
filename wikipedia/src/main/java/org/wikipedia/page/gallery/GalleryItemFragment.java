package org.wikipedia.page.gallery;

import org.wikipedia.page.PageTitle;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.concurrency.SaneAsyncTask;
import org.wikipedia.util.ShareUtils;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.VideoView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import uk.co.senab.photoview.PhotoViewAttacher;
import java.util.Map;

public class GalleryItemFragment extends Fragment {
    public static final String TAG = "GalleryItemFragment";
    public static final String ARG_PAGETITLE = "pageTitle";
    public static final String ARG_MEDIATITLE = "imageTitle";
    public static final String ARG_MIMETYPE = "mimeType";

    private WikipediaApp app;
    private GalleryActivity parentActivity;
    private PageTitle pageTitle;
    private PageTitle imageTitle;
    private String mimeType;
    private ProgressBar progressBar;

    private View containerView;
    private PhotoViewAttacher attacher;
    private ImageView imageView;

    private View videoContainer;
    private VideoView videoView;
    private ImageView videoThumbnail;
    private View videoPlayButton;
    private MediaController mediaController;

    private GalleryItem galleryItem;
    public GalleryItem getGalleryItem() {
        return galleryItem;
    }

    public static GalleryItemFragment newInstance(PageTitle pageTitle, GalleryItem galleryItemProto) {
        GalleryItemFragment f = new GalleryItemFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PAGETITLE, pageTitle);
        args.putParcelable(ARG_MEDIATITLE, new PageTitle(galleryItemProto.getName(), pageTitle.getSite()));
        args.putString(ARG_MIMETYPE, galleryItemProto.getMimeType());
        f.setArguments(args);
        return f;
    }

    public GalleryItemFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        app = WikipediaApp.getInstance();
        pageTitle = getArguments().getParcelable(ARG_PAGETITLE);
        imageTitle = getArguments().getParcelable(ARG_MEDIATITLE);
        mimeType = getArguments().getString(ARG_MIMETYPE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_gallery_item, container, false);
        containerView = rootView.findViewById(R.id.gallery_item_container);
        containerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parentActivity.toggleControls();
            }
        });
        progressBar = (ProgressBar) rootView.findViewById(R.id.gallery_item_progress_bar);
        videoContainer = rootView.findViewById(R.id.gallery_video_container);
        videoView = (VideoView) rootView.findViewById(R.id.gallery_video);
        videoThumbnail = (ImageView) rootView.findViewById(R.id.gallery_video_thumbnail);
        videoPlayButton = rootView.findViewById(R.id.gallery_video_play_button);
        imageView = (ImageView) rootView.findViewById(R.id.gallery_image);
        attacher = new PhotoViewAttacher(imageView);
        attacher.setOnViewTapListener(new PhotoViewAttacher.OnViewTapListener() {
            @Override
            public void onViewTap(View view, float v, float v2) {
                parentActivity.toggleControls();
            }
        });
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        parentActivity = (GalleryActivity) getActivity();
        // do we already have a prepopulated item in the gallery cache?
        galleryItem = parentActivity.getGalleryCache().get(imageTitle);
        if (galleryItem == null) {
            loadGalleryItem();
        } else {
            loadMedia();
        }
    }

    @Override
    public void onDestroyView() {
        attacher.cleanup();
        attacher = null;
        super.onDestroyView();
    }

    private void updateProgressBar(boolean visible, boolean indeterminate, int value) {
        progressBar.setIndeterminate(indeterminate);
        if (!indeterminate) {
            progressBar.setProgress(value);
        }
        progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (!isAdded()) {
            return;
        }
        inflater.inflate(R.menu.menu_gallery, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (!isAdded()) {
            return;
        }
        menu.findItem(R.id.menu_gallery_more_info).setEnabled(galleryItem != null);
        menu.findItem(R.id.menu_gallery_visit_page).setEnabled(galleryItem != null);
        menu.findItem(R.id.menu_gallery_share).setEnabled(galleryItem != null
                                                          && imageView.getDrawable() != null);
        menu.findItem(R.id.menu_gallery_save).setEnabled(galleryItem != null
                                                         && imageView.getDrawable() != null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_gallery_more_info:
                if (galleryItem != null) {
                    parentActivity.showDetailsDialog(galleryItem);
                }
                return true;
            case R.id.menu_gallery_visit_page:
                if (galleryItem != null) {
                    parentActivity.finishWithPageResult(imageTitle);
                }
                return true;
            case R.id.menu_gallery_save:
                saveImageToMediaStore();
                return true;
            case R.id.menu_gallery_share:
                shareImage();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean isVideo() {
        return (mimeType.contains("ogg")
                || mimeType.contains("video"));
    }

    /**
     * Notifies this fragment that the current position of its containing ViewPager has changed.
     * @param fragmentPosition This fragment's position in the ViewPager.
     * @param pagerPosition The pager's current position that is displayed to the user.
     */
    public void onUpdatePosition(int fragmentPosition, int pagerPosition) {
        if (fragmentPosition != pagerPosition) {
            // update stuff if our position is not "current" within the ViewPager...
            if (mediaController != null) {
                if (videoView.isPlaying()) {
                    videoView.pause();
                }
                mediaController.hide();
            }
        } else {
            // update stuff if our position is "current"
            if (mediaController != null) {
                if (!videoView.isPlaying()) {
                    videoView.start();
                }
            }
        }
    }

    /**
     * Perform a network request to load information and metadata for our gallery item.
     */
    private void loadGalleryItem() {
        updateProgressBar(true, true, 0);
        new GalleryItemFetchTask(app.getAPIForSite(pageTitle.getSite()),
                pageTitle.getSite(), imageTitle, isVideo()) {
            @Override
            public void onFinish(Map<PageTitle, GalleryItem> result) {
                if (!isAdded()) {
                    return;
                }
                if (result.size() > 0) {
                    galleryItem = (GalleryItem)result.values().toArray()[0];
                    parentActivity.getGalleryCache().put((PageTitle)result.keySet().toArray()[0],
                                                         galleryItem);
                    loadMedia();
                } else {
                    updateProgressBar(false, true, 0);
                    parentActivity.showError(getString(R.string.error_network_error));
                }
            }
            @Override
            public void onCatch(Throwable caught) {
                Log.e("Wikipedia", "caught " + caught.getMessage());
                if (!isAdded()) {
                    return;
                }
                updateProgressBar(false, true, 0);
                parentActivity.showError(getString(R.string.error_network_error));
            }
        }.execute();
    }

    /**
     * Load the actual media associated with our gallery item into the UI.
     */
    private void loadMedia() {
        if (isVideo()) {
            loadVideo();
        } else {
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
            loadImage(galleryItem.getThumbUrl());
        }

        parentActivity.supportInvalidateOptionsMenu();
        parentActivity.layoutGalleryDescription();
    }

    private View.OnClickListener videoThumbnailClickListener = new View.OnClickListener() {
        private boolean loading = false;
        @Override
        public void onClick(View v) {
            if (loading) {
                return;
            }
            loading = true;
            Log.d(TAG, "Loading video from url: " + galleryItem.getUrl());
            videoView.setVisibility(View.VISIBLE);
            mediaController = new MediaController(parentActivity);
            updateProgressBar(true, true, 0);
            videoView.setMediaController(mediaController);
            videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    updateProgressBar(false, true, 0);
                    // ...update the parent activity, which will trigger us to start playing!
                    parentActivity.layoutGalleryDescription();
                    // hide the video thumbnail, since we're about to start playback
                    videoThumbnail.setVisibility(View.GONE);
                    videoPlayButton.setVisibility(View.GONE);
                    // and start!
                    videoView.start();
                    loading = false;
                }
            });
            videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    updateProgressBar(false, true, 0);
                    parentActivity.showError(getString(R.string.gallery_error_video_failed));
                    videoView.setVisibility(View.GONE);
                    videoThumbnail.setVisibility(View.VISIBLE);
                    videoPlayButton.setVisibility(View.VISIBLE);
                    loading = false;
                    return true;
                }
            });
            videoView.setVideoURI(Uri.parse(galleryItem.getUrl()));
        }
    };

    private void loadVideo() {
        videoContainer.setVisibility(View.VISIBLE);
        videoPlayButton.setVisibility(View.VISIBLE);
        videoView.setVisibility(View.GONE);
        if (TextUtils.isEmpty(galleryItem.getThumbUrl())) {
            videoThumbnail.setVisibility(View.GONE);
        } else {
            // show the video thumbnail while the video loads...
            videoThumbnail.setVisibility(View.VISIBLE);
            Picasso.with(parentActivity)
                   .load(galleryItem.getThumbUrl())
                   .into(videoThumbnail, new Callback() {
                       @Override
                       public void onSuccess() {
                           updateProgressBar(false, true, 0);
                       }

                       @Override
                       public void onError() {
                           updateProgressBar(false, true, 0);
                       }
                   });
        }
        videoThumbnail.setOnClickListener(videoThumbnailClickListener);
    }

    private void loadImage(String url) {
        imageView.setVisibility(View.VISIBLE);
        Log.d(TAG, "Loading image from url: " + url);
        Picasso.with(parentActivity)
               .load(url)
               .into(imageView, new Callback() {
                   @Override
                   public void onSuccess() {
                       if (!isAdded()) {
                           return;
                       }
                       updateProgressBar(false, true, 0);
                       // if it's an SVG or PNG, give it a white background, since most
                       // images with transparency were intended to be viewed on white.
                       if (galleryItem.getMimeType().contains("svg")
                           || galleryItem.getMimeType().contains("png")) {
                           imageView.setBackgroundColor(Color.WHITE);
                       }
                       attacher.update();
                       attacher.setZoomable(true);
                       scaleImageToWindow();
                       parentActivity.supportInvalidateOptionsMenu();
                   }

                   @Override
                   public void onError() {
                       if (!isAdded()) {
                           return;
                       }
                       updateProgressBar(false, true, 0);
                       parentActivity.showError(getString(R.string.gallery_error_draw_failed));
                   }
               });
    }

    /**
     * If the aspect ratio of the image is *almost* the same as the aspect ratio of our window,
     * then just scale the image to fit, so that we won't see gray bars around the image upon
     * first loading it!
     */
    private void scaleImageToWindow() {
        if (galleryItem.getWidth() == 0 || galleryItem.getHeight() == 0) {
            return;
        }
        final float scaleThreshold = 0.25f;
        float windowAspect = (float) containerView.getWidth()
                             / (float) containerView.getHeight();
        float imageAspect = (float) galleryItem.getWidth()
                            / (float) galleryItem.getHeight();
        if (Math.abs(1.0f - imageAspect / windowAspect) < scaleThreshold) {
            if (windowAspect > imageAspect) {
                attacher.setScale(windowAspect / imageAspect);
            } else {
                attacher.setScale(imageAspect / windowAspect);
            }
        }
    }

    /**
     * Share the current image using an activity chooser, so that the user can choose the
     * app with which to share the content.
     * This is done by saving the image to a temporary file in external storage, then specifying
     * that file in the share intent. The name of the temporary file is kept constant, so that
     * it's overwritten every time an image is shared from the app, so that it takes up a
     * constant amount of space.
     */
    private void shareImage() {
        if (galleryItem == null) {
            return;
        }
        parentActivity.getFunnel().logGalleryShare(pageTitle, galleryItem.getName());
        ShareUtils.shareImage(parentActivity,
                ((BitmapDrawable) imageView.getDrawable()).getBitmap(),
                new java.io.File(galleryItem.getUrl()).getName(),
                pageTitle.getDisplayText(),
                "",
                false);
    }

    /**
     * Save the current image to the MediaStore of the local device ("Photos" / "Gallery" / etc).
     */
    private void saveImageToMediaStore() {
        if (galleryItem == null) {
            return;
        }
        parentActivity.getFunnel().logGallerySave(pageTitle, galleryItem.getName());
        new SaneAsyncTask<Void>(SaneAsyncTask.SINGLE_THREAD) {
            @Override
            public Void performTask() throws Throwable {
                String url = MediaStore.Images.Media.insertImage(parentActivity.getContentResolver(),
                                ((BitmapDrawable) imageView.getDrawable()).getBitmap(),
                                pageTitle.getDisplayText(), galleryItem.getName());
                if (url == null) {
                    throw new RuntimeException(getString(R.string.gallery_save_error_mediastore));
                }
                return null;
            }
            @Override
            public void onFinish(Void result) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(parentActivity, getString(R.string.gallery_save_success),
                        Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onCatch(Throwable caught) {
                if (!isAdded()) {
                    return;
                }
                parentActivity.showError(String.format(getString(R.string.gallery_save_error),
                                        caught.getLocalizedMessage()));
            }
        }.execute();
    }

}