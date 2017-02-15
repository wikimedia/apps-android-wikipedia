package org.wikipedia.descriptions;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.util.AttributeSet;
import android.widget.ScrollView;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.util.StringUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DescriptionEditHelpView extends ScrollView {
    @BindView(R.id.view_description_edit_help_contents) TextView helpText;

    @Nullable private Callback callback;

    public interface Callback {
        void onAboutClick();
        void onGuideClick();
    }

    public DescriptionEditHelpView(Context context) {
        super(context);
        init();
    }

    public DescriptionEditHelpView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DescriptionEditHelpView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DescriptionEditHelpView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    public void setCallback(@Nullable Callback callback) {
        this.callback = callback;
    }

    @OnClick(R.id.view_description_edit_help_about) void onAboutClick() {
        if (callback != null) {
            callback.onAboutClick();
        }
    }

    @OnClick(R.id.view_description_edit_help_guide) void onGuideClick() {
        if (callback != null) {
            callback.onGuideClick();
        }
    }

    private void init() {
        inflate(getContext(), R.layout.view_description_edit_help, this);
        ButterKnife.bind(this);
        String helpStr = getString(R.string.description_edit_help_body)
                .replaceAll(":helpAboutTitle", getString(R.string.description_edit_help_about_title))
                .replaceAll(":helpAboutDescription", getString(R.string.description_edit_help_description))
                .replaceAll(":helpTipsTitle", getString(R.string.description_edit_help_tips))
                .replaceAll(":helpTipsDescription", getString(R.string.description_edit_help_tips_description))
                .replaceAll(":helpTipsExamples", getString(R.string.description_edit_help_tips_examples))
                .replaceAll(":helpTipsExample1Hint", getString(R.string.description_edit_help_tips_example1_hint))
                .replaceAll(":helpTipsExample1", getString(R.string.description_edit_help_tips_example1))
                .replaceAll(":helpTipsExample2Hint", getString(R.string.description_edit_help_tips_example2_hint))
                .replaceAll(":helpTipsExample2", getString(R.string.description_edit_help_tips_example2))
                .replaceAll(":helpMoreInfo", getString(R.string.description_edit_help_more_info))
                .replaceAll(":helpMetaInfo", getString(R.string.description_edit_help_meta_info));
        helpText.setText(StringUtil.fromHtml(helpStr));
    }

    private String getString(@StringRes int id) {
        return getContext().getString(id);
    }
}
