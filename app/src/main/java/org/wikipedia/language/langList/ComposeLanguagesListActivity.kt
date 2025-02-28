package org.wikipedia.language.langList

import android.os.Bundle
import androidx.activity.compose.setContent
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.theme.BaseTheme

class ComposeLanguagesListActivity: BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LanguagesListParentScreen()
        }
    }


}