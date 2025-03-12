package org.wikipedia.yearinreview

import android.widget.ImageView
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bumptech.glide.Glide
import org.wikipedia.R
import org.wikipedia.compose.theme.LightColors
import org.wikipedia.compose.theme.WikipediaTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YearInReviewScreenScaffold(
    customBottomBar: @Composable () -> Unit,
    screenContent: @Composable (PaddingValues, ScrollState) -> Unit
) {

    val scrollState = rememberScrollState()

    Scaffold(
        containerColor = WikipediaTheme.colors.paperColor,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = WikipediaTheme.colors.paperColor),
                title = {
                    Icon(
                        painter = painterResource(R.drawable.ic_wikipedia_b),
                        contentDescription = stringResource(R.string.year_in_review_topbar_w_icon)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { TODO() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back_black_24dp),
                            contentDescription = stringResource(R.string.year_in_review_navigate_left)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { TODO() }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_share),
                            contentDescription = stringResource(R.string.year_in_review_share_icon)
                        )
                    }
                }
            )
        },

        bottomBar = customBottomBar,
    )  { innerPadding ->

            screenContent(innerPadding, scrollState)
    }
}

@Composable
fun MainBottomBar(){

    BottomAppBar(
        modifier = Modifier.border(
            width = 1.dp,
            shape = RectangleShape,
            color = WikipediaTheme.colors.borderColor
        ),
        containerColor = Color.Transparent,
        content = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ){
                Row(modifier = Modifier
                    .clickable(onClick = { TODO() })
                    .padding(start = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ){

                    Icon(
                        painter = painterResource(R.drawable.ic_hear_red_24),
                        contentDescription = stringResource(R.string.year_in_review_heart_icon),
                        tint = Color.Unspecified
                    )

                    Text(text = stringResource(R.string.year_in_review_donate),
                        style = WikipediaTheme.typography.h3,
                        color = WikipediaTheme.colors.destructiveColor
                    )
                }

                IconButton(onClick = { TODO() }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_forward_black_24dp),
                        contentDescription = stringResource(R.string.year_in_review_navigate_right)
                    )
                }
            }
        }
    )
}

@Composable
fun GetStartedBottomBar(){
    BottomAppBar(
        containerColor = LightColors.paperColor,
        content = {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .padding(start = 10.dp, end = 10.dp)
            ){
                OutlinedButton(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WikipediaTheme.colors.paperColor,
                        contentColor = WikipediaTheme.colors.progressiveColor),
                    modifier = Modifier
                        .width(152.dp)
                        .height(42.dp),
                    onClick = { TODO() }
                ){
                    Text(
                        text = stringResource(R.string.year_in_review_learn_more),
                        style = WikipediaTheme.typography.button
                    )
                }

                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WikipediaTheme.colors.progressiveColor,
                        contentColor = WikipediaTheme.colors.paperColor
                    ),
                    modifier = Modifier
                        .width(152.dp)
                        .height(42.dp),
                    onClick = {TODO ()}
                ){
                    Text(
                        text = stringResource(R.string.year_in_review_get_started),
                        style = WikipediaTheme.typography.button
                    )
                }
            }
        }
    )
}

@Composable
fun YearInReviewScreenContent(
    innerPadding: PaddingValues,
    scrollState: ScrollState
){
    val context = LocalContext.current

    Column(
        verticalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .padding(innerPadding)
            .verticalScroll(scrollState)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
        ){
            AndroidView(
                factory = {
                    ImageView(context).apply{
                        Glide.with(context)
                            .asGif()
                            .load(R.drawable.year_in_review_block_10_resize)
                            .centerCrop()
                            .into(this)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .aspectRatio(
                        ratio = 3f / 2f,
                        matchHeightConstraintsFirst = true)
            )
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
        ){

            Row(horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
            ){
                Text(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .height(IntrinsicSize.Min)
                        .weight(1f),
                    text = "You edited Wikipedia 150 times",
                    fontSize = 30.sp
                )

                IconButton(
                    onClick = {TODO ()}) {
                    Icon(
                        painter = painterResource(R.drawable.ic_info_outline_black_24dp),
                        contentDescription = stringResource(R.string.year_in_review_information_icon)
                    )
                }
            }

            Text(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .height(IntrinsicSize.Min),

                fontSize = 20.sp,
                text = "This year, Wikipedia was edited at an average rate of 342 times per minute. Articles are collaboratively created and improved using reliable sources. All of us have knowledge to share, learn how to participate."
            )
        }
    }
}

@Preview
@Composable
fun ViewScaffold1(){
    YearInReviewScreenScaffold(
        customBottomBar = { MainBottomBar() },
        screenContent = { innerPadding, scrollState ->
            YearInReviewScreenContent(
                innerPadding = innerPadding,
                scrollState = scrollState)
        }
    )
}

@Preview
@Composable
fun ViewScaffold2(){
    YearInReviewScreenScaffold(
        customBottomBar = { GetStartedBottomBar() },
        screenContent = { innerPadding, scrollState ->
            YearInReviewScreenContent(
                innerPadding = innerPadding,
                scrollState = scrollState)
        }
    )
}