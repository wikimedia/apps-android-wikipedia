package org.wikipedia.language.langList

import android.os.Bundle
import androidx.activity.compose.setContent
import org.wikipedia.activity.BaseActivity

class ComposeLanguagesListActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LanguagesListParentScreen(
                onBackButtonClick = {
                    finish()
                }
            )
        }
    }
}
