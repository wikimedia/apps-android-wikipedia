package org.wikipedia.yearinreview

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.wikipedia.compose.theme.BaseTheme

class YearInReviewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            val navigationController = rememberNavController()

            NavHost(
                navController = navigationController, startDestination = "testscreen"
            ) {
                composable("testscreen") {
                    BaseTheme(content = { TestScreen() })
                }
            }
        }
    }

    companion object {

        const val INTENT_EXTRA_USER_NAME = "userName"

        fun newIntent(context: Context, userName: String): Intent {

            return Intent(context, YearInReviewActivity::class.java)
                .putExtra(INTENT_EXTRA_USER_NAME, userName)
        }
    }
}
