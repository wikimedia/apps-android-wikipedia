package org.wikipedia.readinglist;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.analytics.ReadingListsFunnel;
import org.wikipedia.concurrency.CallbackTask;
import org.wikipedia.model.EnumCode;
import org.wikipedia.model.EnumCodeMap;
import org.wikipedia.page.ExtendedBottomSheetDialogFragment;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.database.ReadingList;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.settings.Prefs;
import org.wikipedia.settings.SiteInfoClient;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MoveToReadingListDialog extends ExtendedBottomSheetDialogFragment {
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
        ON_THIS_DAY_ACTIVITY(9);

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

    public interface OnDismissSuccessListener {
        void onDismiss(boolean success);
        void onMultipleInputDismiss(List<Integer> inputIndices);
    }

    private List<PageTitle> titles;
    private ReadingList from;
    private ReadingListAdapter adapter;
    private View listsContainer;
    //private View onboardingContainer;
    //private View onboardingButton;
    private InvokeSource invokeSource;
    private CreateButtonClickListener createClickListener = new CreateButtonClickListener();

    private List<ReadingList> readingLists = new ArrayList<>();

    @Nullable private OnDismissSuccessListener dismissListener;
    private ReadingListItemCallback listItemCallback = new ReadingListItemCallback();

    public static MoveToReadingListDialog newInstance(@NonNull PageTitle page, long fromList,
                                                      InvokeSource source) {
        return newInstance(Collections.singletonList(page), fromList, source,null);
    }

    public static MoveToReadingListDialog newInstance(@NonNull PageTitle page, ReadingList fromList,
                                                      InvokeSource source) {
        return newInstance(Collections.singletonList(page), fromList.id(), source,null);
    }

    public static MoveToReadingListDialog newInstance(@NonNull PageTitle page, long fromListName,
                                                      InvokeSource source,
                                                      @Nullable OnDismissSuccessListener listener) {
        return newInstance(Collections.singletonList(page), fromListName, source, listener);
    }

    public static MoveToReadingListDialog newInstance(@NonNull List<PageTitle> pages, long fromListName,
                                                      InvokeSource source,
                                                      @Nullable OnDismissSuccessListener listener) {
        MoveToReadingListDialog dialog = new MoveToReadingListDialog();
        Bundle args = new Bundle();
        args.putParcelableArrayList("titles", new ArrayList<Parcelable>(pages));
        args.putLong("fromList", fromListName);
        args.putInt("source", source.code());
        dialog.setArguments(args);
        dialog.setOnDismissListener(listener);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        titles = args.getParcelableArrayList("titles");
        from = ReadingListDbHelper.instance().getReadingListById(args.getLong("fromList"));
        L.e("from size() " + from.pages().size());
        invokeSource = InvokeSource.of(getArguments().getInt("source"));
        adapter = new ReadingListAdapter();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_move_to_reading_list, container);

        listsContainer = rootView.findViewById(R.id.lists_container);

        RecyclerView readingListView = rootView.findViewById(R.id.list_of_lists);
        readingListView.setLayoutManager(new LinearLayoutManager(getActivity()));
        readingListView.setAdapter(adapter);

        View createButton = rootView.findViewById(R.id.create_button);
        createButton.setOnClickListener(createClickListener);

        if (savedInstanceState == null) {
            // Log a click event, but only the first time the dialog is shown.
            new ReadingListsFunnel().logMoveClick(invokeSource);
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

    private void dismiss(boolean success) {
        if (dismissListener != null) {
            dismissListener.onDismiss(success);
        }
        dismiss();
    }

    private void dismiss(List<Integer> indices) {
        if (dismissListener != null) {
            dismissListener.onMultipleInputDismiss(indices);
        }
        dismiss();
    }

    public void setOnDismissListener(OnDismissSuccessListener listener) {
        dismissListener = listener;
    }

    private void updateLists() {
        CallbackTask.execute(() -> ReadingListDbHelper.instance().getAllLists(), new CallbackTask.DefaultCallback<List<ReadingList>>() {
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
                dismiss();
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
                    moveAndDismiss(from, list, titles);
                }).show();
    }

    private void moveAndDismiss(@NonNull final ReadingList fromList,
                                @NonNull final ReadingList toList,
                                @NonNull final PageTitle title) {

        if (toList.pages().size() >= SiteInfoClient.getMaxPagesPerReadingList()) {
            String message = getString(R.string.reading_list_article_limit_message, toList.title(), SiteInfoClient.getMaxPagesPerReadingList());
            FeedbackUtil.makeSnackbar(getActivity(), message, FeedbackUtil.LENGTH_DEFAULT).show();
            dismiss();
            return;
        }

        CallbackTask.execute(() -> ReadingListDbHelper.instance().pageExistsInList(toList, title), new CallbackTask.DefaultCallback<Boolean>() {
            @Override
            public void success(Boolean exists) {
                if (!isAdded()) {
                    return;
                }
                String message;
                boolean success = false;
                if (exists) {
                    message = getString(R.string.reading_list_already_exists);
                    showViewListSnackBar(toList, message);
                } else {
                    message = String.format(getString(R.string.reading_list_moved_to_named), toList.title());
                    new ReadingListsFunnel(title.getWikiSite()).logMoveToList(fromList, toList, 1, invokeSource);

                    ReadingListDbHelper.instance().movePageToList(fromList, toList, title, true);
                    showViewListSnackBar(toList, message);

                    success = true;
                }

                dismiss(success);
            }
        });
    }

    private void moveAndDismiss(@NonNull final ReadingList fromList,
                                @NonNull final ReadingList toList,
                                @NonNull final List<PageTitle> titles) {

        if ((toList.pages().size() + titles.size()) > SiteInfoClient.getMaxPagesPerReadingList()) {
            String message = getString(R.string.reading_list_article_limit_message, toList.title(), SiteInfoClient.getMaxPagesPerReadingList());
            FeedbackUtil.makeSnackbar(getActivity(), message, FeedbackUtil.LENGTH_DEFAULT).show();
            dismiss();
            return;
        }

        CallbackTask.execute(() -> ReadingListDbHelper.instance().addPagesToListIfNotExist(toList, titles), new CallbackTask.DefaultCallback<Integer>() {
            @Override
            public void success(Integer numAdded) {
                if (!isAdded()) {
                    return;
                }
                String message;
                List<Integer> indicesMoved = new ArrayList<>();
                if (numAdded == 0) {
                    message = getString(R.string.reading_list_already_contains_selection);
                } else {
                    message = String.format(getString(R.string.reading_list_moved_articles_list_titled), numAdded,
                            toList.title());
                    new ReadingListsFunnel().logMoveToList(fromList, toList, numAdded, invokeSource);
                    for (int i = 0; i < titles.size(); i++) {
                        //if (ReadingListDbHelper.instance().movePageToList(fromList, toList, titles.get(i), true)) {
                            indicesMoved.add(i);
                        //}
                    }
                }
                showViewListSnackBar(toList, message);
                dismiss(indicesMoved);
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
            moveAndDismiss(from, readingList, titles);
        }

        @Override
        public void onRename(@NonNull ReadingList readingList) {
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
