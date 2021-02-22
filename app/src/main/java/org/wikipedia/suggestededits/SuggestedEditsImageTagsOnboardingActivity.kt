package org.wikipedia.suggestededits

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity

class SuggestedEditsImageTagsOnboardingActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_suggested_edits_tags_onboarding)
        findViewById<View>(R.id.onboarding_done_button).setOnClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, SuggestedEditsImageTagsOnboardingActivity::class.java)
        }
    }
}
