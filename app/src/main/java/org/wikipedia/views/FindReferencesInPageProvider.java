package org.wikipedia.views;

import android.content.Context;
import android.view.ActionProvider;
import android.view.View;
import android.widget.TextView;

import org.wikipedia.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class FindReferencesInPageProvider extends ActionProvider {

    public interface Callback {
        void onFindNextClicked();
        void onFindPrevClicked();
        void onCloseClicked();
        void onViewBindingComplete();
        void onReferenceLabelClicked();
    }

    public FindReferencesInPageProvider(Context context) {
        super(context);
        this.context = context;
    }

    @BindView(R.id.reference_label) TextView referenceLabel;
    @BindView(R.id.reference_count) TextView referenceCount;
    @BindView(R.id.find_in_page_next) View findInPageNext;
    @BindView(R.id.find_in_page_prev) View findInPagePrev;

    private Context context;
    private Callback callback;

    @Override
    public boolean overridesItemVisibility() {
        return true;
    }

    @Override
    public View onCreateActionView() {
        View view = View.inflate(context, R.layout.group_find_references_in_page, null);
        ButterKnife.bind(this, view);
        if (callback != null) {
            callback.onViewBindingComplete();
        }
        return view;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @OnClick(R.id.find_in_page_next) void onFindInPageNextClicked(View v) {
        if (callback != null) {
            callback.onFindNextClicked();
        }
    }

    @OnClick(R.id.find_in_page_prev) void onFindInPagePrevClicked(View v) {
        if (callback != null) {
            callback.onFindPrevClicked();
        }
    }

    @OnClick(R.id.close_button) void onCloseClicked(View v) {
        if (callback != null) {
            callback.onCloseClicked();
        }
    }

    @OnClick(R.id.reference_label) void onReferenceLabelClicked(View v) {
        if (callback != null) {
            callback.onReferenceLabelClicked();
        }
    }

    public void setReferenceLabel(String referenceLabelText) {
        referenceLabel.setText(referenceLabelText);
    }

    public void setReferenceCountText(String referenceCountText) {
        referenceCount.setText(referenceCountText);
    }
}
