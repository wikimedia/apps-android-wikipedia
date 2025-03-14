package org.wikipedia.yearinreview

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme

@Composable
fun YearInReviewScaffold(
    yearInReviewTopBar: @Composable () -> Unit,
    yearInReviewBottomBar: @Composable () -> Unit,
    yearInReviewContent: @Composable () -> Unit
) {
    BaseTheme(content = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(bottom = 50.dp, top = 50.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ){
            yearInReviewTopBar()
            yearInReviewContent()
            yearInReviewBottomBar()
        }
        }
    )
}

@Composable
fun YearInReviewTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ){
        IconButton(onClick = { TODO() }) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back_black_24dp),
                contentDescription = "navigate back icon"
            )
        }

        Icon(
            painter = painterResource(R.drawable.ic_wikipedia_b),
            contentDescription = "wiki icon"
        )

        IconButton(onClick = { TODO() }) {
            Icon(
                painter = painterResource(R.drawable.ic_share),
                contentDescription = "share icon"
            )
        }
    }
}

@Composable
fun YearInReviewContent(){
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center){
        Text(text = "Hello World")
    }
}

@Composable
fun YearInReviewMainBottomBar(){
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(Color.LightGray)
                .align(Alignment.Start)
        ){ }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .align(Alignment.CenterHorizontally),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ){
            Row(modifier = Modifier
                .clickable(onClick = { TODO() })
                .padding(start = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {

                Icon(
                    painter = painterResource(R.drawable.ic_hear_red_24),
                    contentDescription = "Heart Icon",
                    tint = Color.Unspecified
                )

                Text(text = "Donate",
                    fontSize = 16.sp,
                    color = Color(0xFFB32424)
                )
            }

            IconButton(onClick = { TODO() }) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_forward_black_24dp),
                    contentDescription = "navigate forward"
                )
            }
        }
    }
}

@Composable
fun LearnMoreGetStartedBar(){
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .padding(start = 10.dp, end = 10.dp)
    ){
        OutlinedButton(
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF3366CC)),
            modifier = Modifier
                .width(152.dp)
                .height(42.dp),
            onClick = { TODO() }
        ){
            Text("Learn More")
        }

        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3366CC),
                contentColor = Color.White
            ),
                modifier = Modifier
                    .width(152.dp)
                    .height(42.dp),
                onClick = {TODO ()}
        ){
            Text("Get Started")
        }
    }
}

@Preview
@Composable
fun viewCustomScaffold_bottom1(){

    YearInReviewScaffold(
        yearInReviewTopBar = { YearInReviewTopBar() },
        yearInReviewBottomBar = { YearInReviewMainBottomBar() },
        yearInReviewContent = { YearInReviewContent() }
    )
}

@Preview
@Composable
fun viewCustomScaffold_bottom2(){

    YearInReviewScaffold(
        yearInReviewTopBar = { YearInReviewTopBar() },
        yearInReviewBottomBar = { LearnMoreGetStartedBar()},
        yearInReviewContent = { YearInReviewContent() }
    )
}








/* this works to display an image
Image(
                modifier = Modifier
                    .fillMaxWidth(),
                contentScale = ContentScale.Crop,
                painter = painterResource(R.drawable.year_in_review_puzzle_pieces),
                contentDescription = "null"
            )
 */

/* this also works
*
* */
















































































































































































