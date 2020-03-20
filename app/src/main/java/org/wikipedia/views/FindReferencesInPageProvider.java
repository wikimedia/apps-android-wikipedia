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

    public interface FindReferencesInPageListener {
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
    private FindReferencesInPageListener listener;


    @Override
    public boolean overridesItemVisibility() {
        return true;
    }

    @Override
    public View onCreateActionView() {
        View view = View.inflate(context, R.layout.group_find_references_in_page, null);
        ButterKnife.bind(this, view);
        if (listener != null) {
            listener.onViewBindingComplete();
        }
        return view;
    }

    public void setListener(FindReferencesInPageListener listener) {
        this.listener = listener;
    }

    @OnClick(R.id.find_in_page_next) void onFindInPageNextClicked(View v) {
        if (listener != null) {
            listener.onFindNextClicked();
        }
    }

    @OnClick(R.id.find_in_page_prev) void onFindInPagePrevClicked(View v) {
        if (listener != null) {
            listener.onFindPrevClicked();
        }
    }

    @OnClick(R.id.close_button) void onCloseClicked(View v) {
        if (listener != null) {
            listener.onCloseClicked();
        }
    }

    @OnClick(R.id.reference_label) void onReferenceLabelClicked(View v) {
        if (listener != null) {
            listener.onReferenceLabelClicked();
        }
    }

    public void setReferenceLabel(String referenceLabelText) {
        referenceLabel.setText(referenceLabelText);
    }

    public void setReferenceCountText(String referenceCountText) {
        referenceCount.setText(referenceCountText);
    }
}
