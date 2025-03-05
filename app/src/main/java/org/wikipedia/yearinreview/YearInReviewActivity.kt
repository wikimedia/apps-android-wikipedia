package org.wikipedia.yearinreview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

class YearInReviewActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

            val navigationController = rememberNavController()

            NavHost(
                navController = navigationController, startDestination = "testscreen"
            ) {
                composable("testscreen"){
                    TestScreen()
                }
            }
        }
    }
}
