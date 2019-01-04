package org.wikipedia.descriptions;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.wikipedia.R;
import org.wikipedia.activity.SingleFragmentActivity;
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

import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;

public class DescriptionEditActivity extends SingleFragmentActivity<DescriptionEditFragment>
        implements DescriptionEditFragment.Callback, LinkPreviewDialog.Callback {
    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_REVIEW_ENABLE = "review";
    private static final String EXTRA_HIGHLIGHT_TEXT = "highlightText";
    private static final String EXTRA_IS_TRANSLATION = "is_translation";
    private static final String EXTRA_SOURCE_LANG_DESC = "source_desc";
    private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();

    public static Intent newIntent(@NonNull Context context, @NonNull PageTitle title, @Nullable String highlightText, boolean reviewEnabled, boolean isTranslation, CharSequence sourceDesc) {
        return new Intent(context, DescriptionEditActivity.class)
                .putExtra(EXTRA_TITLE, GsonMarshaller.marshal(title))
                .putExtra(EXTRA_HIGHLIGHT_TEXT, highlightText)
                .putExtra(EXTRA_REVIEW_ENABLE, reviewEnabled)
                .putExtra(EXTRA_IS_TRANSLATION, isTranslation)
                .putExtra(EXTRA_SOURCE_LANG_DESC, sourceDesc);
    }

    @Override
    public void onDescriptionEditSuccess() {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onPageSummaryContainerClicked(@NonNull PageTitle pageTitle) {
        bottomSheetPresenter.show(getSupportFragmentManager(),
                LinkPreviewDialog.newInstance(new HistoryEntry(pageTitle, HistoryEntry.SOURCE_EDIT_DESCRIPTION), null));
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
        return DescriptionEditFragment.newInstance(GsonUnmarshaller.unmarshal(PageTitle.class,
                getIntent().getStringExtra(EXTRA_TITLE)),
                getIntent().getStringExtra(EXTRA_HIGHLIGHT_TEXT),
                getIntent().getBooleanExtra(EXTRA_REVIEW_ENABLE, false),
                getIntent().getBooleanExtra(EXTRA_IS_TRANSLATION, false),
                getIntent().getCharSequenceExtra(EXTRA_SOURCE_LANG_DESC));
    }

    @Override
    public void onBackPressed() {
        if (getFragment().editView.showingReviewContent()) {
            getFragment().editView.loadReviewContent(false);
        } else {
            hideSoftKeyboard(this);
            super.onBackPressed();
        }
    }
}
