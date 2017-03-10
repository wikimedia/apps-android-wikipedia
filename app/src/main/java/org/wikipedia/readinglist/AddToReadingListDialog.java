package org.wikipedia.readinglist;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.analytics.ReadingListsFunnel;
import org.wikipedia.concurrency.CallbackTask;
import org.wikipedia.model.EnumCode;
import org.wikipedia.model.EnumCodeMap;
import org.wikipedia.page.ExtendedBottomSheetDialogFragment;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.page.ReadingListPage;
import org.wikipedia.readinglist.page.database.ReadingListDaoProxy;
import org.wikipedia.readinglist.page.database.ReadingListPageDao;
import org.wikipedia.settings.Prefs;
import org.wikipedia.util.FeedbackUtil;

import java.util.ArrayList;
import java.util.List;

public class AddToReadingListDialog extends ExtendedBottomSheetDialogFragment {
    public enum InvokeSource implements EnumCode {
        BOOKMARK_BUTTON(0),
        CONTEXT_MENU(1),
        LINK_PREVIEW_MENU(2),
        PAGE_OVERFLOW_MENU(3),
        FEED(4),
        NEWS_ACTIVITY(5);

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

    private PageTitle pageTitle;
    private ReadingListAdapter adapter;
    private View listsContainer;
    private View onboardingContainer;
    private View onboardingButton;
    private InvokeSource invokeSource;
    private CreateButtonClickListener createClickListener = new CreateButtonClickListener();
    private ReadingLists readingLists = new ReadingLists();
    @Nullable private DialogInterface.OnDismissListener dismissListener;
    private ReadingListItemCallback listItemCallback = new ReadingListItemCallback();

    public static AddToReadingListDialog newInstance(@NonNull PageTitle title, InvokeSource source) {
        return newInstance(title, source, null);
    }

    public static AddToReadingListDialog newInstance(@NonNull PageTitle title, InvokeSource source,
                                                     @Nullable DialogInterface.OnDismissListener listener) {
        AddToReadingListDialog dialog = new AddToReadingListDialog();
        Bundle args = new Bundle();
        args.putParcelable("title", title);
        args.putInt("source", source.code());
        dialog.setArguments(args);
        dialog.setOnDismissListener(listener);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageTitle = getArguments().getParcelable("title");
        invokeSource = InvokeSource.of(getArguments().getInt("source"));
        adapter = new ReadingListAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_add_to_reading_list, container);

        listsContainer = rootView.findViewById(R.id.lists_container);
        onboardingContainer = rootView.findViewById(R.id.onboarding_container);
        onboardingButton = rootView.findViewById(R.id.onboarding_button);
        checkAndShowOnboarding();

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

        if (savedInstanceState == null) {
            // Log a click event, but only the first time the dialog is shown.
            new ReadingListsFunnel(pageTitle.getWikiSite()).logAddClick(invokeSource);
        }

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

