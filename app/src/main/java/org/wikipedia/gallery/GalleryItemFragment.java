package org.wikipedia.gallery;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.chrisbanes.photoview.PhotoView;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.commons.FilePageActivity;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.page.Namespace;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.FileUtil;
import org.wikipedia.util.ImageUrlUtil;
import org.wikipedia.util.PermissionUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.ViewUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static org.wikipedia.Constants.PREFERRED_GALLERY_IMAGE_SIZE;
import static org.wikipedia.util.PermissionUtil.hasWriteExternalStoragePermission;
import static org.wikipedia.util.PermissionUtil.requestWriteStorageRuntimePermissions;

public class GalleryItemFragment extends Fragment implements RequestListener<Drawable> {
    private static final String ARG_PAGETITLE = "pageTitle";
    private static final String ARG_GALLERY_ITEM = "galleryItem";

    public interface Callback {
        void onDownload(@NonNull GalleryItemFragment item);
        void onShare(@NonNull GalleryItemFragment item, @Nullable Bitmap bitmap, @NonNull String subject, @NonNull PageTitle title);
    }

    @BindView(R.id.gallery_item_progress_bar) ProgressBar progressBar;
    @BindView(R.id.gallery_video_container) View videoContainer;
    @BindView(R.id.gallery_video) VideoView videoView;
    @BindView(R.id.gallery_video_thumbnail) ImageView videoThumbnail;
    @BindView(R.id.gallery_video_play_button) View videoPlayButton;
    @BindView(R.id.gallery_image) PhotoView imageView;
    @Nullable private Unbinder unbinder;
    private CompositeDisposable disposables = new CompositeDisposable();

    private MediaController mediaController;

    @Nullable private PageTitle pageTitle;
    @Nullable private MediaListItem mediaListItem;

    @Nullable private PageTitle imageTitle;
    @Nullable PageTitle getImageTitle() {
        return imageTitle;
    }

    @Nullable ImageInfo getMediaInfo() {
        return mediaPage != null ? mediaPage.imageInfo() : null;
    }

    @Nullable private MwQueryPage mediaPage;
    @Nullable public MwQueryPage getMediaPage() {
        return mediaPage;
    }


