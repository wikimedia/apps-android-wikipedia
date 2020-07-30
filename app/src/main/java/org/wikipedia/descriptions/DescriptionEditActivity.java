package org.wikipedia.descriptions;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import org.wikipedia.suggestededits.PageSummaryForEdit;
import org.wikipedia.util.ClipboardUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.ShareUtil;
import org.wikipedia.views.ImagePreviewDialog;

import static org.wikipedia.Constants.INTENT_EXTRA_ACTION;
import static org.wikipedia.Constants.INTENT_EXTRA_INVOKE_SOURCE;
import static org.wikipedia.Constants.InvokeSource;
import static org.wikipedia.Constants.InvokeSource.LINK_PREVIEW_MENU;
import static org.wikipedia.Constants.InvokeSource.PAGE_ACTIVITY;
import static org.wikipedia.descriptions.DescriptionEditActivity.Action.ADD_CAPTION;
import static org.wikipedia.descriptions.DescriptionEditActivity.Action.TRANSLATE_CAPTION;
import static org.wikipedia.descriptions.DescriptionEditActivity.Action.TRANSLATE_DESCRIPTION;
import static org.wikipedia.util.DeviceUtil.hideSoftKeyboard;

public class DescriptionEditActivity extends SingleFragmentActivity<DescriptionEditFragment>
        implements DescriptionEditFragment.Callback, LinkPreviewDialog.Callback {

    public enum Action {
        ADD_DESCRIPTION,
        TRANSLATE_DESCRIPTION,
        ADD_CAPTION,
        TRANSLATE_CAPTION,
        ADD_IMAGE_TAGS
    }

    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_HIGHLIGHT_TEXT = "highlightText";
    private static final String EXTRA_SOURCE_SUMMARY = "sourceSummary";
    private static final String EXTRA_TARGET_SUMMARY = "targetSummary";
    private Action action;
    private ExclusiveBottomSheetPresenter bottomSheetPresenter = new ExclusiveBottomSheetPresenter();

    public static Intent newIntent(@NonNull Context context,
                                   @NonNull PageTitle title,
                                   @Nullable String highlightText,
                                   @Nullable PageSummaryForEdit sourceSummary,
                                   @Nullable PageSummaryForEdit targetSummary,
                                   @NonNull Action action,
                                   @NonNull InvokeSource invokeSource) {
        return new Intent(context, DescriptionEditActivity.class)
                .putExtra(EXTRA_TITLE, GsonMarshaller.marshal(title))
                .putExtra(EXTRA_HIGHLIGHT_TEXT, highlightText)
                .putExtra(EXTRA_SOURCE_SUMMARY, sourceSummary == null ? null : GsonMarshaller.marshal(sourceSummary))
                .putExtra(EXTRA_TARGET_SUMMARY, targetSummary == null ? null : GsonMarshaller.marshal(targetSummary))
                .putExtra(INTENT_EXTRA_ACTION, action)
                .putExtra(INTENT_EXTRA_INVOKE_SOURCE, invokeSource);
    }

    @Override
    public void onDescriptionEditSuccess() {
        setResult(RESULT_OK);
        finish();
    }

    @Override
    public void onBottomBarContainerClicked(@NonNull Action action) {
        PageSummaryForEdit summary;

        if (action == TRANSLATE_DESCRIPTION) {
            summary = GsonUnmarshaller.unmarshal(PageSummaryForEdit.class, getIntent().getStringExtra(EXTRA_TARGET_SUMMARY));
        } else {
            summary = GsonUnmarshaller.unmarshal(PageSummaryForEdit.class, getIntent().getStringExtra(EXTRA_SOURCE_SUMMARY));
        }

        if (action == ADD_CAPTION || action == TRANSLATE_CAPTION) {
            bottomSheetPresenter.show(getSupportFragmentManager(),
                    ImagePreviewDialog.Companion.newInstance(summary, action));
        } else {
            bottomSheetPresenter.show(getSupportFragmentManager(),
                    LinkPreviewDialog.newInstance(new HistoryEntry(summary.getPageTitle(),
                                    getIntent().hasExtra(INTENT_EXTRA_INVOKE_SOURCE) && getIntent().getSerializableExtra(INTENT_EXTRA_INVOKE_SOURCE) == PAGE_ACTIVITY
                                            ? HistoryEntry.SOURCE_EDIT_DESCRIPTION : HistoryEntry.SOURCE_SUGGESTED_EDITS),
                            null));
        }
    }

    public void onLinkPreviewLoadPage(@NonNull PageTitle title, @NonNull HistoryEntry entry, boolean inNewTab) {
        startActivity(PageActivity.newIntentForCurrentTab(this, entry, entry.getTitle()));
    }

    @Override
    public void onLinkPreviewCopyLink(@NonNull PageTitle title) {
        copyLink(title.getUri());
    }

    @Override
    public void onLinkPreviewAddToList(@NonNull PageTitle title) {
        bottomSheetPresenter.show(getSupportFragmentManager(),
                AddToReadingListDialog.newInstance(title, LINK_PREVIEW_MENU));
    }

    @Override
    public void onLinkPreviewShareLink(@NonNull PageTitle title) {
        ShareUtil.shareText(this, title);
    }

    public void updateStatusBarColor(@ColorInt int color) {
        setStatusBarColor(color);
    }

    public void updateNavigationBarColor(@ColorInt int color) {
        setNavigationBarColor(color);
    }

    private void copyLink(@NonNull String url) {
        ClipboardUtil.setPlainText(this, null, url);
        FeedbackUtil.showMessage(this, R.string.address_copied);
    }

    @Override
    public DescriptionEditFragment createFragment() {
        InvokeSource invokeSource = (InvokeSource) getIntent().getSerializableExtra(INTENT_EXTRA_INVOKE_SOURCE);
        action = (Action) getIntent().getSerializableExtra(INTENT_EXTRA_ACTION);
        PageTitle title = GsonUnmarshaller.unmarshal(PageTitle.class, getIntent().getStringExtra(EXTRA_TITLE));
        SuggestedEditsFunnel.get().click(title.getDisplayText(), action);

        return DescriptionEditFragment.newInstance(title,
                getIntent().getStringExtra(EXTRA_HIGHLIGHT_TEXT),
                getIntent().getStringExtra(EXTRA_SOURCE_SUMMARY),
                getIntent().getStringExtra(EXTRA_TARGET_SUMMARY),
                action,
                invokeSource);
    }

    @Override
    public void onBackPressed() {
        if (getFragment().editView.showingReviewContent()) {
            getFragment().editView.loadReviewContent(false);
        } else {
            hideSoftKeyboard(this);
            SuggestedEditsFunnel.get().cancel(action);
            super.onBackPressed();
        }
    }
}
