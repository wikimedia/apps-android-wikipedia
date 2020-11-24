package org.wikipedia.readinglist;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import org.wikipedia.Constants;
import org.wikipedia.Constants.InvokeSource;
import org.wikipedia.R;
import org.wikipedia.analytics.ReadingListsFunnel;
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

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static org.wikipedia.Constants.INTENT_EXTRA_INVOKE_SOURCE;

public class AddToReadingListDialog extends ExtendedBottomSheetDialogFragment {
    private List<PageTitle> titles;
    private ReadingListAdapter adapter;
    private View listsContainer;
    private View onboardingContainer;
    private View onboardingButton;
    private final CreateButtonClickListener createClickListener = new CreateButtonClickListener();
    private boolean showDefaultList;
    List<ReadingList> readingLists = new ArrayList<>();
    private final List<ReadingList> displayedLists = new ArrayList<>();
    InvokeSource invokeSource;
    CompositeDisposable disposables = new CompositeDisposable();

    static final String PAGE_TITLE_LIST = "pageTitleList";
    static final String SHOW_DEFAULT_LIST = "showDefaultList";

    @Nullable private DialogInterface.OnDismissListener dismissListener;
    private final ReadingListItemCallback listItemCallback = new ReadingListItemCallback();

    public static AddToReadingListDialog newInstance(@NonNull PageTitle title,
                                                     @NonNull InvokeSource source) {
        return newInstance(title, source, null);
    }

    public static AddToReadingListDialog newInstance(@NonNull PageTitle title,
                                                     @NonNull InvokeSource source,
                                                     @Nullable DialogInterface.OnDismissListener listener) {
        return newInstance(Collections.singletonList(title), source, listener);
    }

    public static AddToReadingListDialog newInstance(@NonNull List<PageTitle> titles,
                                                     @NonNull InvokeSource source) {
        return newInstance(titles, source, null);
    }

    public static AddToReadingListDialog newInstance(@NonNull List<PageTitle> titles,
                                                     @NonNull InvokeSource source,
                                                     @Nullable DialogInterface.OnDismissListener listener) {
        AddToReadingListDialog dialog = new AddToReadingListDialog();
        Bundle args = new Bundle();
        args.putParcelableArrayList(PAGE_TITLE_LIST, new ArrayList<Parcelable>(titles));
        args.putSerializable(INTENT_EXTRA_INVOKE_SOURCE, source);
        args.putBoolean(SHOW_DEFAULT_LIST, true);
        dialog.setArguments(args);
        dialog.setOnDismissListener(listener);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        titles = getArguments().getParcelableArrayList(PAGE_TITLE_LIST);
        invokeSource = (InvokeSource) getArguments().getSerializable(INTENT_EXTRA_INVOKE_SOURCE);
        showDefaultList = getArguments().getBoolean(SHOW_DEFAULT_LIST);
        adapter = new ReadingListAdapter();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_add_to_reading_list, container);

        listsContainer = rootView.findViewById(R.id.lists_container);
        onboardingContainer = rootView.findViewById(R.id.onboarding_container);
        onboardingButton = rootView.findViewById(R.id.onboarding_button);

        RecyclerView readingListView = rootView.findViewById(R.id.list_of_lists);
        readingListView.setLayoutManager(new LinearLayoutManager(getActivity()));
        readingListView.setAdapter(adapter);

        View createButton = rootView.findViewById(R.id.create_button);
        createButton.setOnClickListener(createClickListener);

        // Log a click event, but only the first time the dialog is shown.
        logClick(savedInstanceState);

