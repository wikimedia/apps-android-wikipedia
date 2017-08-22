package org.wikipedia.offline;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.WindowManager;

import org.wikipedia.activity.SingleFragmentActivity;
import org.wikipedia.json.GsonMarshaller;
import org.wikipedia.json.GsonUnmarshaller;

public class CompilationDetailActivity extends SingleFragmentActivity<CompilationDetailFragment> {
    protected static final String EXTRA_COMPILATION = "compilation";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }

    public static Intent newIntent(@NonNull Context context, @NonNull Compilation comp) {
        return new Intent(context, CompilationDetailActivity.class)
                .putExtra(EXTRA_COMPILATION, GsonMarshaller.marshal(comp));
    }

    @Override
    protected CompilationDetailFragment createFragment() {
        return CompilationDetailFragment.newInstance(GsonUnmarshaller.unmarshal(Compilation.class,
                getIntent().getStringExtra(EXTRA_COMPILATION)));
    }
}
