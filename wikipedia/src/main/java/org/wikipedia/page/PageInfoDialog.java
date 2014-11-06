package org.wikipedia.page;

import org.wikipedia.R;
import android.app.Activity;
import android.graphics.Typeface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.TextView;

/**
 * A dialog to host page issues and disambig information.
 */
class PageInfoDialog extends BottomDialog {
    private final Activity activity;
    private final PageInfo info;
    private final LinkMovementMethodExt movementMethod;
    private ListView list;
    private TextView disambigHeading;
    private TextView issuesHeading;

    PageInfoDialog(Activity activity, PageInfo pageInfo, int height, LinkMovementMethodExt movementMethod) {
        super(activity, R.layout.dialog_page_info);
        this.activity = activity;
        info = pageInfo;
        this.movementMethod = movementMethod;

        View parentView = getDialogLayout();
        list = (ListView) parentView.findViewById(R.id.page_info_list);
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
        list.setAdapter(new DisambigListAdapter(activity, info.getDisambigs(), movementMethod));
        disambigHeading.setTypeface(null, Typeface.BOLD);
        issuesHeading.setTypeface(null, Typeface.NORMAL);
    }

    void showIssues() {
        list.setAdapter(new IssuesListAdapter(activity, info.getIssues()));
        disambigHeading.setTypeface(null, Typeface.NORMAL);
        issuesHeading.setTypeface(null, Typeface.BOLD);
    }
}
