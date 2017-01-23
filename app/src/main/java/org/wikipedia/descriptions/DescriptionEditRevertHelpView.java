package org.wikipedia.descriptions;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
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

        helpText.setText(StringUtil.fromHtml(String.format(getContext().getString(R.string.description_edit_revert_help_body),
                getContext().getString(R.string.wikidata_description_guide_url),
                getHistoryUri(qNumber))));
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
}