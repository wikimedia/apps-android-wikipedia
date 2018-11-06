package org.wikipedia.page.linkpreview;

import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.analytics.GalleryFunnel;
import org.wikipedia.analytics.LinkPreviewFunnel;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.page.PageClientFactory;
import org.wikipedia.gallery.GalleryActivity;
import org.wikipedia.gallery.GalleryThumbnailScrollView;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.ExtendedBottomSheetDialogFragment;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.GeoUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.ViewUtil;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static org.wikipedia.settings.Prefs.isImageDownloadEnabled;
import static org.wikipedia.util.L10nUtil.getStringForArticleLanguage;
import static org.wikipedia.util.L10nUtil.setConditionalLayoutDirection;

public class LinkPreviewDialog extends ExtendedBottomSheetDialogFragment
        implements LinkPreviewErrorView.Callback, DialogInterface.OnDismissListener {
    public interface Callback {
        void onLinkPreviewLoadPage(@NonNull PageTitle title, @NonNull HistoryEntry entry, boolean inNewTab);
        void onLinkPreviewCopyLink(@NonNull PageTitle title);
        void onLinkPreviewAddToList(@NonNull PageTitle title);
        void onLinkPreviewShareLink(@NonNull PageTitle title);
    }

    private boolean navigateSuccess = false;

    private LinearLayout dialogContainer;
    private View contentContainer;
    private LinkPreviewErrorView errorContainer;
    private ProgressBar progressBar;
    private TextView extractText;
    private SimpleDraweeView thumbnailView;
    private GalleryThumbnailScrollView thumbnailGallery;
    private LinkPreviewOverlayView overlayView;
    private TextView titleText;
    private View toolbarView;
    private View overflowButton;

    private HistoryEntry historyEntry;
    private PageTitle pageTitle;
    @Nullable private Location location;
    private LinkPreviewFunnel funnel;
    private CompositeDisposable disposables = new CompositeDisposable();

    public static LinkPreviewDialog newInstance(@NonNull HistoryEntry entry, @Nullable Location location) {
        LinkPreviewDialog dialog = new LinkPreviewDialog();
        Bundle args = new Bundle();
        args.putParcelable("entry", entry);
        if (location != null) {
            args.putParcelable("location", location);
        }
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        WikipediaApp app = WikipediaApp.getInstance();
        historyEntry = getArguments().getParcelable("entry");
        pageTitle = historyEntry.getTitle();
        location = getArguments().getParcelable("location");

        View rootView = inflater.inflate(R.layout.dialog_link_preview, container);
        dialogContainer = rootView.findViewById(R.id.dialog_link_preview_container);
        contentContainer = rootView.findViewById(R.id.dialog_link_preview_content_container);
        errorContainer = rootView.findViewById(R.id.dialog_link_preview_error_container);
        progressBar = rootView.findViewById(R.id.link_preview_progress);
        toolbarView = rootView.findViewById(R.id.link_preview_toolbar);
        toolbarView.setOnClickListener(goToPageListener);

        titleText = rootView.findViewById(R.id.link_preview_title);
        setConditionalLayoutDirection(rootView, pageTitle.getWikiSite().languageCode());
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            // for <5.0, give the title a bit more bottom padding, since these versions
            // incorrectly cut off the bottom of the text when line spacing is <1.
            final int bottomPadding = 8;
            ViewUtil.setBottomPaddingDp(titleText, bottomPadding);
        }

        extractText = rootView.findViewById(R.id.link_preview_extract);
        thumbnailView = rootView.findViewById(R.id.link_preview_thumbnail);
        thumbnailGallery = rootView.findViewById(R.id.link_preview_thumbnail_gallery);
        overflowButton = rootView.findViewById(R.id.link_preview_overflow_button);
        overflowButton.setOnClickListener((View v) -> {
            PopupMenu popupMenu = new PopupMenu(requireActivity(), overflowButton);
            popupMenu.inflate(R.menu.menu_link_preview);
            popupMenu.setOnMenuItemClickListener(menuListener);
            popupMenu.show();
        });

        // show the progress bar while we load content...
        progressBar.setVisibility(View.VISIBLE);

        // and kick off the task to load all the things...
        loadContent();

        funnel = new LinkPreviewFunnel(app, historyEntry.getSource());
        funnel.logLinkClick();

        return rootView;
    }

    public void goToLinkedPage(boolean inNewTab) {
        navigateSuccess = true;
        funnel.logNavigate();
        if (getDialog() != null) {
            getDialog().dismiss();
        }
        loadPage(pageTitle, historyEntry, inNewTab);
    }

    @Override public void onResume() {
        super.onResume();
        ViewGroup containerView = getDialog().findViewById(R.id.container);
        if (overlayView == null && containerView != null) {
            overlayView = new LinkPreviewOverlayView(getContext());
            overlayView.setCallback(new OverlayViewCallback());
            overlayView.setPrimaryButtonText(getStringForArticleLanguage(pageTitle, R.string.button_continue_to_article));
            overlayView.setSecondaryButtonText(getStringForArticleLanguage(pageTitle, R.string.menu_long_press_open_in_new_tab));
            overlayView.showTertiaryButton(location != null);
            containerView.addView(overlayView);
        }
    }

    @Override
    public void onDestroyView() {
        disposables.clear();
        thumbnailGallery.setGalleryViewListener(null);
        toolbarView.setOnClickListener(null);
        overflowButton.setOnClickListener(null);
        if (overlayView != null) {
            overlayView.setCallback(null);
            overlayView = null;
        }
        super.onDestroyView();
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        super.onDismiss(dialogInterface);
        if (!navigateSuccess) {
            funnel.logCancel();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, final Intent data) {
        if (requestCode == Constants.ACTIVITY_REQUEST_GALLERY
                && resultCode == GalleryActivity.ACTIVITY_RESULT_PAGE_SELECTED) {
            startActivity(data);
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onAddToList() {
        if (callback() != null) {
            callback().onLinkPreviewAddToList(pageTitle);
        }
    }

    @Override
    public void onDismiss() {
        dismiss();
    }

    private void loadContent() {
        disposables.add(PageClientFactory.create(pageTitle.getWikiSite(), pageTitle.namespace())
                .summary(pageTitle.getWikiSite(), pageTitle.getPrefixedText(), historyEntry.getReferrer())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(summary -> {
                    funnel.setPageId(summary.getPageId());
                    pageTitle.setThumbUrl(summary.getThumbnailUrl());
                    // TODO: Remove this logic once Parsoid starts supporting language variants.
                    if (pageTitle.getWikiSite().languageCode().equals(pageTitle.getWikiSite().subdomain())) {
                        titleText.setText(StringUtil.fromHtml(summary.getDisplayTitle()));
                    } else {
                        titleText.setText(StringUtil.fromHtml(pageTitle.getDisplayText()));
                    }

                    // TODO: remove after the restbase endpoint supports ZH variants
                    pageTitle.setConvertedText(summary.getConvertedTitle());
                    showPreview(new LinkPreviewContents(summary, pageTitle.getWikiSite()));
                }, caught -> {
                    L.e(caught);
                    titleText.setText(StringUtil.fromHtml(pageTitle.getDisplayText()));
                    showError(caught);
                }));
    }

    private void loadGallery() {
        if (isImageDownloadEnabled()) {
            disposables.add(ServiceFactory.getRest(pageTitle.getWikiSite()).getMedia(pageTitle.getConvertedText())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(gallery -> {
                        thumbnailGallery.setGalleryList(gallery.getItems("image", "video"));
                        thumbnailGallery.setGalleryViewListener(galleryViewListener);
                    }, caught -> {
                        // ignore errors
                        L.w("Failed to fetch gallery collection.", caught);
                    }));
        }
    }

    private void showPreview(@NonNull LinkPreviewContents contents) {
        loadGallery();
        progressBar.setVisibility(View.GONE);
        setPreviewContents(contents);
    }

    private void showError(@Nullable Throwable caught) {
        dialogContainer.setLayoutTransition(null);
        dialogContainer.setMinimumHeight(0);
        progressBar.setVisibility(View.GONE);
        contentContainer.setVisibility(View.GONE);
        overlayView.showSecondaryButton(false);
        overlayView.showTertiaryButton(false);
        errorContainer.setVisibility(View.VISIBLE);
        errorContainer.setError(caught);
        errorContainer.setCallback(this);
        LinkPreviewErrorType errorType = LinkPreviewErrorType.get(caught);
        overlayView.setPrimaryButtonText(getResources().getString(errorType.buttonText()));
        overlayView.setCallback(errorType.buttonAction(errorContainer));
        if (errorType != LinkPreviewErrorType.OFFLINE) {
            toolbarView.setOnClickListener(null);
            overflowButton.setVisibility(View.GONE);
        }
    }

    private void setPreviewContents(@NonNull LinkPreviewContents contents) {
        if (contents.getExtract().length() > 0) {
            extractText.setText(contents.getExtract());
        }
        ViewUtil.loadImageUrlInto(thumbnailView, contents.getTitle().getThumbUrl());
        if (overlayView != null) {
            overlayView.setPrimaryButtonText(getStringForArticleLanguage(pageTitle,
                    contents.isDisambiguation() ? R.string.button_continue_to_disambiguation : R.string.button_continue_to_article));
        }
    }

    private PopupMenu.OnMenuItemClickListener menuListener = new PopupMenu.OnMenuItemClickListener() {
        @Override
        public boolean onMenuItemClick(MenuItem item) {
            Callback callback = callback();
            switch (item.getItemId()) {
                case R.id.menu_link_preview_add_to_list:
                    if (callback != null) {
                        callback.onLinkPreviewAddToList(pageTitle);
                    }
                    return true;
                case R.id.menu_link_preview_share_page:
                    if (callback != null) {
                        callback.onLinkPreviewShareLink(pageTitle);
                    }
                    return true;
                case R.id.menu_link_preview_copy_link:
                    if (callback != null) {
                        callback.onLinkPreviewCopyLink(pageTitle);
                    }
                    dismiss();
                    return true;
                default:
                    break;
            }
            return false;
        }
    };

    private GalleryThumbnailScrollView.GalleryViewListener galleryViewListener
            = new GalleryThumbnailScrollView.GalleryViewListener() {
        @Override
        public void onGalleryItemClicked(String imageName) {
            startActivityForResult(GalleryActivity.newIntent(requireContext(), pageTitle, imageName,
                    pageTitle.getWikiSite(), GalleryFunnel.SOURCE_LINK_PREVIEW),
                    Constants.ACTIVITY_REQUEST_GALLERY);
        }
    };

    private View.OnClickListener goToPageListener = (View v) -> goToLinkedPage(false);

    private void goToExternalMapsApp() {
        if (location != null) {
            dismiss();
            GeoUtil.sendGeoIntent(requireActivity(), location, pageTitle.getDisplayText());
        }
    }

    private void loadPage(PageTitle title, HistoryEntry entry, boolean inNewTab) {
        Callback callback = callback();
        if (callback != null) {
            callback.onLinkPreviewLoadPage(title, entry, inNewTab);
        }
    }

    private class OverlayViewCallback implements LinkPreviewOverlayView.Callback {
        @Override
        public void onPrimaryClick() {
            goToLinkedPage(false);
        }

        @Override
        public void onSecondaryClick() {
            goToLinkedPage(true);
        }

        @Override
        public void onTertiaryClick() {
            goToExternalMapsApp();
        }
    }

    @Nullable private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}
