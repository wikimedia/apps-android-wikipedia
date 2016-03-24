package org.wikipedia.readinglist;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.R;
import org.wikipedia.page.ExtendedBottomSheetDialogFragment;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.FeedbackUtil;

import java.util.ArrayList;
import java.util.List;

public class AddToReadingListDialog extends ExtendedBottomSheetDialogFragment {
    private PageTitle pageTitle;
    private ReadingListAdapter adapter;
    private CreateButtonClickListener createClickListener = new CreateButtonClickListener();
    private List<ReadingList> readingLists = new ArrayList<>();
    private DialogInterface.OnDismissListener dismissListener;

    public static AddToReadingListDialog newInstance(PageTitle title) {
        AddToReadingListDialog dialog = new AddToReadingListDialog();
        Bundle args = new Bundle();
        args.putParcelable("title", title);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageTitle = getArguments().getParcelable("title");
        adapter = new ReadingListAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_add_to_reading_list, container);
        RecyclerView readingListView = (RecyclerView) rootView.findViewById(R.id.list_of_lists);
        readingListView.setLayoutManager(new LinearLayoutManager(getActivity()));
        readingListView.setAdapter(adapter);

        View createButton = rootView.findViewById(R.id.create_button);
        createButton.setOnClickListener(createClickListener);

        View closeButton = rootView.findViewById(R.id.close_button);
        FeedbackUtil.setToolbarButtonLongPressToast(closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        updateLists();
        return rootView;
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if (dismissListener != null) {
            dismissListener.onDismiss(null);
        }
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        dismissListener = listener;
    }

    private void updateLists() {
        readingLists = ReadingList.DAO.queryLists(ReadingListData.SORT_BY_RECENT);
        adapter.notifyDataSetChanged();
    }

    private class CreateButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            final ReadingList readingList = new ReadingList();
            readingList.setTitle(getString(R.string.reading_list_name_sample));
            AlertDialog dialog = ReadingListDialogs.createEditDialog(getContext(), readingList, false, new Runnable() {
                @Override
                public void run() {
                    addAndDismiss(readingList);
                }
            }, null);
            dialog.show();
        }
    }

    private void addAndDismiss(ReadingList readingList) {
        if (ReadingList.DAO.listContainsTitle(readingList, pageTitle)) {
            ((PageActivity) getActivity())
                    .showReadingListAddedSnackbar(getString(R.string.reading_list_already_exists));
        } else {
            ((PageActivity) getActivity())
                    .showReadingListAddedSnackbar(TextUtils.isEmpty(readingList.getTitle())
                            ? getString(R.string.reading_list_added_to_unnamed)
                            : String.format(getString(R.string.reading_list_added_to_named),
                            readingList.getTitle()));

            ReadingList.DAO.addTitleToList(readingList, pageTitle);
        }

        ReadingList.DAO.makeListMostRecent(readingList);
        dismiss();
    }

    private class ReadingListItemHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private ReadingListItemView itemView;
        private ReadingList readingList;

        ReadingListItemHolder(ReadingListItemView itemView) {
            super(itemView);
            this.itemView = itemView;
            itemView.setOnClickListener(this);
        }

        public void bindItem(ReadingList readingList) {
            this.readingList = readingList;
            itemView.setReadingList(readingList);
        }

        @Override
        public void onClick(View v) {
            addAndDismiss(readingList);
        }
    }

    private final class ReadingListAdapter extends RecyclerView.Adapter<ReadingListItemHolder> {
        @Override
        public int getItemCount() {
            return readingLists.size();
        }

        @Override
        public ReadingListItemHolder onCreateViewHolder(ViewGroup parent, int pos) {
            ReadingListItemView view = new ReadingListItemView(getContext());
            return new ReadingListItemHolder(view);
        }

        @Override
        public void onBindViewHolder(ReadingListItemHolder holder, int pos) {
            holder.bindItem(readingLists.get(pos));
        }
    }
}
