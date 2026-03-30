package org.wikipedia.onboarding.personalization

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import org.wikipedia.Constants
import org.wikipedia.activity.BaseActivity
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.page.PageTitle
import org.wikipedia.search.SearchActivity

class PersonalizationActivity : BaseActivity() {

    private val viewModel: PersonalizationViewModel by viewModels()

    private val searchLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == SearchActivity.RESULT_LINK_SUCCESS) {
            val pageTitle = it.data?.parcelableExtra<PageTitle>(SearchActivity.EXTRA_RETURN_LINK_TITLE)!!
            viewModel.addArticle(pageTitle)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BaseTheme {
                PersonalizationScreen(
                    viewModel = viewModel,
                    onSkipClick = { finish() },
                    onSearchClick = {
                        val intent = SearchActivity.newIntent(this, Constants.InvokeSource.INTEREST_SELECTION, null, returnLink = true)
                        searchLauncher.launch(intent)
                    }
                )
            }
        }
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, PersonalizationActivity::class.java)
        }
    }
}
