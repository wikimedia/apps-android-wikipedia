package org.wikipedia.categories;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.wikipedia.R;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.page.ExtendedBottomSheetDialogFragment;
import org.wikipedia.page.PageTitle;
import org.wikipedia.readinglist.database.ReadingList;
import org.wikipedia.util.log.L;
import org.wikipedia.views.PageItemView;
import org.wikipedia.views.WikiErrorView;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static org.wikipedia.util.L10nUtil.setConditionalLayoutDirection;

public class CategoryDialog extends ExtendedBottomSheetDialogFragment {
    private static final String TITLE = "title";

    @BindView(R.id.dialog_categories_progress) ProgressBar progressBar;
    @BindView(R.id.categories_dialog_title) TextView titleText;
    @BindView(R.id.categories_none_found) View categoriesEmptyView;
    @BindView(R.id.categories_error) WikiErrorView errorView;
    @BindView(R.id.categories_recycler) RecyclerView categoriesRecycler;
    private Unbinder unbinder;

    private PageTitle pageTitle;
    private List<MwQueryPage.Category> categoryList = new ArrayList<>();
    private ItemCallback itemCallback = new ItemCallback();
    private CompositeDisposable disposables = new CompositeDisposable();

    public static CategoryDialog newInstance(@NonNull PageTitle title) {
        CategoryDialog dialog = new CategoryDialog();
        Bundle args = new Bundle();
        args.putParcelable(TITLE, title);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageTitle = getArguments().getParcelable(TITLE);
    }

    @Override
    public void onDestroy() {
        disposables.clear();
        if (unbinder != null) {
            unbinder.unbind();
            unbinder = null;
        }
        super.onDestroy();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.dialog_categories, container);
        unbinder = ButterKnife.bind(this, rootView);

        categoriesRecycler.setLayoutManager(new LinearLayoutManager(requireActivity()));
        categoriesRecycler.setAdapter(new CategoryAdapter());

        //titleText.setText(StringUtil.fromHtml(pageTitle.getDisplayText()));
        setConditionalLayoutDirection(rootView, pageTitle.getWikiSite().languageCode());

        loadCategories();
        return rootView;
    }

    private void loadCategories() {
        errorView.setVisibility(View.GONE);
        categoriesEmptyView.setVisibility(View.GONE);
        categoriesRecycler.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        disposables.add(ServiceFactory.get(pageTitle.getWikiSite()).getCategories(pageTitle.getPrefixedText())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> progressBar.setVisibility(View.GONE))
                .subscribe(response -> {
                    categoryList.clear();
                    for (MwQueryPage.Category cat : response.query().firstPage().categories()) {
                        if (!cat.hidden()) {
                            categoryList.add(cat);
                        }
                    }
                    layOutCategories();
                }, throwable -> {
                    errorView.setError(throwable);
                    errorView.setVisibility(View.VISIBLE);
                    L.e(throwable);
                }));
    }

    private void layOutCategories() {
        if (categoryList.isEmpty()) {
            categoriesEmptyView.setVisibility(View.VISIBLE);
            categoriesRecycler.setVisibility(View.GONE);
        }
        categoriesRecycler.setVisibility(View.VISIBLE);
        categoriesEmptyView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
    }


    private class CategoryItemHolder extends RecyclerView.ViewHolder {
        private PageItemView itemView;

        CategoryItemHolder(PageItemView itemView) {
            super(itemView);
            this.itemView = itemView;
        }

        void bindItem(MwQueryPage.Category category) {
            PageTitle title = new PageTitle(category.title(), pageTitle.getWikiSite());
            itemView.setItem(title);
            itemView.setTitle(title.getText().replace("_", " "));
        }

        public PageItemView getView() {
            return itemView;
        }
    }

    private final class CategoryAdapter extends RecyclerView.Adapter<CategoryItemHolder> {
        @Override
        public int getItemCount() {
            return categoryList.size();
        }

        @Override
        public CategoryItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int pos) {
            PageItemView<PageTitle> view = new PageItemView<>(requireContext());
            return new CategoryItemHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull CategoryItemHolder holder, int pos) {
            holder.bindItem(categoryList.get(pos));
        }

        @Override public void onViewAttachedToWindow(@NonNull CategoryItemHolder holder) {
            super.onViewAttachedToWindow(holder);
            holder.getView().setCallback(itemCallback);
        }

        @Override public void onViewDetachedFromWindow(@NonNull CategoryItemHolder holder) {
            holder.getView().setCallback(null);
            super.onViewDetachedFromWindow(holder);
        }
    }

    private class ItemCallback implements PageItemView.Callback<PageTitle> {

        @Override
        public void onClick(@Nullable PageTitle item) {
            if (item != null) {
                startActivity(CategoryActivity.newIntent(requireActivity(), item));
            }
        }

        @Override
        public boolean onLongClick(@Nullable PageTitle item) {
            return false;
        }

        @Override
        public void onThumbClick(@Nullable PageTitle item) {
            if (item != null) {
                startActivity(CategoryActivity.newIntent(requireActivity(), item));
            }
        }

        @Override
        public void onActionClick(@Nullable PageTitle item, @NonNull View view) {
        }

        @Override
        public void onListChipClick(@Nullable ReadingList readingList) {
        }
    }
}
