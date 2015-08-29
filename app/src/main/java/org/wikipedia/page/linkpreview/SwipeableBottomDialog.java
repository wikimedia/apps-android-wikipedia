package org.wikipedia.page.linkpreview;

import org.wikipedia.R;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

/*
 A few notes about the geometry of this special dialog class:
 It works by using a ListView to allow the user to smoothly scroll the dialog contents vertically
 along the screen. The ListView contains four items, three of which are transparent spaces, and
 the fourth being the actual dialog layout. Here's a breakdown of the items:

 |---------------------------|
 |   dismiss trigger item    | <-- When the user scrolls to this item, the dialog is dismissed.
 |---------------------------|
 |                           |
 |    transparent space      | <-- Allows the user to scroll the dialog contents off-screen
 |                           |
 |---------------------------|    ---------------
 |                           |           ^
 |                           |           |
 |    transparent space      | <---------|------------ This space is what we tell the ListView
 |                           |           |             to scroll to when the dialog is shown,
 |                           |     screen height       so that the dialog contents are in view.
 |                           |           |
 |---------------------------|           |
 | ////////////////////////  |           |
 | [actual dialog contents]  |           |
 | ////////////////////////  |           v
 |---------------------------|    ----------------

 The space views need to be created dynamically (instead of inflating from xml) because we
 allow the caller to specify an arbitrary "peek height" with which the dialog contents will peek
 out from the bottom, which can be shorter than the actual height of the dialog xml.
 */
public abstract class SwipeableBottomDialog extends DialogFragment {
    private final List<View> dialogViews = new ArrayList<>();
    private ListView dialogListView;
    private int dialogPeekHeight;

    private final AbsListView.OnScrollListener onScrollListener
            = new AbsListView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            // don't need this
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
                             int totalItemCount) {
            if (firstVisibleItem == 0) {
                dismiss();
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        int displayWidth = getDialogWidth();
        int displayHeight = inflater.getContext().getResources().getDisplayMetrics().heightPixels;

        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.dialog_bottom_swipe, container);

        dialogViews.add(makeSpaceView(displayWidth, 1));
        dialogViews.add(makeSpaceView(displayWidth, dialogPeekHeight));
        dialogViews.add(makeSpaceView(displayWidth, displayHeight - dialogPeekHeight));
        dialogViews.add(inflateDialogView(inflater, container));

        dialogListView = (ListView) rootView.findViewById(R.id.bottom_swipe_container_list);
        dialogListView.setAdapter(new SwipeableAdapter());
        dialogListView.setSelection(2);

        // For some reason, if we call setOnScrollListener() without post()ing, the listener
        // doesn't actually seem to get set, and has no effect. I've tried setting it in all the
        // other initialization methods (e.g. onCreateDialog, onStart), but this is the only
        // way that worked.
        rootView.post(new Runnable() {
            @Override
            public void run() {
                dialogListView.setOnScrollListener(onScrollListener);
            }
        });

        return rootView;
    }

    protected abstract View inflateDialogView(LayoutInflater inflater, ViewGroup container);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, R.style.BottomDialog);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.getWindow().setLayout(getDialogWidth(), ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    public void setDialogPeekHeight(int height) {
        dialogPeekHeight = height;
    }

    private int getDialogWidth() {
        return Math.min(getResources().getDisplayMetrics().widthPixels,
                (int) getResources().getDimension(R.dimen.swipeableDialogMaxWidth));
    }

    private View makeSpaceView(int width, int height) {
        View view = new View(getActivity());
        view.setLayoutParams(new ListView.LayoutParams(width, height));
        view.setClickable(true);
        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    dismiss();
                }
                return true;
            }
        });
        return view;
    }

    private final class SwipeableAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return dialogViews.size();
        }

        @Override
        public Object getItem(int position) {
            return dialogViews.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return dialogViews.get(position);
        }
    }
}
