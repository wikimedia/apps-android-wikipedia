package org.wikipedia.random;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.dataclient.ServiceFactory;
import org.wikipedia.dataclient.page.PageSummary;
import org.wikipedia.page.PageTitle;
import org.wikipedia.util.ImageUrlUtil;
import org.wikipedia.util.StringUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.FaceAndColorDetectImageView;
import org.wikipedia.views.GoneIfEmptyTextView;
import org.wikipedia.views.WikiErrorView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

import static org.wikipedia.Constants.PREFERRED_CARD_THUMBNAIL_SIZE;

public class RandomItemFragment extends Fragment {
    @BindView(R.id.random_item_container) ViewGroup containerView;
    @BindView(R.id.random_item_progress) View progressBar;
    @BindView(R.id.view_random_article_card_image) FaceAndColorDetectImageView imageView;
    @BindView(R.id.view_random_article_card_article_title) TextView articleTitleView;
    @BindView(R.id.view_random_article_card_article_subtitle) GoneIfEmptyTextView articleSubtitleView;
    @BindView(R.id.view_random_article_card_extract) TextView extractView;
    @BindView(R.id.random_item_error_view) WikiErrorView errorView;

    private static final float IMAGE_ASPECT_RATIO_PORTRAIT = 1.77f;
    private static final float IMAGE_ASPECT_RATIO_LANDSCAPE = 3.8f;
    private CompositeDisposable disposables = new CompositeDisposable();
    @Nullable private PageSummary summary;
    private int pagerPosition = -1;

    @NonNull
    public static RandomItemFragment newInstance() {
        return new RandomItemFragment();
    }

    void setPagerPosition(int position) {
        pagerPosition = position;
    }

    int getPagerPosition() {
        return pagerPosition;
    }

    boolean isLoadComplete() {
        return summary != null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_random_item, container, false);
        ButterKnife.bind(this, view);
        imageView.setLegacyVisibilityHandlingEnabled(true);
        imageView.setAspectRatio(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ? IMAGE_ASPECT_RATIO_LANDSCAPE : IMAGE_ASPECT_RATIO_PORTRAIT);
        errorView.setBackClickListener(v -> requireActivity().finish());
        errorView.setRetryClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            getRandomPage();
        });
        updateContents();
        if (summary == null) {
            getRandomPage();
        }
        return view;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }

    private void getRandomPage() {
        disposables.add(ServiceFactory.getRest(WikipediaApp.getInstance().getWikiSite()).getRandomSummary()
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
        containerView.setVisibility(View.GONE);
    }

    @OnClick(R.id.random_item_container) void onClick(View v) {
        if (getTitle() != null) {
            parent().onSelectPage(getTitle());
        }
    }

    private void updateContents() {
        errorView.setVisibility(View.GONE);
        containerView.setVisibility(summary == null ? View.GONE : View.VISIBLE);
        progressBar.setVisibility(summary == null ? View.VISIBLE : View.GONE);
        if (summary == null) {
            return;
        }
        articleTitleView.setText(StringUtil.fromHtml(summary.getDisplayTitle()));
        articleSubtitleView.setText(null); //summary.getDescription());
        extractView.setText(StringUtil.fromHtml(summary.getExtractHtml()));
        ViewTreeObserver observer = extractView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (!isAdded() || extractView == null) {
                    return;
                }
                int maxLines = extractView.getHeight() / extractView.getLineHeight() - 1;
                final int minLines = 3;
                extractView.setMaxLines(Math.max(maxLines, minLines));
                extractView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });

        imageView.loadImage(TextUtils.isEmpty(summary.getThumbnailUrl())
                ? null : Uri.parse(ImageUrlUtil.getUrlForPreferredSize(summary.getThumbnailUrl(), PREFERRED_CARD_THUMBNAIL_SIZE)));
    }

    @Nullable public PageTitle getTitle() {
        return summary == null ? null
                : new PageTitle(summary.getApiTitle(), WikipediaApp.getInstance().getWikiSite());
    }

    private RandomFragment parent() {
        return (RandomFragment) requireActivity().getSupportFragmentManager().getFragments().get(0);
    }
}
