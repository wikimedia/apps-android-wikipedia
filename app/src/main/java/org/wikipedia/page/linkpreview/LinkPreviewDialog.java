package org.wikipedia.page.linkpreview;

import org.wikipedia.analytics.GalleryFunnel;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.LinkPreviewFunnel;
import org.wikipedia.MainActivity;
import org.wikipedia.page.Page;
import org.wikipedia.page.MainActivityLongPressHandler;
import org.wikipedia.page.PageCache;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.gallery.GalleryActivity;
import org.wikipedia.page.gallery.GalleryCollection;
import org.wikipedia.page.gallery.GalleryCollectionFetchTask;
import org.wikipedia.page.gallery.GalleryThumbnailScrollView;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.savedpages.LoadSavedPageTask;
import org.wikipedia.server.PageServiceFactory;
import org.wikipedia.server.PageSummary;
import org.wikipedia.util.ApiUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.UriUtil;
import org.wikipedia.views.ViewUtil;

import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import static org.wikipedia.util.L10nUtil.getStringForArticleLanguage;
import static org.wikipedia.util.L10nUtil.setConditionalLayoutDirection;

public class LinkPreviewDialog extends SwipeableBottomDialog implements DialogInterface.OnDismissListener {
    private static final String TAG = "LinkPreviewDialog";

    private boolean navigateSuccess = false;

    private ProgressBar progressBar;
    private TextView extractText;
    private SimpleDraweeView thumbnailView;
    private GalleryThumbnailScrollView thumbnailGallery;
    private View toolbarView;
    private View overflowButton;
    private Button goButton;

    private PageTitle pageTitle;
    private int entrySource;
    @Nullable private Location location;

    private LinkPreviewFunnel funnel;
    private LinkPreviewContents contents;
    private OnNavigateListener onNavigateListener;
    private LongPressHandler overflowMenuHandler;

    private GalleryThumbnailScrollView.GalleryViewListener galleryViewListener
            = new GalleryThumbnailScrollView.GalleryViewListener() {
        @Override
        public void onGalleryItemClicked(String imageName) {
            PageTitle imageTitle = new PageTitle(imageName, pageTitle.getSite());
            GalleryActivity.showGallery(getActivity(), pageTitle, imageTitle,
                    GalleryFunnel.SOURCE_LINK_PREVIEW);
        }
    };

