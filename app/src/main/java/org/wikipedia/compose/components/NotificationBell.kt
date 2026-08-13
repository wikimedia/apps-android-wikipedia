package org.wikipedia.compose.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.theme.Theme
import kotlin.math.min

@Composable
fun NotificationBell(
    unreadCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconRes: Int = R.drawable.ic_notifications_black_24dp
) {
    val scale = remember { Animatable(1f) }
    var previousCount by remember { mutableIntStateOf(unreadCount) }

    LaunchedEffect(unreadCount) {
        if (unreadCount > previousCount) {
            scale.snapTo(1f)
            scale.animateTo(2f, tween(250))
            scale.animateTo(1f, tween(250))
        }
        previousCount = unreadCount
    }

    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(
                onClick = onClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false)
            )
            .graphicsLayer(
                scaleX = scale.value,
                scaleY = scale.value
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = WikipediaTheme.colors.primaryColor,
            modifier = Modifier.size(24.dp)
        )
        AnimatedVisibility(
            visible = unreadCount > 0,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-6).dp, y = 6.dp)
        ) {
            CircleWithNumber(number = unreadCount)
        }
    }
}

@Composable
fun CircleWithNumber(
    number: Int,
    modifier: Modifier = Modifier,
    backgroundColor: Color = WikipediaTheme.colors.destructiveColor,
) {
    val displayNumber = min(number, 99).toString()
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayNumber,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White
        )
    }
}

data class NotificationBellState(
    val unreadCount: Int = 0,
    val canShow: Boolean = false
)

@Preview(showBackground = true)
@Composable
private fun NotificationBellPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        NotificationBell(
            unreadCount = 45,
            onClick = {}
        )
    }
}
