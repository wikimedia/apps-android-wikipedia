package org.wikipedia.descriptions;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.analytics.SuggestedEditsFunnel;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.page.ExclusiveBottomSheetPresenter;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.page.linkpreview.LinkPreviewDialog;
import org.wikipedia.readinglist.AddToReadingListDialog;
import org.wikipedia.util.ClipboardUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ShareUtil;

import static org.wikipedia.Constants.INTENT_EXTRA_INVOKE_SOURCE;
import static org.wikipedia.Constants.InvokeSource;
import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;

public class DescriptionEditActivity extends SingleFragmentActivity<DescriptionEditFragment>
        implements DescriptionEditFragment.Callback, LinkPreviewDialog.Callback {

    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_REVIEW_ENABLE = "review";
    private static final String EXTRA_HIGHLIGHT_TEXT = "highlightText";
    private static final String EXTRA_TRANSLATION_SOURCE_DESCRIPTION = "translationSourceDescription";
    private static final String EXTRA_TRANSLATION_SOURCE_LANGUAGE_CODE = "translationSourceLanguageCode";
    private static final String EXTRA_INVOKE_SOURCE = "invokeSource";
    private InvokeSource invokeSource;
    private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();

    public static Intent newIntent(@NonNull Context context,
                                   @NonNull PageTitle title,
                                   @Nullable String highlightText,
                                   boolean reviewEnabled,
                                   @Nullable CharSequence translationSourceDescription,
                                   @Nullable String translationSourceLanguageCode,
                                   @NonNull InvokeSource invokeSource) {
        return new Intent(context, DescriptionEditActivity.class)
                .putExtra(EXTRA_TITLE, GsonMarshaller.marshal(title))
                .putExtra(EXTRA_HIGHLIGHT_TEXT, highlightText)
                .putExtra(EXTRA_REVIEW_ENABLE, reviewEnabled)
                .putExtra(EXTRA_TRANSLATION_SOURCE_DESCRIPTION, translationSourceDescription)
                .putExtra(EXTRA_TRANSLATION_SOURCE_LANGUAGE_CODE, translationSourceLanguageCode)
                .putExtra(EXTRA_INVOKE_SOURCE, invokeSource);
    }

    @Override
    public void onDescriptionEditSuccess() {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onPageSummaryContainerClicked(@NonNull PageTitle pageTitle) {
        bottomSheetPresenter.show(getSupportFragmentManager(),
                LinkPreviewDialog.newInstance(new HistoryEntry(pageTitle,
                        getIntent().hasExtra(EXTRA_INVOKE_SOURCE) && getIntent().getSerializableExtra(EXTRA_INVOKE_SOURCE) == InvokeSource.PAGE_ACTIVITY
                                ? HistoryEntry.SOURCE_EDIT_DESCRIPTION : HistoryEntry.SOURCE_SUGGESTED_EDITS),
                        null));
    }

    public void onLinkPreviewLoadPage(@NonNull PageTitle title, @NonNull HistoryEntry entry, boolean inNewTab) {
        startActivity(PageActivity.newIntentForNewTab(this, entry, entry.getTitle()));
    }

    @Override
    public void onLinkPreviewCopyLink(@NonNull PageTitle title) {
        copyLink(title.getCanonicalUri());
    }

    @Override
    public void onLinkPreviewAddToList(@NonNull PageTitle title) {
        bottomSheetPresenter.show(getSupportFragmentManager(),
                AddToReadingListDialog.newInstance(title,
                        AddToReadingListDialog.InvokeSource.LINK_PREVIEW_MENU));
    }

    @Override
    public void onLinkPreviewShareLink(@NonNull PageTitle title) {
        ShareUtil.shareText(this, title);
    }

    private void copyLink(@NonNull String url) {
        ClipboardUtil.setPlainText(this, null, url);
        FeedbackUtil.showMessage(this, R.string.address_copied);
    }

    @Override
    public DescriptionEditFragment createFragment() {
        invokeSource = (InvokeSource) getIntent().getSerializableExtra(INTENT_EXTRA_INVOKE_SOURCE);
        SuggestedEditsFunnel.get().click(invokeSource);

        return DescriptionEditFragment.newInstance(GsonUnmarshaller.unmarshal(PageTitle.class,
                getIntent().getStringExtra(EXTRA_TITLE)),
                getIntent().getStringExtra(EXTRA_HIGHLIGHT_TEXT),
                getIntent().getBooleanExtra(EXTRA_REVIEW_ENABLE, false),
                getIntent().getCharSequenceExtra(EXTRA_TRANSLATION_SOURCE_DESCRIPTION),
                getIntent().getStringExtra(EXTRA_TRANSLATION_SOURCE_LANGUAGE_CODE),
                invokeSource);
    }

    @Override
    public void onBackPressed() {
        if (getFragment().editView.showingReviewContent()) {
            getFragment().editView.loadReviewContent(false);
        } else {
            hideSoftKeyboard(this);
            SuggestedEditsFunnel.get().cancel(invokeSource);
            super.onBackPressed();
        }
    }
}
