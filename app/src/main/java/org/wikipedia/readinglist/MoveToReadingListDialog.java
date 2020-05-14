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
import org.wikipedia.settings.SiteInfoClient;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.log.L;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

import static org.wikipedia.Constants.INTENT_EXTRA_INVOKE_SOURCE;

public class MoveToReadingListDialog extends AddToReadingListDialog {

    public static MoveToReadingListDialog newInstance(@NonNull PageTitle title,
                                                      @NonNull Constants.InvokeSource source) {
        return newInstance(Collections.singletonList(title), source, null);
    }

    public static MoveToReadingListDialog newInstance(@NonNull List<PageTitle> titles,
                                                      @NonNull Constants.InvokeSource source) {
        return newInstance(titles, source, null);
    }

    public static MoveToReadingListDialog newInstance(@NonNull List<PageTitle> titles,
                                                      @NonNull Constants.InvokeSource source,
                                                      @Nullable DialogInterface.OnDismissListener listener) {
        MoveToReadingListDialog dialog = new MoveToReadingListDialog();
        Bundle args = new Bundle();
        args.putParcelableArrayList(PAGE_TITLE_LIST, new ArrayList<Parcelable>(titles));
        args.putSerializable(INTENT_EXTRA_INVOKE_SOURCE, source);
        dialog.setArguments(args);
        dialog.setOnDismissListener(listener);
        return dialog;
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
    void addAndDismiss(final ReadingList readingList, final PageTitle title) {

        if (readingList.pages().size() >= SiteInfoClient.getMaxPagesPerReadingList()) {
            String message = getString(R.string.reading_list_move_article_limit_message, readingList.title(), SiteInfoClient.getMaxPagesPerReadingList());
            FeedbackUtil.makeSnackbar(getActivity(), message, FeedbackUtil.LENGTH_DEFAULT).show();
            dismiss();
            return;
        }

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
                        ReadingListDbHelper.instance().movePageToList(readingList, title, true);
                        showViewListSnackBar(readingList, message);
                    }
                    dismiss();
                }, L::w));
    }

    @Override
    void addAndDismiss(final ReadingList readingList, final List<PageTitle> titles) {

        if ((readingList.pages().size() + titles.size()) > SiteInfoClient.getMaxPagesPerReadingList()) {
            String message = getString(R.string.reading_list_article_limit_message, readingList.title(), SiteInfoClient.getMaxPagesPerReadingList());
            FeedbackUtil.makeSnackbar(getActivity(), message, FeedbackUtil.LENGTH_DEFAULT).show();
            dismiss();
            return;
        }

        if (titles.size() == 1) {
            addAndDismiss(readingList, titles.get(0));
            return;
        }

        disposables.add(Observable.fromCallable(() -> ReadingListDbHelper.instance().movePagesToListIfNotExist(readingList, titles))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(numAdded -> {
                    String message;
                    if (numAdded == 0) {
                        message = getString(R.string.reading_list_articles_already_exist_message, readingList.title());
                    } else {
                        message = getString(R.string.reading_list_articles_moved_to_named, numAdded, readingList.title());
                        // TODO: add funnel?
                    }
                    showViewListSnackBar(readingList, message);
                    dismiss();
                }, L::w));
    }
}
