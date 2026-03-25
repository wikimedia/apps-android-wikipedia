package org.wikipedia.feed

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import androidx.fragment.app.Fragment
import org.wikipedia.R
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.theme.Theme
import org.wikipedia.views.imageservice.ImageService

class HomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireActivity()).apply {
            setContent {
                BaseTheme {
                    HomeFragmentContents()
                }
            }
        }
    }
}

private val prototypeImageUrls = listOf(
    "https://upload.wikimedia.org/wikipedia/commons/thumb/2/25/SW_Hullathy_Gram_Panchayat_Villages_Nilgiris_Nov24_A7CR_05293.jpg/1280px-SW_Hullathy_Gram_Panchayat_Villages_Nilgiris_Nov24_A7CR_05293.jpg",
    "https://upload.wikimedia.org/wikipedia/commons/thumb/1/10/Color_of_Friendship.jpg/1280px-Color_of_Friendship.jpg",
    "https://upload.wikimedia.org/wikipedia/commons/thumb/d/dd/MAP_Expo_Empereur_Ojin_Poup%C3%A9e_03_01_2012.jpg/1280px-MAP_Expo_Empereur_Ojin_Poup%C3%A9e_03_01_2012.jpg",
    "https://upload.wikimedia.org/wikipedia/commons/thumb/a/a7/Sachsenheim_-_Ochsenbach_-_Geigersberg_-_n%C3%B6rdlicher_Teil_von_SSO_im_M%C3%A4rz.jpg/1280px-Sachsenheim_-_Ochsenbach_-_Geigersberg_-_n%C3%B6rdlicher_Teil_von_SSO_im_M%C3%A4rz.jpg",
    "https://upload.wikimedia.org/wikipedia/commons/thumb/0/02/Templo_de_Rams%C3%A9s_II%2C_Abu_Simbel%2C_Egipto%2C_2022-04-02%2C_DD_26-28_HDR.jpg/1280px-Templo_de_Rams%C3%A9s_II%2C_Abu_Simbel%2C_Egipto%2C_2022-04-02%2C_DD_26-28_HDR.jpg",
)

@Composable
fun HomeFragmentContents() {
    val context = LocalContext.current
    val screenHeight = LocalConfiguration.current.screenHeightDp
    val listState = rememberLazyListState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            modifier = Modifier
                .fillMaxSize()
                .background(WikipediaTheme.colors.backgroundColor)
        ) {
            itemsIndexed(prototypeImageUrls) { _, imageUrl ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenHeight.dp)
                ) {
                    AsyncImage(
                        model = ImageService.getRequest(context, url = imageUrl),
                        placeholder = ColorPainter(WikipediaTheme.colors.backgroundColor),
                        error = ColorPainter(WikipediaTheme.colors.backgroundColor),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.72f),
                            Color.Black.copy(alpha = 0.32f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Image(
                painter = painterResource(R.drawable.feed_header_wordmark),
                contentDescription = null,
                colorFilter = ColorFilter.tint(Color.White),
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(start = 20.dp, top = 64.dp)
                    .width(128.dp)
                    .align(Alignment.TopStart)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeFragmentPreview() {
    BaseTheme(currentTheme = Theme.LIGHT) {
        HomeFragmentContents()
    }
}