        onboardingContainer.setVisibility(View.GONE);
        listsContainer.setVisibility(View.GONE);
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
    public void onDestroyView() {
        disposables.clear();
        super.onDestroyView();
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

    private void checkAndShowOnboarding() {
        boolean isOnboarding = Prefs.isReadingListTutorialEnabled();
        if (isOnboarding) {
            // Don't show onboarding message if the user already has items in lists (i.e. from syncing).
            for (ReadingList list : readingLists) {
                if (!list.pages().isEmpty()) {
                    isOnboarding = false;
                    Prefs.setReadingListTutorialEnabled(false);
                    break;
                }
            }
        }
        onboardingButton.setOnClickListener((v) -> {
            onboardingContainer.setVisibility(View.GONE);
            listsContainer.setVisibility(View.VISIBLE);
            Prefs.setReadingListTutorialEnabled(false);
            if (displayedLists.isEmpty()) {
                showCreateListDialog();
            }
        });
        listsContainer.setVisibility(isOnboarding ? View.GONE : View.VISIBLE);
        onboardingContainer.setVisibility(isOnboarding ? View.VISIBLE : View.GONE);
    }

    private void updateLists() {
        disposables.add(Observable.fromCallable(() -> ReadingListDbHelper.instance().getAllLists())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(lists -> {
                    readingLists = lists;
                    displayedLists.addAll(readingLists);
                    if (!showDefaultList && !displayedLists.isEmpty()) {
                        displayedLists.remove(0);
                    }
                    ReadingList.sort(displayedLists, Prefs.getReadingListSortMode(ReadingList.SORT_BY_NAME_ASC));
                    adapter.notifyDataSetChanged();
                    checkAndShowOnboarding();
                }, L::w));
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
        List<String> existingTitles = new ArrayList<>();
        for (ReadingList tempList : readingLists) {
            existingTitles.add(tempList.title());
        }
        ReadingListTitleDialog.readingListTitleDialog(requireActivity(), "", "",
                existingTitles, (text, description) -> {
                    ReadingList list = ReadingListDbHelper.instance().createList(text, description);
                    addAndDismiss(list, titles);
                }).show();
    }

    private void addAndDismiss(final ReadingList readingList, final List<PageTitle> titles) {
        if ((readingList.pages().size() + titles.size()) > SiteInfoClient.getMaxPagesPerReadingList()) {
            String message = getString(R.string.reading_list_article_limit_message, readingList.title(), SiteInfoClient.getMaxPagesPerReadingList());
            FeedbackUtil.makeSnackbar(getActivity(), message, FeedbackUtil.LENGTH_DEFAULT).show();
            dismiss();
            return;
        }
        commitChanges(readingList, titles);
    }

    void logClick(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            new ReadingListsFunnel().logAddClick(invokeSource);
        }
    }

    void commitChanges(final ReadingList readingList, final List<PageTitle> titles) {
        disposables.add(Observable.fromCallable(() -> ReadingListDbHelper.instance().addPagesToListIfNotExist(readingList, titles))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(addedTitlesList -> {
                    String message;
                    if (addedTitlesList.isEmpty()) {
                        message = titles.size() == 1
                                ? getString(R.string.reading_list_article_already_exists_message, readingList.title(), titles.get(0).getDisplayText())
                                : getString(R.string.reading_list_articles_already_exist_message, readingList.title());
                    } else {
                        message = (addedTitlesList.size() == 1) ? getString(R.string.reading_list_article_added_to_named, addedTitlesList.get(0), readingList.title())
                                : getString(R.string.reading_list_articles_added_to_named, addedTitlesList.size(), readingList.title());
                        new ReadingListsFunnel().logAddToList(readingList, readingLists.size(), invokeSource);
                    }
                    showViewListSnackBar(readingList, message);
                    dismiss();
                }, L::w));
    }

    void showViewListSnackBar(@NonNull final ReadingList list, @NonNull String message) {
        FeedbackUtil.makeSnackbar(getActivity(), message, FeedbackUtil.LENGTH_DEFAULT)
                .setAction(R.string.reading_list_added_view_button, v -> v.getContext().startActivity(ReadingListActivity.newIntent(v.getContext(), list))).show();
    }

    private class ReadingListItemCallback implements ReadingListItemView.Callback {
        @Override
        public void onClick(@NonNull ReadingList readingList) {
            addAndDismiss(readingList, titles);
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

    private static class ReadingListItemHolder extends RecyclerView.ViewHolder {
        private final ReadingListItemView itemView;

        ReadingListItemHolder(ReadingListItemView itemView) {
            super(itemView);
            this.itemView = itemView;
            itemView.setLongClickable(false);
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
            return displayedLists.size();
        }

        @Override
        public ReadingListItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int pos) {
            ReadingListItemView view = new ReadingListItemView(getContext());
            return new ReadingListItemHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ReadingListItemHolder holder, int pos) {
            holder.bindItem(displayedLists.get(pos));
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
