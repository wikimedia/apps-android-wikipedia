package org.wikipedia.edits

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_suggested_edits_tags_onboarding.*
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity

class EditsImageTagsOnboardingActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_suggested_edits_tags_onboarding)
        onboarding_done_button.setOnClickListener {
            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, EditsImageTagsOnboardingActivity::class.java)
        }
    }
}
