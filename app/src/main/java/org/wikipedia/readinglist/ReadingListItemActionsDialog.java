package org.wikipedia.readinglist;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.page.ExtendedBottomSheetDialogFragment;
import org.wikipedia.readinglist.page.ReadingListPage;

public class ReadingListItemActionsDialog extends ExtendedBottomSheetDialogFragment {
    public interface Callback {
        void onToggleOffline(int pageIndex);
        void onShare(int pageIndex);
        void onAddToOther(int pageIndex);
        void onDelete(int pageIndex);
    }

    private int pageIndex;
    private ReadingListItemActionsView actionsView;
    private ItemActionsCallback itemActionsCallback = new ItemActionsCallback();

    @NonNull
    public static ReadingListItemActionsDialog newInstance(@NonNull ReadingListPage page,
                                                           @NonNull ReadingList list) {
        ReadingListItemActionsDialog instance = new ReadingListItemActionsDialog();
        Bundle args = new Bundle();
        args.putString("pageTitle", page.title());
        args.putString("listTitle", list.getTitle());
        args.putInt("pageIndex", list.getPages().indexOf(page));
        args.putBoolean("pageOffline", page.isOffline());
        instance.setArguments(args);
        return instance;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        actionsView = new ReadingListItemActionsView(getContext());
        actionsView.setCallback(itemActionsCallback);
        pageIndex = getArguments().getInt("pageIndex");
        actionsView.setState(getArguments().getString("pageTitle", ""),
                getArguments().getString("listTitle", ""),
                getArguments().getBoolean("pageOffline"));
        return actionsView;
    }

    @Override
    public void onDestroyView() {
        actionsView.setCallback(null);
        actionsView = null;
        super.onDestroyView();
    }

    private class ItemActionsCallback implements ReadingListItemActionsView.Callback {
        @Override
        public void onToggleOffline() {
            dismiss();
            if (callback() != null) {
                callback().onToggleOffline(pageIndex);
            }
        }

        @Override
        public void onShare() {
            dismiss();
            if (callback() != null) {
                callback().onShare(pageIndex);
            }
        }

        @Override
        public void onAddToOther() {
            dismiss();
            if (callback() != null) {
                callback().onAddToOther(pageIndex);
            }
        }

        @Override
        public void onDelete() {
            dismiss();
            if (callback() != null) {
                callback().onDelete(pageIndex);
            }
        }
    }

    @Nullable
    private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}
