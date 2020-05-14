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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
}
