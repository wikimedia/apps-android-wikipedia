package org.wikipedia.settings;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.TextView;
import org.wikipedia.R;
import org.wikipedia.WikipediaApp;

public class AboutActivity extends ActionBarActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ((TextView) findViewById(R.id.about_version_text)).setText(WikipediaApp.APP_VERSION_STRING);
        ((TextView) findViewById(R.id.send_feedback_text)).setText(Html.fromHtml(
                "<a href=\"mailto:mobile-android-wikipedia@wikimedia.org?subject=Android App " +
                WikipediaApp.APP_VERSION_STRING +
                " Feedback\">" +
                getString(R.string.send_feedback) +
                "</a>"));

        makeEverythingClickable((ViewGroup) findViewById(R.id.about_container));
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

    private void makeEverythingClickable(ViewGroup vg) {
        for (int i = 0; i < vg.getChildCount(); i++) {
            if (vg.getChildAt(i) instanceof ViewGroup) {
                makeEverythingClickable((ViewGroup)vg.getChildAt(i));
            } else if (vg.getChildAt(i) instanceof TextView) {
                TextView tv = (TextView) vg.getChildAt(i);
                tv.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }

    }
}
