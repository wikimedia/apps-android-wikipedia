package org.wikipedia.feed.onthisday;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.R;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.page.ExtendedBottomSheetDialogFragment;
import org.wikipedia.util.ResourceUtil;

public class OnThisDayActionsDialog extends ExtendedBottomSheetDialogFragment {
    public interface Callback {
        void onAddPageToList(@NonNull HistoryEntry entry);
        void onSharePage(@NonNull HistoryEntry entry);
    }

    private HistoryEntry entry;
    private OnThisDayActionsView actionsView;
    private ItemActionsCallback itemActionsCallback = new ItemActionsCallback();
    private OnThisDayCardView onThisDayCardView;

    @NonNull
    public static OnThisDayActionsDialog newInstance(@NonNull HistoryEntry entry) {

        OnThisDayActionsDialog instance = new OnThisDayActionsDialog();
        Bundle args = new Bundle();
        args.putParcelable("historyEntry", entry);
        args.putString("pageTitle", entry.getTitle().getDisplayText());
        instance.setArguments(args);
        return instance;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        actionsView = new OnThisDayActionsView(getContext());
        actionsView.setBackgroundColor(ResourceUtil.getThemedColor(getContext(), R.attr.paper_color));
        actionsView.setCallback(itemActionsCallback);
        entry = getArguments().getParcelable("historyEntry");
        actionsView.setState(getArguments().getString("pageTitle", ""));

        onThisDayCardView = new OnThisDayCardView(getContext());

        return actionsView;
    }

    @Override
    public void onDestroyView() {
        actionsView.setCallback(null);
        actionsView = null;
        super.onDestroyView();
    }

    public void setOnThisDayCardView(OnThisDayCardView onThisDayCardView) {
        this.onThisDayCardView = onThisDayCardView;
    }

    private class ItemActionsCallback implements OnThisDayActionsView.Callback {
        @Override
        public void onAddPageToList() {
            dismiss();
            if (callback() != null) {
                callback().onAddPageToList(entry);
            } else {
                onThisDayCardView.onAddPageToList(entry);
            }
        }

        @Override
        public void onSharePage() {
            dismiss();
            if (callback() != null) {
                callback().onSharePage(entry);
            }  else {
                onThisDayCardView.onSharePage(entry);
            }
        }
    }

    @Nullable
    private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}
