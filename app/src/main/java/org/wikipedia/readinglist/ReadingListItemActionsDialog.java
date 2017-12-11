package org.wikipedia.readinglist;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.R;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.page.ExtendedBottomSheetDialogFragment;
import org.wikipedia.readinglist.database.ReadingList;
import org.wikipedia.readinglist.database.ReadingListPage;
import org.wikipedia.util.ResourceUtil;

public class ReadingListItemActionsDialog extends ExtendedBottomSheetDialogFragment {
    public interface Callback {
        void onToggleItemOffline(int pageIndex);
        void onShareItem(int pageIndex);
        void onAddItemToOther(int pageIndex);
        void onDeleteItem(int pageIndex);
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
        args.putString("listTitle", list.title());
        args.putInt("pageIndex", list.pages().indexOf(page));
        args.putBoolean("pageOffline", page.offline());
        instance.setArguments(args);
        return instance;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        actionsView = new ReadingListItemActionsView(getContext());
        actionsView.setBackgroundColor(ResourceUtil.getThemedColor(getContext(), R.attr.paper_color));
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
                callback().onToggleItemOffline(pageIndex);
            }
        }

        @Override
        public void onShare() {
            dismiss();
            if (callback() != null) {
                callback().onShareItem(pageIndex);
            }
        }

        @Override
        public void onAddToOther() {
            dismiss();
            if (callback() != null) {
                callback().onAddItemToOther(pageIndex);
            }
        }

        @Override
        public void onDelete() {
            dismiss();
            if (callback() != null) {
                callback().onDeleteItem(pageIndex);
            }
        }
    }

    @Nullable
    private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}
