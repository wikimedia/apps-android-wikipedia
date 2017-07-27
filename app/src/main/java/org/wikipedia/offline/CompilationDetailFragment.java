package org.wikipedia.offline;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;
import org.wikipedia.util.DimenUtil;
import org.wikipedia.util.GradientUtil;
import org.wikipedia.views.FaceAndColorDetectImageView;
import org.wikipedia.views.ViewUtil;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

import static org.wikipedia.offline.CompilationDetailActivity.EXTRA_COMPILATION;
import static org.wikipedia.util.DateUtil.getShortDateString;
import static org.wikipedia.util.DimenUtil.leadImageHeightForDevice;
import static org.wikipedia.util.FileUtil.bytesToGB;

public class CompilationDetailFragment extends Fragment {
    @BindView(R.id.compilation_detail_toolbar) Toolbar toolbar;
    @BindView(R.id.compilation_detail_header_container) View containerView;
    @BindView(R.id.compilation_detail_header_image) FaceAndColorDetectImageView imageView;
    @BindView(R.id.compilation_detail_header_gradient) View gradientView;
    @BindView(R.id.view_compilation_info_title) TextView nameView;
    @BindView(R.id.view_compilation_info_date_size) TextView dateSizeView;
    @BindView(R.id.view_compilation_info_summary) TextView summaryView;
    @BindView(R.id.view_compilation_info_description) TextView descriptionView;
    @BindView(R.id.button_compilation_detail_download) TextView downloadButton;
    @BindView(R.id.view_compilation_detail_download_control) CompilationDownloadControlView controls;

    private Unbinder unbinder;

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

        ViewUtil.setTopPaddingDp(toolbar, (int) DimenUtil.getTranslucentStatusBarHeight(getContext()));
        getAppCompatActivity().setSupportActionBar(toolbar);
        getAppCompatActivity().getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getAppCompatActivity().getSupportActionBar().setTitle("");

        Compilation comp = GsonUnmarshaller.unmarshal(Compilation.class,
                getActivity().getIntent().getStringExtra(EXTRA_COMPILATION));

        Uri imageUri = comp.imageUri();
        int height = imageUri == null ? DimenUtil.getContentTopOffsetPx(getContext()) : leadImageHeightForDevice();
        if (imageUri == null) {
            toolbar.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.main_toolbar_background));
        }
        DimenUtil.setViewHeight(containerView, height);
        ViewUtil.setBackgroundDrawable(gradientView, GradientUtil.getCubicGradient(Color.BLACK, Gravity.TOP));

        imageView.loadImage(imageUri);
        nameView.setText(comp.name());
        dateSizeView.setText(String.format(getString(R.string.offline_compilation_detail_date_size),
                getShortDateString(comp.timestamp()), bytesToGB(comp.size())));
        summaryView.setText(comp.summary());
        descriptionView.setText(comp.description());
        downloadButton.setText(String.format(getString(R.string.offline_compilation_detail_button_download),
                bytesToGB(comp.size())));
        controls.setCompilation(comp);

        return view;
    }

    @Override public void onDestroyView() {
        unbinder.unbind();
        unbinder = null;
        super.onDestroyView();
    }

    private AppCompatActivity getAppCompatActivity() {
        return (AppCompatActivity) getActivity();
    }
}
