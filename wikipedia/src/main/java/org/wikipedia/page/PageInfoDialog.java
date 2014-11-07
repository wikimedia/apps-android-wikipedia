package org.wikipedia.page;

import org.wikipedia.R;
import android.app.Activity;
import android.graphics.Typeface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

/**
 * A dialog to host page issues and disambig information.
 */
class PageInfoDialog extends BottomDialog {
    private final ViewFlipper flipper;
    private final TextView disambigHeading;
    private final TextView issuesHeading;

    PageInfoDialog(Activity activity, PageInfo pageInfo, int height, LinkMovementMethodExt movementMethod) {
        super(activity, R.layout.dialog_page_info);

        View parentView = getDialogLayout();
        flipper = (ViewFlipper) parentView.findViewById(R.id.page_info_flipper);
        ListView disambigList = (ListView) parentView.findViewById(R.id.disambig_list);
        ListView issuesList = (ListView) parentView.findViewById(R.id.page_issues_list);
        disambigHeading = (TextView) parentView.findViewById(R.id.page_info_similar_titles_heading);
        issuesHeading = (TextView) parentView.findViewById(R.id.page_info_page_issues_heading);
        View separatorHeading = parentView.findViewById(R.id.page_info_heading_separator);
        View closeButton = parentView.findViewById(R.id.page_info_close);

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

//        parentView.setMinimumHeight(height);
        parentView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, height));

        disambigList.setAdapter(new DisambigListAdapter(activity, pageInfo.getDisambigs(), movementMethod));
        issuesList.setAdapter(new IssuesListAdapter(activity, pageInfo.getIssues()));

        if (pageInfo.getDisambigs().length > 0) {
            disambigHeading.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDisambig();
                }
            });
        } else {
            disambigHeading.setVisibility(View.GONE);
            separatorHeading.setVisibility(View.GONE);
        }
        if (pageInfo.getIssues().length > 0) {
            issuesHeading.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showIssues();
                }
            });
        } else {
            issuesHeading.setVisibility(View.GONE);
            separatorHeading.setVisibility(View.GONE);
        }
    }

    void showDisambig() {
        if (flipper.getCurrentView() != flipper.getChildAt(0)) {
            flipper.setInAnimation(getContext(), R.anim.slide_in_left);
            flipper.setOutAnimation(getContext(), R.anim.slide_out_right);
            flipper.showNext();
        }

        disambigHeading.setTypeface(null, Typeface.BOLD);
        issuesHeading.setTypeface(null, Typeface.NORMAL);
    }

    void showIssues() {
        if (flipper.getCurrentView() != flipper.getChildAt(1)) {
            flipper.setInAnimation(getContext(), R.anim.slide_in_right);
            flipper.setOutAnimation(getContext(), R.anim.slide_out_left);
            flipper.showPrevious();
        }

        disambigHeading.setTypeface(null, Typeface.NORMAL);
        issuesHeading.setTypeface(null, Typeface.BOLD);
    }
}
