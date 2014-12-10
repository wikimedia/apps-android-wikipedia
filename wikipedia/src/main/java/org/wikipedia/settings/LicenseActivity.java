package org.wikipedia.settings;

import android.os.Bundle;
import android.text.Html;
import android.view.MenuItem;
import android.widget.TextView;

import org.wikipedia.R;
import org.wikipedia.ThemedActionBarActivity;
import org.wikipedia.Utils;

import java.io.IOException;

/**
 * Displays license text of the libraries we use.
 */
public class LicenseActivity extends ThemedActionBarActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license);

        final String path = getIntent().getData().getPath();
        // Example string: "/android_asset/licenses/ACRA"

        final int libraryNameStart = 24;
        setTitle(getString(R.string.license_title, path.substring(libraryNameStart)));

        try {
            TextView textView = (TextView) findViewById(R.id.license_text);
            final int assetPathStart = 15;
            final String text = Utils.readFile(getAssets().open(path.substring(assetPathStart)));
            textView.setText(Html.fromHtml(text.replace("\n\n", "<br/><br/>")));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                throw new RuntimeException("Unclickable things have been clicked. The apocalypse is nearby");
        }
        return super.onOptionsItemSelected(item);
    }
}
