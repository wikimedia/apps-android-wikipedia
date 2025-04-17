package org.wikipedia.yearinreview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import org.wikipedia.util.Resource

@Composable
fun TestScreen(
    viewModel: YearInReviewViewModel,
    screenDataObj: YearInReviewScreenData = readCountData
) {
    val jobMapState = viewModel.masterMap.collectAsState()
    val derivedMapState = remember { derivedStateOf { jobMapState.value[PersonalizedJobID.READ_COUNT.name] } }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center
    ) {

        when (derivedMapState.value) {
            is Resource.Loading -> {
                Text(
                    text = "LOADING",
                    fontSize = 20.sp,
                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
                )
            }

            is Resource.Success -> {
                val fetchedHeadLine =
                    (derivedMapState.value as Resource.Success<YearInReviewTextData>).data.headLineText
                val fetchedBodyText =
                    (derivedMapState.value as Resource.Success<YearInReviewTextData>).data.bodyText
                Text(
                    text = String.format(
                        stringResource(screenDataObj.headLineText),
                        fetchedHeadLine
                    ),
                    fontSize = 20.sp,
                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
                )

                Text(
                    text = String.format(stringResource(screenDataObj.bodyText), fetchedBodyText),
                    fontSize = 20.sp,
                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
                )
            }

            else -> {
                Text(
                    text = "Error",
                    fontSize = 30.sp,
                    modifier = Modifier.align(alignment = Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Preview
@Composable
fun PreviewTestScreen() {
    // TestScreen()
}
