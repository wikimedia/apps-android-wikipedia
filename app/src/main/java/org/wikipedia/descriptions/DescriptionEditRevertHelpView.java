package org.wikipedia.descriptions;

import android.content.Context;
import android.net.Uri;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.BulletSpan;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.databinding.ViewDescriptionEditRevertHelpBinding;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.StringUtil;

public class DescriptionEditRevertHelpView extends ScrollView {
    public DescriptionEditRevertHelpView(@NonNull Context context, @NonNull String qNumber) {
        super(context);
        init(qNumber);
    }

    @SuppressWarnings("checkstyle:magicnumber") private void init(@NonNull String qNumber) {
        final ViewDescriptionEditRevertHelpBinding binding = ViewDescriptionEditRevertHelpBinding.bind(this);
        final TextView helpText = binding.viewDescriptionEditRevertHelpContents;

        helpText.setMovementMethod(new LinkMovementMethod());

        Spanned helpStr = StringUtil.fromHtml(getString(R.string.description_edit_revert_help_body)
                .replaceAll(":revertSubtitle", getString(R.string.description_edit_revert_subtitle))
                .replaceAll(":revertIntro", getString(R.string.description_edit_revert_intro))
                .replaceAll(":revertHistory",
                        getContext().getString(R.string.description_edit_revert_history, getHistoryUri(qNumber))));

        int gapWidth = DimenUtil.roundedDpToPx(8);
        SpannableString revertReason1 = new SpannableString(StringUtil.fromHtml(getContext().getString(R.string.description_edit_revert_reason1, getString(R.string.wikidata_description_guide_url))));
        SpannableString revertReason2 = new SpannableString(StringUtil.fromHtml(getString(R.string.description_edit_revert_reason2)));
        revertReason1.setSpan(new BulletSpan(gapWidth), 0, revertReason1.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        revertReason2.setSpan(new BulletSpan(gapWidth), 0, revertReason2.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        SpannableString helpSpan = new SpannableString(TextUtils.expandTemplate(helpStr, revertReason1, revertReason2));
        helpText.setText(helpSpan);
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
