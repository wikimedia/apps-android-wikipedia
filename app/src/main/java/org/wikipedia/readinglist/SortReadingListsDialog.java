package org.wikipedia.readinglist;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.activity.FragmentUtil;
import org.wikipedia.page.ExtendedBottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class SortReadingListsDialog extends ExtendedBottomSheetDialogFragment {
    public interface Callback {
        void onSortOptionClick(int position);
    }
    private static final String SORT_OPTION = "sortOption";
    private ReadingListSortAdapter adapter;
    private List<String> sortOptions = new ArrayList<>();
    private int chosenSortOption;

    public static SortReadingListsDialog newInstance(int sortOption) {
        SortReadingListsDialog dialog = new SortReadingListsDialog();
        Bundle args = new Bundle();
        args.putInt(SORT_OPTION, sortOption);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        chosenSortOption = getArguments().getInt(SORT_OPTION);
        sortOptions = Arrays.asList(getResources().getStringArray(R.array.sort_options));
        adapter = new ReadingListSortAdapter();

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_sort_reading_lists, container);
        RecyclerView sortOptionsList = rootView.findViewById(R.id.sort_options_list);
        sortOptionsList.setAdapter(adapter);
        sortOptionsList.setLayoutManager(new LinearLayoutManager(getActivity()));

        return rootView;
    }

    private final class ReadingListSortAdapter extends RecyclerView.Adapter<SortReadingListsDialog.SortItemHolder> {
        @Override
        public int getItemCount() {
            return sortOptions.size();
        }

        @NonNull
        @Override
        public SortItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int pos) {
            Context context = parent.getContext();
            LayoutInflater inflater = LayoutInflater.from(context);
            View view = inflater.inflate(R.layout.view_reading_lists_sort_options_item, parent, false);
            return new SortItemHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SortItemHolder holder, int pos) {
            holder.bindItem(pos);

            holder.itemView.setOnClickListener((View view) -> {
                if (callback() != null) {
                    callback().onSortOptionClick(pos);
                }
                dismiss();
            });
        }
    }

    private class SortItemHolder extends RecyclerView.ViewHolder {
        private TextView sortOptionTextView;
        private ImageView checkImage;

        SortItemHolder(View itemView) {
            super(itemView);
            sortOptionTextView = itemView.findViewById(R.id.sort_type);
            checkImage = itemView.findViewById(R.id.check);
        }

        void bindItem(int sortOption) {
            sortOptionTextView.setText(sortOptions.get(sortOption));
            if (chosenSortOption == sortOption) {
                checkImage.setVisibility(View.VISIBLE);
            } else {
                checkImage.setVisibility(View.GONE);
            }
        }
    }

    @Nullable
    private Callback callback() {
        return FragmentUtil.getCallback(this, Callback.class);
    }
}
