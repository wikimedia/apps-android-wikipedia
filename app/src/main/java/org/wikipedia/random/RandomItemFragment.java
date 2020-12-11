package org.wikipedia.random;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.wikipedia.R;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.WikiSite;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.ImageUrlUtil;
import org.wikipedia.util.L10nUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.WikiArticleCardView;
import org.wikipedia.views.WikiErrorView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static org.wikipedia.Constants.PREFERRED_CARD_THUMBNAIL_SIZE;
import static org.wikipedia.random.RandomActivity.INTENT_EXTRA_WIKISITE;

public class RandomItemFragment extends Fragment {
    @BindView(R.id.random_item_progress) View progressBar;
    @BindView(R.id.random_item_wiki_article_card_view) WikiArticleCardView wikiArticleCardView;
    @BindView(R.id.random_item_error_view) WikiErrorView errorView;

    private CompositeDisposable disposables = new CompositeDisposable();
    @Nullable private PageSummary summary;
    private WikiSite wikiSite;

    public static final int EXTRACT_MAX_LINES = 4;

    @NonNull
    public static RandomItemFragment newInstance(@NonNull WikiSite wikiSite) {
        RandomItemFragment fragment = new RandomItemFragment();
        Bundle args = new Bundle();
        args.putParcelable(INTENT_EXTRA_WIKISITE, wikiSite);
        fragment.setArguments(args);
        return fragment;
    }

    boolean isLoadComplete() {
        return summary != null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wikiSite = getArguments().getParcelable(INTENT_EXTRA_WIKISITE);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_random_item, container, false);
        ButterKnife.bind(this, view);
        errorView.setBackClickListener(v -> requireActivity().finish());
        errorView.setRetryClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            getRandomPage();
        });
        updateContents();
        if (summary == null) {
            getRandomPage();
        }
        L10nUtil.setConditionalLayoutDirection(view, wikiSite.languageCode());
        return view;
    }

    @Override
    public void onDestroyView() {
        disposables.clear();
        super.onDestroyView();
    }

    private void getRandomPage() {
        disposables.add(ServiceFactory.getRest(wikiSite).getRandomSummary()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(pageSummary -> {
                    summary = pageSummary;
                    updateContents();
                    parent().onChildLoaded();
                }, this::setErrorState));
    }

    private void setErrorState(@NonNull Throwable t) {
        L.e(t);
        errorView.setError(t);
        errorView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        wikiArticleCardView.setVisibility(View.GONE);
    }

    @OnClick(R.id.random_item_wiki_article_card_view) void onClick(View v) {
        if (getTitle() != null) {
            parent().onSelectPage(getTitle(), wikiArticleCardView.getSharedElements());
        }
    }

    private void updateContents() {
        errorView.setVisibility(View.GONE);
        wikiArticleCardView.setVisibility(summary == null ? View.GONE : View.VISIBLE);
        progressBar.setVisibility(summary == null ? View.VISIBLE : View.GONE);
        if (summary == null) {
            return;
        }
        wikiArticleCardView.setTitle(summary.getDisplayTitle());
        wikiArticleCardView.setDescription(summary.getDescription());
        wikiArticleCardView.setExtract(summary.getExtract(), EXTRACT_MAX_LINES);

        if (TextUtils.isEmpty(summary.getThumbnailUrl())) {
            wikiArticleCardView.getImageContainer().setVisibility(View.GONE);
        } else {
            wikiArticleCardView.getImageContainer().setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    DimenUtil.leadImageHeightForDevice(getContext()) - DimenUtil.getToolbarHeightPx(getContext())));
            wikiArticleCardView.getImageContainer().setVisibility(View.VISIBLE);
            wikiArticleCardView.getImageView().loadImage(Uri.parse(ImageUrlUtil.getUrlForPreferredSize(summary.getThumbnailUrl(), PREFERRED_CARD_THUMBNAIL_SIZE)));
        }
    }

    @Nullable public PageTitle getTitle() {
        return summary == null ? null : summary.getPageTitle(wikiSite);
    }

    private RandomFragment parent() {
        return (RandomFragment) requireActivity().getSupportFragmentManager().getFragments().get(0);
    }
}
