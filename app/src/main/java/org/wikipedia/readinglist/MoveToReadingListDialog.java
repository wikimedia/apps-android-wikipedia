package org.wikipedia.readinglist;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.wikipedia.Constants;
import org.wikipedia.R;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.database.ReadingList;
import org.wikipedia.readinglist.database.ReadingListDbHelper;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static org.wikipedia.Constants.INTENT_EXTRA_INVOKE_SOURCE;

public class MoveToReadingListDialog extends AddToReadingListDialog {

    private static final String SOURCE_READING_LIST_ID = "sourceReadingListId";
    private long sourceReadingListId;

    public static MoveToReadingListDialog newInstance(long sourceReadingListId,
                                                      @NonNull PageTitle title,
                                                      @NonNull Constants.InvokeSource source) {
        return newInstance(sourceReadingListId, Collections.singletonList(title), source, null);
    }

    public static MoveToReadingListDialog newInstance(long sourceReadingListId,
                                                      @NonNull List<PageTitle> titles,
                                                      @NonNull Constants.InvokeSource source) {
        return newInstance(sourceReadingListId, titles, source, null);
    }

    public static MoveToReadingListDialog newInstance(long sourceReadingListId,
                                                      @NonNull List<PageTitle> titles,
                                                      @NonNull Constants.InvokeSource source,
                                                      @Nullable DialogInterface.OnDismissListener listener) {
        MoveToReadingListDialog dialog = new MoveToReadingListDialog();
        Bundle args = new Bundle();
        args.putParcelableArrayList(PAGE_TITLE_LIST, new ArrayList<Parcelable>(titles));
        args.putSerializable(INTENT_EXTRA_INVOKE_SOURCE, source);
        args.putLong(SOURCE_READING_LIST_ID, sourceReadingListId);
        dialog.setArguments(args);
        dialog.setOnDismissListener(listener);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sourceReadingListId = getArguments().getLong(SOURCE_READING_LIST_ID);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View parentView = super.onCreateView(inflater, container, savedInstanceState);
        TextView dialogTitle = parentView.findViewById(R.id.dialog_title);
        dialogTitle.setText(R.string.reading_list_move_to);
        return parentView;
    }

    @Override
    void execute(final ReadingList readingList, final PageTitle title) {
        disposables.add(Observable.fromCallable(() -> ReadingListDbHelper.instance().pageExistsInList(readingList, title))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(exists -> {
                    String message;
                    if (exists) {
                        message = getString(R.string.reading_list_article_already_exists_message, readingList.title(), title.getDisplayText());
                        showViewListSnackBar(readingList, message);
                    } else {
                        message = getString(R.string.reading_list_article_moved_to_named, title.getDisplayText(), readingList.title());
                        // TODO: add funnel?
                        ReadingListDbHelper.instance().movePageToList(sourceReadingListId, readingList, title, true);
                        showViewListSnackBar(readingList, message);
                    }
                    dismiss();
                }, L::w));
    }

    @Override
    void execute(final ReadingList readingList, final List<PageTitle> titles) {
        disposables.add(Observable.fromCallable(() -> ReadingListDbHelper.instance().movePagesToListIfNotExist(sourceReadingListId, readingList, titles))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(movedTitlesList -> {
                    String message;
                    if (movedTitlesList.isEmpty()) {
                        message = getString(R.string.reading_list_articles_already_exist_message, readingList.title());
                    } else {
                        message = (movedTitlesList.size() == 1) ? getString(R.string.reading_list_article_moved_to_named, movedTitlesList.get(0), readingList.title())
                                : getString(R.string.reading_list_articles_moved_to_named, movedTitlesList.size(), readingList.title());
                        // TODO: add funnel?
                    }
                    showViewListSnackBar(readingList, message);
                    dismiss();
                }, L::w));
    }
}
