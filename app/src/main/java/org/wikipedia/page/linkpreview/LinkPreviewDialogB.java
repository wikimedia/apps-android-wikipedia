package org.wikipedia.page.linkpreview;

import org.wikipedia.history.HistoryEntry;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.LinkPreviewFunnel;
import org.wikipedia.page.Page;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageCache;
import org.wikipedia.page.PageTitle;
import org.wikipedia.savedpages.LoadSavedPageTask;
import org.wikipedia.server.PageServiceFactory;
import org.wikipedia.server.PageSummary;
import org.wikipedia.util.ApiUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ResourceUtil;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import retrofit.RetrofitError;
import retrofit.client.Response;

import static org.wikipedia.util.L10nUtil.getStringForArticleLanguage;
import static org.wikipedia.util.L10nUtil.setConditionalLayoutDirection;

public class LinkPreviewDialogB extends SwipeableBottomDialog implements DialogInterface.OnDismissListener {
    private static final String TAG = "LinkPreviewDialog";

    private boolean navigateSuccess;

    private ProgressBar progressBar;
    private TextView extractText;
    private ImageView leadImage;

    private PageTitle pageTitle;
    private int entrySource;

    private LinkPreviewFunnel funnel;
    private LinkPreviewContents contents;
    private OnNavigateListener onNavigateListener;

    private View.OnClickListener goToPageListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            goToLinkedPage();
        }
    };

    public static LinkPreviewDialogB newInstance(PageTitle title, int entrySource) {
        LinkPreviewDialogB dialog = new LinkPreviewDialogB();
        Bundle args = new Bundle();
        args.putParcelable("title", title);
        args.putInt("entrySource", entrySource);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, R.style.LinkPreviewDialog);
        setContentPeekHeight((int) getResources().getDimension(R.dimen.linkPreviewPeekHeight));
    }

    @Override
    protected View inflateDialogView(LayoutInflater inflater, ViewGroup container) {
        WikipediaApp app = WikipediaApp.getInstance();
        pageTitle = getArguments().getParcelable("title");
        entrySource = getArguments().getInt("entrySource");

        View rootView = inflater.inflate(R.layout.dialog_link_preview_b, container);
        progressBar = (ProgressBar) rootView.findViewById(R.id.link_preview_progress);
        leadImage = (ImageView) rootView.findViewById(R.id.link_preview_lead_image);

        View overlayRootView = addOverlay(inflater, R.layout.dialog_link_preview_overlay);
        Button goButton = (Button) overlayRootView.findViewById(R.id.link_preview_go_button);
        goButton.setOnClickListener(goToPageListener);
        goButton.setText(getStringForArticleLanguage(pageTitle, R.string.button_continue_to_article));

        TextView titleText = (TextView) rootView.findViewById(R.id.link_preview_title);
        titleText.setOnClickListener(goToPageListener);
        titleText.setText(pageTitle.getDisplayText());
        setConditionalLayoutDirection(rootView, pageTitle.getSite().getLanguageCode());
        if (!ApiUtil.hasKitKat()) {
            // for oldish devices, reset line spacing to 1, since it truncates the descenders.
            titleText.setLineSpacing(0, 1.0f);
        }

        onNavigateListener = new DefaultOnNavigateListener();
        extractText = (TextView) rootView.findViewById(R.id.link_preview_extract);
        extractText.setMovementMethod(new ScrollingMovementMethod());

        // show the progress bar while we load content...
        progressBar.setVisibility(View.VISIBLE);

        // and kick off the task to load all the things...
        loadContent();

        funnel = new LinkPreviewFunnel(app);
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
        if (!isAdded()) {
            return;
        }
        getApplication().getPageCache()
                .get(pageTitle, 0, new PageCache.CacheGetListener() {
                    @Override
                    public void onGetComplete(Page page, int sequence) {
                        if (page != null) {
                            displayPreviewFromCachedPage(page);
                        } else {
                            loadContentFromSavedPage();
                        }
                    }

                    @Override
                    public void onGetError(Throwable e, int sequence) {
                        Log.e(TAG, "Failed to get page from cache.", e);
                        loadContentFromSavedPage();
                    }
                });
    }

    private void loadContentFromSavedPage() {
        Log.v(TAG, "Loading link preview from Saved Pages");
        if (!isAdded()) {
            return;
        }
        new LoadSavedPageTask(pageTitle) {
            @Override
            public void onFinish(Page page) {
                displayPreviewFromCachedPage(page);
            }

            @Override
            public void onCatch(Throwable caught) {
                progressBar.setVisibility(View.GONE);
                FeedbackUtil.showMessage(getActivity(), R.string.error_network_error);
                dismiss();
            }
        }.execute();
    }

    private void displayPreviewFromCachedPage(Page page) {
        if (!isAdded()) {
            Log.d(TAG, "Detached from activity, so stopping update.");
            return;
        }
        progressBar.setVisibility(View.GONE);
        contents = new LinkPreviewContents(page);
        layoutPreview();
    }

    private PageSummary.Callback linkPreviewOnLoadCallback = new PageSummary.Callback() {
        @Override
        public void success(PageSummary pageSummary, Response response) {
            if (!isAdded()) {
                return;
            }
            Log.v(TAG, response.getUrl());
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
        public void failure(RetrofitError error) {
            if (!isAdded()) {
                return;
            }
            Log.e(TAG, "Link preview fetch error: " + error);
        }
    };

    private PageActivity getPageActivity() {
        return (PageActivity) getActivity();
    }

    private WikipediaApp getApplication() {
        return WikipediaApp.getInstance();
    }

    private class DefaultOnNavigateListener implements OnNavigateListener {
        @Override
        public void onNavigate(PageTitle title) {
            HistoryEntry newEntry = new HistoryEntry(title, entrySource);
            getPageActivity().displayNewPage(title, newEntry);
        }
    }

    private void layoutPreview() {
        if (!TextUtils.isEmpty(contents.getExtract())) {
            extractText.setText(contents.getExtract());
        }
        int placeholderResId = ResourceUtil.getThemedAttributeId(getContext(), R.attr.lead_image_drawable);
        if (shouldDownloadLeadImage()) {
            Picasso.with(getActivity())
                    .load(contents.getTitle().getThumbUrl())
                    .placeholder(placeholderResId)
                    .error(placeholderResId)
                    .into(leadImage);
        } else {
            Picasso.with(getActivity())
                    .load(placeholderResId)
                    .into(leadImage);
        }
    }

    private boolean shouldDownloadLeadImage() {
        return !TextUtils.isEmpty(contents.getTitle().getThumbUrl())
                && WikipediaApp.getInstance().isImageDownloadEnabled();
    }
}
