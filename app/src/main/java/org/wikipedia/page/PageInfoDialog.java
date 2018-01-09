package org.wikipedia.page;

import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import org.wikipedia.LongPressHandler.ListViewContextMenuListener;
import org.wikipedia.R;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.readinglist.AddToReadingListDialog;

import static org.wikipedia.util.L10nUtil.getStringForArticleLanguage;

/**
 * A dialog to host page issues and disambig information.
 */
public class PageInfoDialog extends NoDimBottomSheetDialog {
    private final ViewFlipper flipper;
    private final TextView disambigHeading;
    private final TextView issuesHeading;
    private final ListView disambigList;

    public PageInfoDialog(final PageFragment fragment, PageInfo pageInfo, boolean startAtDisambig) {
        super(fragment.getContext());
        View parentView = LayoutInflater.from(fragment.getContext()).inflate(R.layout.dialog_page_info, null);
        setContentView(parentView);

        flipper = parentView.findViewById(R.id.page_info_flipper);
        disambigList = parentView.findViewById(R.id.disambig_list);
        ListView issuesList = parentView.findViewById(R.id.page_issues_list);
        disambigHeading = parentView.findViewById(R.id.page_info_similar_titles_heading);
        issuesHeading = parentView.findViewById(R.id.page_info_page_issues_heading);
        View separatorHeading = parentView.findViewById(R.id.page_info_heading_separator);
        View closeButton = parentView.findViewById(R.id.page_info_close);

        disambigHeading.setText(getStringForArticleLanguage(pageInfo.getTitle(), R.string.page_similar_titles));
        issuesHeading.setText(getStringForArticleLanguage(pageInfo.getTitle(), R.string.dialog_page_issues));

        closeButton.setOnClickListener((View v) -> dismiss());

        issuesList.setAdapter(new IssuesListAdapter(fragment.getActivity(), pageInfo.getContentIssues()));
        disambigList.setAdapter(new DisambigListAdapter(fragment.getActivity(), pageInfo.getSimilarTitles()));
        disambigList.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            PageTitle title = ((DisambigResult) disambigList.getAdapter().getItem(position)).getTitle();
            HistoryEntry historyEntry = new HistoryEntry(title, HistoryEntry.SOURCE_DISAMBIG);
            dismiss();
            fragment.loadPage(title, historyEntry);
        });

        if (fragment.callback() != null) {
            ListViewContextMenuListener contextMenuListener
                    = new LongPressHandler(fragment);
            new org.wikipedia.LongPressHandler(disambigList, HistoryEntry.SOURCE_DISAMBIG,
                    contextMenuListener);
        }

        if (pageInfo.getSimilarTitles().length > 0) {
            disambigHeading.setOnClickListener((v) -> showDisambig());
        } else {
            disambigHeading.setVisibility(View.GONE);
            separatorHeading.setVisibility(View.GONE);
        }
        if (pageInfo.getContentIssues().length > 0) {
            issuesHeading.setOnClickListener((v) -> showIssues());
        } else {
            issuesHeading.setVisibility(View.GONE);
            separatorHeading.setVisibility(View.GONE);
        }

        if (startAtDisambig) {
            showDisambig();
        } else {
            showIssues();
        }
    }

    private void showDisambig() {
        startExpanded();
        if (flipper.getCurrentView() != flipper.getChildAt(0)) {
            flipper.setInAnimation(getContext(), R.anim.slide_in_left);
            flipper.setOutAnimation(getContext(), R.anim.slide_out_right);
            flipper.showNext();
        }

        disambigHeading.setTypeface(null, Typeface.BOLD);
        disambigHeading.setEnabled(false);
        issuesHeading.setTypeface(null, Typeface.NORMAL);
        issuesHeading.setEnabled(true);
    }

    private void showIssues() {
        if (flipper.getCurrentView() != flipper.getChildAt(1)) {
            flipper.setInAnimation(getContext(), R.anim.slide_in_right);
            flipper.setOutAnimation(getContext(), R.anim.slide_out_left);
            flipper.showPrevious();
        }

        disambigHeading.setTypeface(null, Typeface.NORMAL);
        disambigHeading.setEnabled(true);
        issuesHeading.setTypeface(null, Typeface.BOLD);
        issuesHeading.setEnabled(false);
    }

    private class LongPressHandler extends PageContainerLongPressHandler
            implements ListViewContextMenuListener {
        LongPressHandler(@NonNull PageFragment fragment) {
            super(fragment);
        }

        @Override
        public PageTitle getTitleForListPosition(int position) {
            return ((DisambigResult) disambigList.getAdapter().getItem(position)).getTitle();
        }

        @Override
        public void onOpenLink(PageTitle title, HistoryEntry entry) {
            super.onOpenLink(title, entry);
            dismiss();
        }

        @Override
        public void onOpenInNewTab(PageTitle title, HistoryEntry entry) {
            super.onOpenInNewTab(title, entry);
            dismiss();
        }

        @Override
        public void onCopyLink(PageTitle title) {
            super.onCopyLink(title);
            dismiss();
        }

        @Override
        public void onShareLink(PageTitle title) {
            super.onShareLink(title);
            dismiss();
        }

        @Override
        public void onAddToList(PageTitle title, AddToReadingListDialog.InvokeSource source) {
            super.onAddToList(title, source);
        }
    }
}
