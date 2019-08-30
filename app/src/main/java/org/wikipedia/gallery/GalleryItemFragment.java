package org.wikipedia.gallery;

import android.graphics.Bitmap;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.BaseControllerListener;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.generic.GenericDraweeHierarchyBuilder;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.samples.zoomable.DoubleTapGestureListener;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.dataclient.Service;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
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
import org.wikipedia.views.ZoomableDraweeViewWithBackground;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
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

    @BindView(R.id.gallery_item_progress_bar) ProgressBar progressBar;
    @BindView(R.id.gallery_video_container) View videoContainer;
    @BindView(R.id.gallery_video) VideoView videoView;
    @BindView(R.id.gallery_video_thumbnail) SimpleDraweeView videoThumbnail;
    @BindView(R.id.gallery_video_play_button) View videoPlayButton;
    @BindView(R.id.gallery_image) ZoomableDraweeViewWithBackground imageView;
    @Nullable private Unbinder unbinder;
    private CompositeDisposable disposables = new CompositeDisposable();

    private MediaController mediaController;

    @NonNull private WikipediaApp app = WikipediaApp.getInstance();
    @Nullable private GalleryActivity parentActivity;
    @Nullable private PageTitle pageTitle;
    @Nullable private MediaListItem mediaListItem;

    @Nullable private PageTitle imageTitle;
    @Nullable public PageTitle getImageTitle() {
        return imageTitle;
    }

    @Nullable private ImageInfo mediaInfo;
    @Nullable public ImageInfo getMediaInfo() {
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
        View rootView = inflater.inflate(R.layout.fragment_gallery_item, container, false);
        unbinder = ButterKnife.bind(this, rootView);

        imageView.setTapListener(new DoubleTapGestureListener(imageView) {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (isAdded()) {
                    parentActivity.toggleControls();
                }
                return true;
            }
        });

        GenericDraweeHierarchy hierarchy = new GenericDraweeHierarchyBuilder(getResources())
                .setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
                .build();
        imageView.setHierarchy(hierarchy);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
        parentActivity = (GalleryActivity) getActivity();

        loadMedia();
    }

    @Override
    public void onDestroyView() {
        disposables.clear();
        imageView.setController(null);
        imageView.setOnClickListener(null);
        videoThumbnail.setController(null);
        videoThumbnail.setOnClickListener(null);
        if (unbinder != null) {
            unbinder.unbind();
            unbinder = null;
        }
        super.onDestroyView();
    }

    private void updateProgressBar(boolean visible) {
        progressBar.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        app.getRefWatcher().watch(this);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
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
                if (mediaInfo != null) {
                    parentActivity.finishWithPageResult(imageTitle);
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

    /**
     * Notifies this fragment that the current position of its containing ViewPager has changed.
     *
     * @param fragmentPosition This fragment's position in the ViewPager.
     * @param pagerPosition    The pager's current position that is displayed to the user.
     */
    public void onUpdatePosition(int fragmentPosition, int pagerPosition) {
        if (!isAdded()) {
            return;
        }
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

    private void handleImageSaveRequest() {
        if (!(hasWriteExternalStoragePermission(this.getActivity()))) {
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
        updateProgressBar(true);
        disposables.add(ServiceFactory.get(pageTitle.getWikiSite()).getMediaInfo(mediaListItem.getTitle())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doAfterTerminate(() -> {
                    updateProgressBar(false);
                    parentActivity.supportInvalidateOptionsMenu();
                    parentActivity.layOutGalleryDescription();
                })
                .subscribe(response -> {
                    mediaInfo = response.query().firstPage().videoInfo();
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
            mediaController = new MediaController(parentActivity);
            if (!DeviceUtil.isNavigationBarShowing()) {
                mediaController.setPadding(0, 0, 0, (int) DimenUtil.dpToPx(DimenUtil.getNavigationBarHeight(requireContext())));
            }
            updateProgressBar(true);
            videoView.setMediaController(mediaController);
            videoView.setOnPreparedListener((mp) -> {
                updateProgressBar(false);
                // ...update the parent activity, which will trigger us to start playing!
                parentActivity.layOutGalleryDescription();
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
        if (TextUtils.isEmpty(mediaInfo.getThumbUrl())) {
            videoThumbnail.setVisibility(View.GONE);
        } else {
            // show the video thumbnail while the video loads...
            videoThumbnail.setVisibility(View.VISIBLE);
            videoThumbnail.setController(Fresco.newDraweeControllerBuilder()
                    .setUri(mediaInfo.getThumbUrl())
                    .setAutoPlayAnimations(true)
                    .setControllerListener(new BaseControllerListener<com.facebook.imagepipeline.image.ImageInfo>() {
                        @Override
                        public void onFinalImageSet(String id, com.facebook.imagepipeline.image.ImageInfo imageInfo, Animatable animatable) {
                            updateProgressBar(false);
                        }

                        @Override
                        public void onFailure(String id, Throwable throwable) {
                            updateProgressBar(false);
                        }
                    })
                    .build());
        }
        videoThumbnail.setOnClickListener(videoThumbnailClickListener);
    }

    private void loadImage(String url) {
        imageView.setVisibility(View.VISIBLE);
        L.v("Loading image from url: " + url);

        updateProgressBar(true);
        imageView.setDrawBackground(false);
        imageView.setController(Fresco.newDraweeControllerBuilder()
                .setUri(url)
                .setAutoPlayAnimations(true)
                .setControllerListener(new BaseControllerListener<com.facebook.imagepipeline.image.ImageInfo>() {
                    @Override
                    public void onFinalImageSet(String id, com.facebook.imagepipeline.image.ImageInfo imageInfo, Animatable animatable) {
                        imageView.setDrawBackground(true);
                        updateProgressBar(false);
                        parentActivity.supportInvalidateOptionsMenu();
                    }

                    @Override
                    public void onFailure(String id, Throwable throwable) {
                        updateProgressBar(false);
                        FeedbackUtil.showMessage(getActivity(), R.string.gallery_error_draw_failed);
                        L.d(throwable);
                    }
                })
                .build());
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
                if (callback() != null) {
                    callback().onShare(GalleryItemFragment.this, bitmap, getShareSubject(), imageTitle);
                }
            }
        }.get();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.ACTIVITY_REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION:
                if (PermissionUtil.isPermitted(grantResults)) {
                    saveImage();
                } else {
                    L.e("Write permission was denied by user");
                    FeedbackUtil.showMessage(getActivity(),
                            R.string.gallery_save_image_write_permission_rationale);
                }
                break;
            default:
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
