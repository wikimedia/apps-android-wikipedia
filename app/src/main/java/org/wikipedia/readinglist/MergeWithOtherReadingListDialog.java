package org.wikipedia.readinglist;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.analytics.ReadingListsFunnel;
import org.wikipedia.concurrency.CallbackTask;
import org.wikipedia.model.EnumCode;
import org.wikipedia.model.EnumCodeMap;
import org.wikipedia.page.ExtendedBottomSheetDialogFragment;
import org.wikipedia.readinglist.database.ReadingList;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.settings.Prefs;
import org.wikipedia.settings.SiteInfoClient;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MergeWithOtherReadingListDialog extends ExtendedBottomSheetDialogFragment {
    public enum InvokeSource implements EnumCode {
        BOOKMARK_BUTTON(0),
        CONTEXT_MENU(1),
        LINK_PREVIEW_MENU(2),
        PAGE_OVERFLOW_MENU(3),
        FEED(4),
        NEWS_ACTIVITY(5),
        READING_LIST_ACTIVITY(6),
        MOST_READ_ACTIVITY(7),
        RANDOM_ACTIVITY(8),
        ON_THIS_DAY_ACTIVITY(9),
        READ_MORE_BOOKMARK_BUTTON(10);

        private static final EnumCodeMap<InvokeSource> MAP = new EnumCodeMap<>(InvokeSource.class);

        private final int code;

        public static InvokeSource of(int code) {
            return MAP.get(code);
        }

        @Override public int code() {
            return code;
        }

        InvokeSource(int code) {
            this.code = code;
        }
    }

    private ReadingList fromList;
    private ReadingListAdapter adapter;
    private InvokeSource invokeSource;
    private CreateButtonClickListener createClickListener = new CreateButtonClickListener();

    private List<ReadingList> readingLists = new ArrayList<>();

    @Nullable private OnDismissSuccessListener dismissListener;
    private ReadingListItemCallback listItemCallback = new ReadingListItemCallback();

    public interface OnDismissSuccessListener {
        void onDismiss(boolean success);
    }

    public static MergeWithOtherReadingListDialog newInstance(long list1,
                                                              InvokeSource source) {
        return newInstance(list1, source, null);
    }

    public static MergeWithOtherReadingListDialog newInstance(
                                                     long list1,
                                                     InvokeSource source,
                                                     @Nullable OnDismissSuccessListener listener) {
        MergeWithOtherReadingListDialog dialog = new MergeWithOtherReadingListDialog();
        Bundle args = new Bundle();
        args.putLong("list1", list1);
        args.putInt("source", source.code());
        dialog.setArguments(args);
        dialog.setOnDismissListener(listener);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fromList = ReadingListDbHelper.instance().getReadingListById(getArguments().getLong("list1"));
        invokeSource = InvokeSource.of(getArguments().getInt("source"));
        adapter = new ReadingListAdapter();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_merge_with_other, container);

        RecyclerView readingListView = rootView.findViewById(R.id.list_of_lists);
        readingListView.setLayoutManager(new LinearLayoutManager(getActivity()));
        readingListView.setAdapter(adapter);

        View createButton = rootView.findViewById(R.id.create_button);
        createButton.setOnClickListener(createClickListener);

        if (savedInstanceState == null) {
            // Log a click event, but only the first time the dialog is shown.
            new ReadingListsFunnel().logMergeClick(invokeSource);
        }

        updateLists();
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        BottomSheetBehavior.from((View) getView().getParent()).setPeekHeight(DimenUtil
                .roundedDpToPx(DimenUtil.getDimension(R.dimen.readingListSheetPeekHeight)));
    }

    @Override
    public void dismiss() {
        super.dismiss();
    }

    private void dismiss(boolean success,
                         @Nullable ReadingList viewList,
                         @Nullable String message) {
        if (success) {
            ReadingListDbHelper.instance().deleteList(fromList);
            if (viewList != null && message != null) {
                showViewListSnackBar(viewList, message);
            }
        }
        if (dismissListener != null) {
            dismissListener.onDismiss(success);
        }
        dismiss();
    }

    public void setOnDismissListener(OnDismissSuccessListener listener) {
        dismissListener = listener;
    }

    private void updateLists() {
        CallbackTask.execute(() -> ReadingListDbHelper.instance().getAllListsExcept(Collections.singletonList(fromList.id())), new CallbackTask.DefaultCallback<List<ReadingList>>() {
            @Override
            public void success(List<ReadingList> lists) {
                if (getActivity() == null) {
                    return;
                }
                readingLists = lists;
                ReadingList.sort(readingLists, Prefs.getReadingListSortMode(ReadingList.SORT_BY_NAME_ASC));
                adapter.notifyDataSetChanged();
            }
        });
    }

    private class CreateButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (readingLists.size() >= Constants.MAX_READING_LISTS_LIMIT) {
                String message = getString(R.string.reading_lists_limit_message);
                dismiss(false, null, null);
                FeedbackUtil.makeSnackbar(getActivity(), message, FeedbackUtil.LENGTH_DEFAULT).show();
            } else {
                showCreateListDialog();
            }
        }
    }

    private void showCreateListDialog() {
        String title = getString(R.string.reading_list_name_sample);
        List<String> existingTitles = new ArrayList<>();
        for (ReadingList tempList : readingLists) {
            existingTitles.add(tempList.title());
        }
        ReadingListTitleDialog.readingListTitleDialog(requireContext(), title, "",
                existingTitles, (text, description) -> {
                    ReadingList list = ReadingListDbHelper.instance().createList(text, description);
                    mergeAndDismiss(fromList, list);
                }).show();
    }

    private void mergeAndDismiss(final ReadingList readingList1, final ReadingList readingList2) {

        if ((readingList1.pages().size() + readingList2.pages().size()) > SiteInfoClient.getMaxPagesPerReadingList()) {
            String message = getString(R.string.reading_list_article_limit_message, readingList1.title(), SiteInfoClient.getMaxPagesPerReadingList());
            FeedbackUtil.makeSnackbar(getActivity(), message, FeedbackUtil.LENGTH_DEFAULT).show();
            dismiss(false, null, null);
            return;
        }

        CallbackTask.execute(() -> ReadingListDbHelper.instance().addPagesToListIfNotExist(readingList2, readingList1.titles()), new CallbackTask.DefaultCallback<Integer>() {
            @Override
            public void success(Integer numAdded) {
                if (!isAdded()) {
                    return;
                }
                String message;
                if (numAdded == 0) {
                    message = getString(R.string.reading_list_already_contains_selection);
                } else {
                    message = String.format(getString(R.string.reading_list_merged_list_with_other_titled),
                            readingList1.title(),
                            readingList2.title());
                    new ReadingListsFunnel().logMergeWithList(readingList1, readingList2, invokeSource);
                }
                dismiss(true, readingList2, message);
            }
        });
    }

    private void showViewListSnackBar(@NonNull final ReadingList list, @NonNull String message) {
        FeedbackUtil.makeSnackbar(getActivity(), message, FeedbackUtil.LENGTH_DEFAULT)
                .setAction(R.string.reading_list_added_view_button, v -> v.getContext().startActivity(ReadingListActivity.newIntent(v.getContext(), list))).show();
    }

    private class ReadingListItemCallback implements ReadingListItemView.Callback {
        @Override
        public void onClick(@NonNull ReadingList readingList) {
            if (!Prefs.getMergeDoNotShowAgain()) {
                List<Boolean> dontShowAgain = new ArrayList<>();
                dontShowAgain.add(false);
                AlertDialog.Builder dialog = new AlertDialog.Builder(requireActivity());
                View mView = getLayoutInflater().inflate(R.layout.dialog_merge_warn, null);
                CheckBox mCheckBox = mView.findViewById(R.id.checkBox);
                mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        dontShowAgain.set(0, b);
                    }
                });
                dialog.setMessage(getString(R.string.reading_list_merge_confirm, fromList.title(), readingList.title(), fromList.title()));
                dialog.setView(mView);
                dialog.setPositiveButton(getString(android.R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Prefs.setMergeDoNotShowAgain(dontShowAgain.get(0));
                        mergeAndDismiss(fromList, readingList);
                    }
                }).setNegativeButton(getString(android.R.string.no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Prefs.setMergeDoNotShowAgain(dontShowAgain.get(0));
                    }
                }).create();
                dialog.show();
            } else {
                mergeAndDismiss(fromList, readingList);
            }
        }

        @Override
        public void onRename(@NonNull ReadingList readingList) {
        }

        public void onMerge(@NonNull ReadingList readingList) {
        }

        @Override
        public void onDelete(@NonNull ReadingList readingList) {
        }

        @Override
        public void onSaveAllOffline(@NonNull ReadingList readingList) {
        }

        @Override
        public void onRemoveAllOffline(@NonNull ReadingList readingList) {
        }
    }

    private class ReadingListItemHolder extends RecyclerView.ViewHolder {
        private ReadingListItemView itemView;

        ReadingListItemHolder(ReadingListItemView itemView) {
            super(itemView);
            this.itemView = itemView;
            itemView.setOverflowButtonVisible(false);
        }

        void bindItem(ReadingList readingList) {
            itemView.setReadingList(readingList, ReadingListItemView.Description.SUMMARY);
        }

        public ReadingListItemView getView() {
            return itemView;
        }
    }

    private final class ReadingListAdapter extends RecyclerView.Adapter<ReadingListItemHolder> {
        @Override
        public int getItemCount() {
            return readingLists.size();
        }

        @Override
        public ReadingListItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int pos) {
            ReadingListItemView view = new ReadingListItemView(getContext());
            return new ReadingListItemHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ReadingListItemHolder holder, int pos) {
            holder.bindItem(readingLists.get(pos));
        }

        @Override public void onViewAttachedToWindow(@NonNull ReadingListItemHolder holder) {
            super.onViewAttachedToWindow(holder);
            holder.getView().setCallback(listItemCallback);
        }

        @Override public void onViewDetachedFromWindow(@NonNull ReadingListItemHolder holder) {
            holder.getView().setCallback(null);
            super.onViewDetachedFromWindow(holder);
        }
    }
}
