package org.wikipedia.settings;

import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import org.wikipedia.R;
import org.wikipedia.ThemedActionBarActivity;
import org.wikipedia.Utils;
import org.wikipedia.WikipediaApp;

public class AboutActivity extends ThemedActionBarActivity {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ((TextView) findViewById(R.id.about_translators)).setText(Html.fromHtml(getString(R.string.about_translators_translatewiki)));
        ((TextView) findViewById(R.id.about_terms_of_use)).setText(Html.fromHtml(getString(R.string.about_terms_of_use)));
        ((TextView) findViewById(R.id.about_privacy_policy)).setText(Html.fromHtml(getString(R.string.about_privacy_policy)));
        ((TextView) findViewById(R.id.about_wmf)).setText(Html.fromHtml(getString(R.string.about_wmf)));
        ((TextView) findViewById(R.id.about_version_text)).setText(WikipediaApp.APP_VERSION_STRING);
        ((TextView) findViewById(R.id.send_feedback_text)).setText(Html.fromHtml(
                "<a href=\"mailto:mobile-android-wikipedia@wikimedia.org?subject=Android App " +
                WikipediaApp.APP_VERSION_STRING +
                " Feedback\">" +
                getString(R.string.send_feedback) +
                "</a>"));

        //if there's no Email app, hide the Feedback link.
        if (!Utils.mailAppExists(this)) {
            findViewById(R.id.send_feedback_text).setVisibility(View.GONE);
        }

        WikipediaApp.getInstance().adjustDrawableToTheme(((ImageView) findViewById(R.id.about_logo_image)).getDrawable());

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