    public static GalleryItemFragment newInstance(@Nullable PageTitle pageTitle, @NonNull MediaListItem item) {
        GalleryItemFragment f = new GalleryItemFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_PAGETITLE, pageTitle);
        args.putSerializable(ARG_GALLERY_ITEM, item);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mediaListItem = (MediaListItem) getArguments().getSerializable(ARG_GALLERY_ITEM);
        pageTitle = getArguments().getParcelable(ARG_PAGETITLE);
        if (pageTitle == null) {
            pageTitle = new PageTitle(mediaListItem.getTitle(), new WikiSite(Service.COMMONS_URL));
        }
        imageTitle = new PageTitle(Namespace.FILE.toLegacyString(),
                StringUtil.removeNamespace(mediaListItem.getTitle()),
                pageTitle.getWikiSite());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_gallery_item, container, false);
        unbinder = ButterKnife.bind(this, rootView);

        imageView.setOnClickListener(v -> {
            if (isAdded()) {
                ((GalleryActivity) requireActivity()).toggleControls();
            }
        });

        imageView.setOnMatrixChangeListener(rect -> {
            if (!isAdded() || imageView == null) {
                return;
            }
            ((GalleryActivity) requireActivity()).setViewPagerEnabled(imageView.getScale() <= 1f);
        });

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        loadMedia();
    }

    @Override
    public void onDestroyView() {
        disposables.clear();
        imageView.setOnClickListener(null);
        videoThumbnail.setOnClickListener(null);
        if (unbinder != null) {
            unbinder.unbind();
            unbinder = null;
        }
        super.onDestroyView();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mediaController != null) {
            if (videoView.isPlaying()) {
                videoView.pause();
            }
            mediaController.hide();
        }
    }

    private void updateProgressBar(boolean visible) {
        progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (!isAdded()) {
            return;
        }
        menu.findItem(R.id.menu_gallery_visit_image_page).setEnabled(getMediaInfo() != null);
        menu.findItem(R.id.menu_gallery_share).setEnabled(getMediaInfo() != null
                && !TextUtils.isEmpty(getMediaInfo().getThumbUrl()) && imageView.getDrawable() != null);
        menu.findItem(R.id.menu_gallery_save).setEnabled(getMediaInfo() != null
                && !TextUtils.isEmpty(getMediaInfo().getThumbUrl()) && imageView.getDrawable() != null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_gallery_visit_image_page:
                if (getMediaInfo() != null && imageTitle != null) {
                    startActivity(FilePageActivity.newIntent(requireContext(), imageTitle));
                }
                return true;
            case R.id.menu_gallery_save:
                handleImageSaveRequest();
                return true;
            case R.id.menu_gallery_share:
                shareImage();
                return true;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void handleImageSaveRequest() {
        if (!(hasWriteExternalStoragePermission(requireActivity()))) {
            requestWriteExternalStoragePermission();
        } else {
            saveImage();
        }
    }

    private void requestWriteExternalStoragePermission() {
        requestWriteStorageRuntimePermissions(this,
                Constants.ACTIVITY_REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION);
    }

    /**
     * Load the actual media associated with our gallery item into the UI.
     */
    private void loadMedia() {
        if (pageTitle == null || mediaListItem == null) {
            return;
        }
        updateProgressBar(true);
        disposables.add(getMediaInfoDisposable(mediaListItem.getTitle(), WikipediaApp.getInstance().getAppOrSystemLanguageCode())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate(() -> {
                    updateProgressBar(false);
                    requireActivity().invalidateOptionsMenu();
                    ((GalleryActivity) requireActivity()).layOutGalleryDescription();
                })
                .subscribe(response -> {
                    mediaPage = response.query().firstPage();
                    if (FileUtil.isVideo(mediaListItem.getType())) {
                        loadVideo();
                    } else {
                        loadImage(ImageUrlUtil.getUrlForPreferredSize(getMediaInfo().getThumbUrl(), PREFERRED_GALLERY_IMAGE_SIZE));
                    }
                }, throwable -> {
                    FeedbackUtil.showMessage(getActivity(), R.string.gallery_error_draw_failed);
                    L.d(throwable);
                }));
    }

    private Observable<MwQueryResponse> getMediaInfoDisposable(String title, String lang) {
        if (FileUtil.isVideo(mediaListItem.getType())) {
            return ServiceFactory.get(mediaListItem.isInCommons() ? new WikiSite(Service.COMMONS_URL) : pageTitle.getWikiSite()).getVideoInfo(title, lang);
        } else {
            return ServiceFactory.get(mediaListItem.isInCommons() ? new WikiSite(Service.COMMONS_URL) : pageTitle.getWikiSite()).getImageInfo(title, lang);
        }
    }

    private View.OnClickListener videoThumbnailClickListener = new View.OnClickListener() {
        private boolean loading = false;

        @Override
        public void onClick(View v) {
            if (loading || getMediaInfo() == null || getMediaInfo().getBestDerivative() == null) {
                return;
            }
            loading = true;
            L.d("Loading video from url: " + getMediaInfo().getBestDerivative().getSrc());
            videoView.setVisibility(View.VISIBLE);
            mediaController = new MediaController(requireActivity());
            if (!DeviceUtil.isNavigationBarShowing()) {
                mediaController.setPadding(0, 0, 0, (int) DimenUtil.dpToPx(DimenUtil.getNavigationBarHeight(requireContext())));
            }
            updateProgressBar(true);
            videoView.setMediaController(mediaController);
            videoView.setOnPreparedListener((mp) -> {
                updateProgressBar(false);
                // ...update the parent activity, which will trigger us to start playing!
                ((GalleryActivity) requireActivity()).layOutGalleryDescription();
                // hide the video thumbnail, since we're about to start playback
                videoThumbnail.setVisibility(View.GONE);
                videoPlayButton.setVisibility(View.GONE);
                // and start!
                videoView.start();
                loading = false;
            });
            videoView.setOnErrorListener((mp, what, extra) -> {
                updateProgressBar(false);
                FeedbackUtil.showMessage(getActivity(),
                        R.string.gallery_error_video_failed);
                videoView.setVisibility(View.GONE);
                videoThumbnail.setVisibility(View.VISIBLE);
                videoPlayButton.setVisibility(View.VISIBLE);
                loading = false;
                return true;
            });
            videoView.setVideoURI(Uri.parse(getMediaInfo().getBestDerivative().getSrc()));
        }
    };

    private void loadVideo() {
        videoContainer.setVisibility(View.VISIBLE);
        videoPlayButton.setVisibility(View.VISIBLE);
        videoView.setVisibility(View.GONE);
        if (getMediaInfo() == null || TextUtils.isEmpty(getMediaInfo().getThumbUrl())) {
            videoThumbnail.setVisibility(View.GONE);
        } else {
            // show the video thumbnail while the video loads...
            videoThumbnail.setVisibility(View.VISIBLE);
            ViewUtil.loadImage(videoThumbnail, getMediaInfo().getThumbUrl());
        }
        videoThumbnail.setOnClickListener(videoThumbnailClickListener);
    }

    private void loadImage(String url) {
        imageView.setVisibility(View.INVISIBLE);
        L.v("Loading image from url: " + url);

        updateProgressBar(true);
        ViewUtil.loadImage(imageView, url, false, false, true, this);
        // TODO: show error if loading failed.
    }

    @Override
    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
        ((GalleryActivity) requireActivity()).onMediaLoaded();
        return false;
    }

    @Override
    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
        imageView.setVisibility(View.VISIBLE);
        ((GalleryActivity) requireActivity()).onMediaLoaded();
        return false;
    }

    private void shareImage() {
        if (getMediaInfo() == null) {
            return;
        }
        new ImagePipelineBitmapGetter(ImageUrlUtil.getUrlForPreferredSize(getMediaInfo().getThumbUrl(), PREFERRED_GALLERY_IMAGE_SIZE)){
            @Override
            public void onSuccess(@Nullable Bitmap bitmap) {
                if (!isAdded()) {
                    return;
                }
                if (callback() != null && getShareSubject() != null && imageTitle != null) {
                    callback().onShare(GalleryItemFragment.this, bitmap, getShareSubject(), imageTitle);
                }
            }
        }.get(requireContext());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == Constants.ACTIVITY_REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION) {
            if (PermissionUtil.isPermitted(grantResults)) {
                saveImage();
            } else {
                L.e("Write permission was denied by user");
                FeedbackUtil.showMessage(getActivity(),
                        R.string.gallery_save_image_write_permission_rationale);
            }
        } else {
            throw new RuntimeException("unexpected permission request code " + requestCode);
        }
    }

    @Nullable
    private String getShareSubject() {
        return pageTitle != null ? pageTitle.getDisplayText() : null;
    }

    private void saveImage() {
        if (getMediaInfo() != null && callback() != null) {
            callback().onDownload(this);
        }
    }

    @Nullable private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}
