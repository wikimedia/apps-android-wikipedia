package org.wikipedia.offline;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.WikipediaApp;
import org.wikipedia.gallery.MediaDownloadReceiver;
import org.wikipedia.history.HistoryEntry;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.page.PageActivity;
import org.wikipedia.page.PageTitle;
import org.wikipedia.staticdata.MainPageNameData;
import org.wikipedia.util.DeviceUtil;
import org.wikipedia.util.FeedbackUtil;
import org.wikipedia.util.GradientUtil;
import org.wikipedia.util.log.L;
import org.wikipedia.views.FaceAndColorDetectImageView;

import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static org.wikipedia.offline.CompilationDetailActivity.EXTRA_COMPILATION;
import static org.wikipedia.util.DateUtil.getShortDateString;
import static org.wikipedia.util.FileUtil.bytesToUserVisibleUnit;

public class CompilationDetailFragment extends DownloadObserverFragment {
    @BindView(R.id.compilation_detail_toolbar) Toolbar toolbar;
    @BindView(R.id.compilation_detail_header_image) FaceAndColorDetectImageView imageView;
    @BindView(R.id.compilation_detail_header_gradient) View gradientView;
    @BindView(R.id.view_compilation_info_title) TextView nameView;
    @BindView(R.id.view_compilation_info_date_size) TextView dateSizeView;
    @BindView(R.id.view_compilation_info_summary) TextView summaryView;
    @BindView(R.id.view_compilation_info_description) TextView descriptionView;
    @BindView(R.id.button_compilation_detail_download) TextView downloadButton;
    @BindView(R.id.button_compilation_detail_main_page) TextView mainPageButton;
    @BindView(R.id.compilation_detail_downloaded_buttons_container) View downloadedContainerView;
    @BindView(R.id.view_compilation_detail_download_control) CompilationDownloadControlView controls;

    private Unbinder unbinder;
    private Compilation compilation;
    private boolean downloadPending;
    private boolean downloadComplete;

    @NonNull
    public static CompilationDetailFragment newInstance(@NonNull Compilation info) {
        CompilationDetailFragment instance = new CompilationDetailFragment();
        Bundle args = new Bundle();
        args.putString(EXTRA_COMPILATION, GsonMarshaller.marshal(info));
        instance.setArguments(args);
        return instance;
    }

    @Nullable @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_compilation_detail, container, false);
        unbinder = ButterKnife.bind(this, view);

        getAppCompatActivity().setSupportActionBar(toolbar);
        getAppCompatActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getAppCompatActivity().getSupportActionBar().setTitle("");

        compilation = GsonUnmarshaller.unmarshal(Compilation.class,
                getActivity().getIntent().getStringExtra(EXTRA_COMPILATION));

        gradientView.setBackground(GradientUtil.getPowerGradient(R.color.black54, Gravity.TOP));
        imageView.loadImage(compilation.featureImageUri());
        nameView.setText(compilation.name());
        dateSizeView.setText(getString(R.string.offline_compilation_detail_date_size_v2,
                getShortDateString(compilation.date()), bytesToUserVisibleUnit(getContext(), compilation.size())));
        summaryView.setText(compilation.summary());
        descriptionView.setText(compilation.description());
        downloadButton.setText(getString(R.string.offline_compilation_detail_button_download_v2,
                bytesToUserVisibleUnit(getContext(), compilation.size())));

        controls.setCallback(() -> getDownloadObserver().remove(compilation));

        mainPageButton.setText(MainPageNameData.valueFor(WikipediaApp.getInstance().getAppLanguageCode()));
        updateDownloadState(true, null);
        return view;
    }

    @Override
    public void onDestroyView() {
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    @OnClick(R.id.button_compilation_detail_download) void onDownloadClick() {
        if (DeviceUtil.isOnline()) {
            if (!getDownloadObserver().isDownloading(compilation)) {
                MediaDownloadReceiver.download(getContext(), compilation);
                downloadPending = true;
                updateDownloadState(true, null);
            }
        } else {
            FeedbackUtil.showMessage(getActivity(), R.string.offline_compilation_download_device_offline);
        }
    }

    @OnClick(R.id.button_compilation_detail_main_page) void onMainPageClick() {
        try {
            PageTitle title = new PageTitle(OfflineManager.instance().getMainPageTitle(compilation),
                    WikipediaApp.getInstance().getWikiSite());

            HistoryEntry entry = new HistoryEntry(title, HistoryEntry.SOURCE_MAIN_PAGE);
            startActivity(PageActivity.newIntentForNewTab(getContext(), entry, entry.getTitle()));
        } catch (IOException e) {
            L.e(e);
            FeedbackUtil.showError(getActivity(), e);
        }
    }

    @OnClick(R.id.button_compilation_detail_remove) void onRemoveClick() {
        getDownloadObserver().removeWithConfirmation(getActivity(), compilation, (dialog, which) -> getAppCompatActivity().finish());
    }

    private AppCompatActivity getAppCompatActivity() {
        return (AppCompatActivity) getActivity();
    }

    private void updateDownloadState(boolean indeterminate, @Nullable DownloadManagerItem item) {
        if (indeterminate) {
            downloadComplete = false;
            downloadButton.setVisibility(View.VISIBLE);
            downloadButton.setEnabled(false);
            downloadedContainerView.setVisibility(View.GONE);
            controls.setVisibility(View.GONE);
            return;
        }
        if (item == null && !compilation.existsOnDisk()) {
            downloadComplete = false;
            downloadButton.setVisibility(View.VISIBLE);
            downloadButton.setEnabled(!downloadPending);
            downloadedContainerView.setVisibility(View.GONE);
            controls.setVisibility(View.GONE);
        } else if (CompilationDownloadControlView.shouldShowControls(item)) {
            downloadComplete = false;
            downloadButton.setVisibility(View.GONE);
            downloadedContainerView.setVisibility(View.GONE);
            controls.setVisibility(View.VISIBLE);
            controls.update(item);
        } else {
            if (!downloadComplete && item != null) {
                downloadComplete = true;
                OfflineManager.instance().ensureAdded(compilation, item.uri());
            }
            downloadButton.setVisibility(View.GONE);
            downloadedContainerView.setVisibility(View.VISIBLE);
            controls.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPollDownloads() {
        for (DownloadManagerItem item : getCurrentDownloads()) {
            if (item.is(compilation)) {
                downloadPending = false;
                updateDownloadState(false, item);
                return;
            }
        }
        updateDownloadState(false, null);
    }
}