    private void checkAndShowOnboarding() {
        boolean isOnboarding = WikipediaApp.getInstance().getOnboardingStateMachine().isReadingListTutorialEnabled();
        onboardingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onboardingContainer.setVisibility(View.GONE);
                listsContainer.setVisibility(View.VISIBLE);
                WikipediaApp.getInstance().getOnboardingStateMachine().setReadingListTutorial();
                if (readingLists.isEmpty()) {
                    showCreateListDialog();
                }
            }
        });
        listsContainer.setVisibility(isOnboarding ? View.GONE : View.VISIBLE);
        onboardingContainer.setVisibility(isOnboarding ? View.VISIBLE : View.GONE);
    }

    private void updateLists() {
        ReadingList.DAO.queryMruLists(null, new CallbackTask.Callback<List<ReadingList>>() {
            @Override
            public void success(List<ReadingList> rows) {
                readingLists.set(rows);
                readingLists.sort(Prefs.getReadingListSortMode(ReadingLists.SORT_BY_NAME_ASC));
                adapter.notifyDataSetChanged();
            }
        });
    }

    private class CreateButtonClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            showCreateListDialog();
        }
    }

    private void showCreateListDialog() {
        String title = getString(R.string.reading_list_name_sample);
        long now = System.currentTimeMillis();
        final ReadingList list = ReadingList
                .builder()
                .key(ReadingListDaoProxy.listKey(title))
                .title(title)
                .mtime(now)
                .atime(now)
                .description(null)
                .pages(new ArrayList<ReadingListPage>())
                .build();

        ReadingListTitleDialog.readingListTitleDialog(getContext(), list.getTitle(),
                readingLists.getTitles(), new ReadingListTitleDialog.Callback() {
            @Override
            public void onSuccess(@NonNull CharSequence text) {
                list.setTitle(text.toString());
                ReadingList.DAO.addList(list);
                addAndDismiss(list);
            }
        }).show();
    }

    private void addAndDismiss(final ReadingList readingList) {
        final ReadingListPage page = findOrCreatePage(readingList, pageTitle);
        ReadingList.DAO.listContainsTitleAsync(readingList, page, new CallbackTask.Callback<Boolean>() {
            @Override
            public void success(Boolean contains) {
                if (isAdded()) {
                    String message;
                    if (contains) {
                        message = getString(R.string.reading_list_already_exists);
                    } else {
                        message = TextUtils.isEmpty(readingList.getTitle())
                                ? getString(R.string.reading_list_added_to_unnamed)
                                : String.format(getString(R.string.reading_list_added_to_named),
                                readingList.getTitle());
                        new ReadingListsFunnel(pageTitle.getWikiSite()).logAddToList(readingList, readingLists.size(), invokeSource);
                        ReadingList.DAO.makeListMostRecent(readingList);
                    }

                    showViewListSnackBar(readingList, message);
                    ReadingList.DAO.addTitleToList(readingList, page);
                    dismiss();
                }
            }
        });
    }

    private void showViewListSnackBar(@NonNull final ReadingList readingList, @NonNull String message) {
        FeedbackUtil.makeSnackbar(getActivity(), message, FeedbackUtil.LENGTH_DEFAULT)
                .setAction(R.string.reading_list_added_view_button, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        v.getContext().startActivity(ReadingListActivity.newIntent(v.getContext(), readingList));
                    }
                }).show();
    }

    @NonNull private ReadingListPage findOrCreatePage(ReadingList readingList, PageTitle title) {
        ReadingListPage page = ReadingListPageDao.instance().findPage(ReadingListDaoProxy.key(title));
        if (page == null) {
            page = ReadingListDaoProxy.page(readingList, title);
        }
        return page;
    }

    private class ReadingListItemCallback implements ReadingListItemView.Callback {
        @Override
        public void onClick(@NonNull ReadingList readingList) {
            addAndDismiss(readingList);
        }

        @Override
        public void onRename(@NonNull final ReadingList readingList) {
        }

        @Override
        public void onEditDescription(@NonNull final ReadingList readingList) {
        }

        @Override
        public void onDelete(@NonNull ReadingList readingList) {
        }
    }

    private class ReadingListItemHolder extends RecyclerView.ViewHolder {
        private ReadingListItemView itemView;

        ReadingListItemHolder(ReadingListItemView itemView) {
            super(itemView);
            this.itemView = itemView;
            itemView.setOverflowButtonVisible(false);
        }

        public void bindItem(ReadingList readingList) {
            itemView.setReadingList(readingList);
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
        public ReadingListItemHolder onCreateViewHolder(ViewGroup parent, int pos) {
            ReadingListItemView view = new ReadingListItemView(getContext());
            return new ReadingListItemHolder(view);
        }

        @Override
        public void onBindViewHolder(ReadingListItemHolder holder, int pos) {
            holder.bindItem(readingLists.get(pos));
        }

        @Override public void onViewAttachedToWindow(ReadingListItemHolder holder) {
            super.onViewAttachedToWindow(holder);
            holder.getView().setCallback(listItemCallback);
        }

        @Override public void onViewDetachedFromWindow(ReadingListItemHolder holder) {
            holder.getView().setCallback(null);
            super.onViewDetachedFromWindow(holder);
        }
    }
}