    private View.OnClickListener goToPageListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            goToLinkedPage();
        }
    };

    private View.OnClickListener getDirectionsListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            goToExternalMapsApp();
        }
    };

    public static LinkPreviewDialog newInstance(PageTitle title, int entrySource, @Nullable Location location) {
        LinkPreviewDialog dialog = new LinkPreviewDialog();
        Bundle args = new Bundle();
        args.putParcelable("title", title);
        args.putInt("entrySource", entrySource);
        if (location != null) {
            args.putParcelable("location", location);
        }
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, R.style.LinkPreviewDialog);
        setContentPeekHeight((int) getResources().getDimension(R.dimen.bottomSheetPeekHeight));
    }

    @Override
    protected View inflateDialogView(LayoutInflater inflater, ViewGroup container) {
        WikipediaApp app = WikipediaApp.getInstance();
        boolean shouldLoadImages = app.isImageDownloadEnabled();
        pageTitle = getArguments().getParcelable("title");
        entrySource = getArguments().getInt("entrySource");
        location = getArguments().getParcelable("location");

        View rootView = inflater.inflate(R.layout.dialog_link_preview, container);
        progressBar = (ProgressBar) rootView.findViewById(R.id.link_preview_progress);
        toolbarView = rootView.findViewById(R.id.link_preview_toolbar);
        toolbarView.setOnClickListener(goToPageListener);

        View overlayRootView = addOverlay(inflater, R.layout.dialog_link_preview_overlay);
        goButton = (Button) overlayRootView.findViewById(R.id.link_preview_go_button);
        goButton.setOnClickListener(goToPageListener);
        goButton.setText(getStringForArticleLanguage(pageTitle, R.string.button_continue_to_article));

        Button directionsButton = (Button) overlayRootView.findViewById(R.id.link_preview_directions_button);
        if (location != null) {
            directionsButton.setOnClickListener(getDirectionsListener);
        } else {
            directionsButton.setVisibility(View.GONE);
        }

        TextView titleText = (TextView) rootView.findViewById(R.id.link_preview_title);
        titleText.setText(pageTitle.getDisplayText());
        setConditionalLayoutDirection(rootView, pageTitle.getSite().languageCode());
        if (!ApiUtil.hasKitKat()) {
            // for oldish devices, reset line spacing to 1, since it truncates the descenders.
            titleText.setLineSpacing(0, 1.0f);
        } else if (!ApiUtil.hasLollipop()) {
            // for <5.0, give the title a bit more bottom padding, since these versions
            // incorrectly cut off the bottom of the text when line spacing is <1.
            final int bottomPadding = 8;
            ViewUtil.setBottomPaddingDp(titleText, bottomPadding);
        }

        onNavigateListener = new DefaultOnNavigateListener();
        extractText = (TextView) rootView.findViewById(R.id.link_preview_extract);
        thumbnailView = (SimpleDraweeView) rootView.findViewById(R.id.link_preview_thumbnail);

        thumbnailGallery = (GalleryThumbnailScrollView) rootView.findViewById(R.id.link_preview_thumbnail_gallery);
        if (shouldLoadImages) {
            new GalleryThumbnailFetchTask(pageTitle).execute();
            thumbnailGallery.setGalleryViewListener(galleryViewListener);
        }

        overflowButton = rootView.findViewById(R.id.link_preview_overflow_button);
        overflowButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(getActivity(), overflowButton);
                popupMenu.inflate(R.menu.menu_link_preview);
                popupMenu.setOnMenuItemClickListener(menuListener);
                popupMenu.show();
            }
        });

        // show the progress bar while we load content...
        progressBar.setVisibility(View.VISIBLE);

        // and kick off the task to load all the things...
        loadContent();

        funnel = new LinkPreviewFunnel(app, entrySource);
        funnel.logLinkClick();

        return rootView;
    }

    public interface OnNavigateListener {
        void onNavigate(PageTitle title);
    }

    public void goToLinkedPage() {
        navigateSuccess = true;
        funnel.logNavigate();
        if (getDialog() != null) {
            getDialog().dismiss();
        }
        if (onNavigateListener != null) {
            onNavigateListener.onNavigate(pageTitle);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        overflowMenuHandler = new LongPressHandler(getMainActivity());
    }

    @Override
    public void onDestroyView() {
        thumbnailGallery.setGalleryViewListener(null);
        toolbarView.setOnClickListener(null);
        overflowButton.setOnClickListener(null);
        goButton.setOnClickListener(null);
        super.onDestroyView();
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        super.onDismiss(dialogInterface);
        if (!navigateSuccess) {
            funnel.logCancel();
        }
    }

    private void loadContent() {
        PageServiceFactory.create(pageTitle.getSite()).pageSummary(
                pageTitle.getPrefixedText(),
                linkPreviewOnLoadCallback);
    }

    private void loadContentFromCache() {
        Log.v(TAG, "Loading link preview from cache");
        getApplication().getPageCache()
                .get(pageTitle, 0, new PageCache.CacheGetListener() {
                    @Override
                    public void onGetComplete(Page page, int sequence) {
                        if (!isAdded()) {
                            return;
                        }
                        if (page != null) {
                            displayPreviewFromCachedPage(page);
                        } else {
                            loadContentFromSavedPage();
                        }
                    }

                    @Override
                    public void onGetError(Throwable e, int sequence) {
                        if (!isAdded()) {
                            return;
                        }
                        Log.e(TAG, "Failed to get page from cache.", e);
                        loadContentFromSavedPage();
                    }
                });
    }

    private void loadContentFromSavedPage() {
        Log.v(TAG, "Loading link preview from Saved Pages");
        new LoadSavedPageTask(pageTitle) {
            @Override
            public void onFinish(Page page) {
                if (!isAdded()) {
                    return;
                }
                displayPreviewFromCachedPage(page);
            }

            @Override
            public void onCatch(Throwable caught) {
                if (!isAdded()) {
                    return;
                }
                progressBar.setVisibility(View.GONE);
                FeedbackUtil.showMessage(getActivity(), R.string.error_network_error);
                dismiss();
            }
        }.execute();
    }

    private void displayPreviewFromCachedPage(Page page) {
        progressBar.setVisibility(View.GONE);
        contents = new LinkPreviewContents(page);
        layoutPreview();
    }

    private PageSummary.Callback linkPreviewOnLoadCallback = new PageSummary.Callback() {
        @Override
        public void success(PageSummary pageSummary) {
            if (!isAdded()) {
                return;
            }
            if (!pageSummary.hasError()) {
                progressBar.setVisibility(View.GONE);
                contents = new LinkPreviewContents(pageSummary, pageTitle.getSite());
                layoutPreview();
            } else {
                pageSummary.logError("Page summary request failed");
                loadContentFromCache();
            }
        }

        @Override
        public void failure(Throwable throwable) {
            if (!isAdded()) {
                return;
            }
            Log.e(TAG, "Link preview fetch error: " + throwable);
        }
    };

    private MainActivity getMainActivity() {
        return (MainActivity) getActivity();
    }

    private WikipediaApp getApplication() {
        return WikipediaApp.getInstance();
    }

    private PopupMenu.OnMenuItemClickListener menuListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.menu_link_preview_open_in_new_tab:
                    overflowMenuHandler.onOpenInNewTab(pageTitle,
                            new HistoryEntry(pageTitle, entrySource));
                    dismiss();
                    return true;
                case R.id.menu_link_preview_add_to_list:
                    overflowMenuHandler.onAddToList(pageTitle,
                            AddToReadingListDialog.InvokeSource.LINK_PREVIEW_MENU);
                    return true;
                case R.id.menu_link_preview_share_page:
                    overflowMenuHandler.onShareLink(pageTitle);
                    return true;
                case R.id.menu_link_preview_copy_link:
                    overflowMenuHandler.onCopyLink(pageTitle);
                    dismiss();
                    return true;
                default:
                    break;
            }
            return false;
        }
    };

    private class DefaultOnNavigateListener implements OnNavigateListener {
        @Override
        public void onNavigate(PageTitle title) {
            HistoryEntry newEntry = new HistoryEntry(title, entrySource);
            getMainActivity().loadPage(title, newEntry);
        }
    }

    private void layoutPreview() {
        if (contents.getExtract().length() > 0) {
            extractText.setText(contents.getExtract());
        }
        ViewUtil.loadImageUrlInto(thumbnailView, contents.getTitle().getThumbUrl());
    }

    private class GalleryThumbnailFetchTask extends GalleryCollectionFetchTask {
        GalleryThumbnailFetchTask(PageTitle title) {
            super(WikipediaApp.getInstance().getAPIForSite(title.getSite()), title.getSite(), title,
                    true);
        }

        @Override
        public void onGalleryResult(GalleryCollection result) {
            if (!result.getItemList().isEmpty()) {
                thumbnailGallery.setGalleryCollection(result);

                // When the visibility is immediately changed, the images flicker. Add a short delay.
                final int animationDelayMillis = 100;
                thumbnailGallery.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        thumbnailGallery.setVisibility(View.VISIBLE);
                    }
                }, animationDelayMillis);
            }
        }

        @Override
        public void onCatch(Throwable caught) {
            // Don't worry about showing a notification to the user if this fails.
            Log.w(TAG, "Failed to fetch gallery collection.", caught);
        }
    }

    private class LongPressHandler extends MainActivityLongPressHandler {
        LongPressHandler(@NonNull MainActivity activity) {
            super(activity);
        }
    }

    private void goToExternalMapsApp() {
        if (location != null) {
            dismiss();
            UriUtil.sendGeoIntent(getActivity(), location, pageTitle.getDisplayText());
        }
    }
}
