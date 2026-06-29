package org.wikipedia.yir

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.wikipedia.R

@Composable
fun YirTopBar(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    onDonate: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(
                painter = painterResource(R.drawable.ic_close_black_24dp),
                contentDescription = stringResource(R.string.table_close),
                tint = Color.White
            )
        }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            actions()
            onDonate?.let {
                TextButton(onClick = it) {
                    Icon(
                        painter = painterResource(R.drawable.ic_heart_24),
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(
                        text = stringResource(R.string.nav_item_donate),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}
