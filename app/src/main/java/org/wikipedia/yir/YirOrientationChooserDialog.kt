package org.wikipedia.yir

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.page.ExtendedBottomSheetDialogFragment

class YirOrientationChooserDialog : ExtendedBottomSheetDialogFragment(startExpanded = true) {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {
                    Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                        Text(
                            text = "Open YiR story spike",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = WikipediaTheme.colors.primaryColor
                        )
                        Text(
                            text = "Choose how the cards advance.",
                            fontSize = 14.sp,
                            color = WikipediaTheme.colors.secondaryColor,
                            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                        )

                        OrientationOption(
                            title = "Vertical",
                            subtitle = "Swipe up / down — like the For You feed",
                            onClick = { launch(YirPagerOrientation.VERTICAL) }
                        )
                        Spacer(Modifier.height(12.dp))
                        OrientationOption(
                            title = "Horizontal",
                            subtitle = "Swipe left / right — like the current carousel",
                            onClick = { launch(YirPagerOrientation.HORIZONTAL) }
                        )
                    }
                }
            }
        }
    }

    private fun launch(orientation: YirPagerOrientation) {
        startActivity(YirActivity.newIntent(requireContext(), orientation))
        dismiss()
    }
}

@Composable
private fun OrientationOption(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, WikipediaTheme.colors.borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = WikipediaTheme.colors.primaryColor
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = WikipediaTheme.colors.secondaryColor,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_arrow_forward_black_24dp),
            contentDescription = null,
            tint = WikipediaTheme.colors.primaryColor
        )
    }
}
