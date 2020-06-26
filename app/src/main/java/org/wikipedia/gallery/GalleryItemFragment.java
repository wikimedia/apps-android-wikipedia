package org.wikipedia.gallery;

import android.graphics.Bitmap;
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

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.commons.FilePageActivity;
import org.wikipedia.databinding.FragmentGalleryItemBinding;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
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

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static org.wikipedia.Constants.PREFERRED_GALLERY_IMAGE_SIZE;
import static org.wikipedia.util.PermissionUtil.hasWriteExternalStoragePermission;
import static org.wikipedia.util.PermissionUtil.requestWriteStorageRuntimePermissions;

public class GalleryItemFragment extends Fragment {
    private static final String ARG_PAGETITLE = "pageTitle";
    private static final String ARG_GALLERY_ITEM = "galleryItem";

    public interface Callback {
        void onDownload(@NonNull GalleryItemFragment item);
        void onShare(@NonNull GalleryItemFragment item, @Nullable Bitmap bitmap, @NonNull String subject, @NonNull PageTitle title);
    }

    private FragmentGalleryItemBinding binding;
    private ProgressBar progressBar;
    private View videoContainer;
    private VideoView videoView;
    private ImageView videoThumbnail;
    private View videoPlayButton;
    private ImageView imageView;
    private CompositeDisposable disposables = new CompositeDisposable();

    private MediaController mediaController;

    @Nullable private PageTitle pageTitle;
    @Nullable private MediaListItem mediaListItem;

    @Nullable private PageTitle imageTitle;
    @Nullable PageTitle getImageTitle() {
        return imageTitle;
    }

    @Nullable private ImageInfo mediaInfo;
    @Nullable ImageInfo getMediaInfo() {
        return mediaInfo;
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
        binding = FragmentGalleryItemBinding.inflate(inflater, container, false);

        progressBar = binding.galleryItemProgressBar;
        videoContainer = binding.galleryVideoContainer;
        videoView = binding.galleryVideo;
        videoThumbnail = binding.galleryVideoThumbnail;
        videoPlayButton = binding.galleryVideoPlayButton;
        imageView = binding.galleryImage;

        imageView.setOnClickListener(v -> {
            if (isAdded()) {
                ((GalleryActivity) requireActivity()).toggleControls();
            }
        });
        return binding.getRoot();
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
        binding = null;
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
        menu.findItem(R.id.menu_gallery_visit_page).setEnabled(mediaInfo != null);
        menu.findItem(R.id.menu_gallery_share).setEnabled(mediaInfo != null
                && !TextUtils.isEmpty(mediaInfo.getThumbUrl()) && imageView.getDrawable() != null);
        menu.findItem(R.id.menu_gallery_save).setEnabled(mediaInfo != null
                && !TextUtils.isEmpty(mediaInfo.getThumbUrl()) && imageView.getDrawable() != null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_gallery_visit_page:
                if (mediaInfo != null && imageTitle != null) {
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
                    mediaInfo = response.query().firstPage().imageInfo();
                    if (FileUtil.isVideo(mediaListItem.getType())) {
                        loadVideo();
                    } else {
                        loadImage(ImageUrlUtil.getUrlForPreferredSize(mediaInfo.getThumbUrl(), PREFERRED_GALLERY_IMAGE_SIZE));
                    }
                }, throwable -> {
                    FeedbackUtil.showMessage(getActivity(), R.string.gallery_error_draw_failed);
                    L.d(throwable);
                }));
    }

    private Observable<MwQueryResponse> getMediaInfoDisposable(String title, String lang) {
        if (FileUtil.isVideo(mediaListItem.getType())) {
            return ServiceFactory.get(pageTitle.getWikiSite()).getVideoInfo(title, lang);
        } else {
            return ServiceFactory.get(pageTitle.getWikiSite()).getImageInfo(title, lang);
        }
    }

    private View.OnClickListener videoThumbnailClickListener = new View.OnClickListener() {
        private boolean loading = false;

        @Override
        public void onClick(View v) {
            if (loading || mediaInfo == null || mediaInfo.getBestDerivative() == null) {
                return;
            }
            loading = true;
            L.d("Loading video from url: " + mediaInfo.getBestDerivative().getSrc());
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
            videoView.setVideoURI(Uri.parse(mediaInfo.getBestDerivative().getSrc()));
        }
    };

    private void loadVideo() {
        videoContainer.setVisibility(View.VISIBLE);
        videoPlayButton.setVisibility(View.VISIBLE);
        videoView.setVisibility(View.GONE);
        if (mediaInfo == null || TextUtils.isEmpty(mediaInfo.getThumbUrl())) {
            videoThumbnail.setVisibility(View.GONE);
        } else {
            // show the video thumbnail while the video loads...
            videoThumbnail.setVisibility(View.VISIBLE);
            ViewUtil.loadImage(videoThumbnail, mediaInfo.getThumbUrl());
        }
        videoThumbnail.setOnClickListener(videoThumbnailClickListener);
    }

    private void loadImage(String url) {
        imageView.setVisibility(View.VISIBLE);
        L.v("Loading image from url: " + url);

        updateProgressBar(true);
        ViewUtil.loadImageWithWhiteBackground(imageView, url);
        // TODO: show error if loading failed.
    }

    private void shareImage() {
        if (mediaInfo == null) {
            return;
        }
        new ImagePipelineBitmapGetter(ImageUrlUtil.getUrlForPreferredSize(mediaInfo.getThumbUrl(), PREFERRED_GALLERY_IMAGE_SIZE)){
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
        if (mediaInfo != null && callback() != null) {
            callback().onDownload(this);
        }
    }

    @Nullable private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}
