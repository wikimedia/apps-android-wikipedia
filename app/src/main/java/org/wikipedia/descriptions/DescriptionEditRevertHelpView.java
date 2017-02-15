package org.wikipedia.descriptions;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.text.method.LinkMovementMethod;
import android.widget.ScrollView;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.util.StringUtil;

import butterknife.BindView;
import butterknife.ButterKnife;

public class DescriptionEditRevertHelpView extends ScrollView {
    @BindView(R.id.view_description_edit_revert_help_contents) TextView helpText;

    public DescriptionEditRevertHelpView(@NonNull Context context, @NonNull String qNumber) {
        super(context);
        init(qNumber);
    }

    private void init(@NonNull String qNumber) {
        inflate(getContext(), R.layout.view_description_edit_revert_help, this);
        ButterKnife.bind(this);
        helpText.setMovementMethod(new LinkMovementMethod());

        String helpStr = getString(R.string.description_edit_revert_help_body)
                .replaceAll(":revertSubtitle", getString(R.string.description_edit_revert_subtitle))
                .replaceAll(":revertIntro", getString(R.string.description_edit_revert_intro))
                .replaceAll(":revertReason1",
                        String.format(getString(R.string.description_edit_revert_reason1), getString(R.string.wikidata_description_guide_url)))
                .replaceAll(":revertReason2", getString(R.string.description_edit_revert_reason2))
                .replaceAll(":revertHistory",
                        String.format(getString(R.string.description_edit_revert_history), getHistoryUri(qNumber)));

        helpText.setText(StringUtil.fromHtml(helpStr));
    }

    private Uri getHistoryUri(@NonNull String qNumber) {
        return new Uri.Builder()
                .scheme(WikipediaApp.getInstance().getWikiSite().scheme())
                .authority("m.wikidata.org")
                .appendPath("wiki")
                .appendPath("Special:History")
                .appendPath(qNumber)
                .build();
    }

    private String getString(@StringRes int id) {
        return getContext().getString(id);
    }
}
