package org.wikipedia.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.wikipedia.R
import org.wikipedia.compose.components.SettingsSection
import org.wikipedia.compose.theme.WikipediaTheme

@Composable
internal fun SettingsAccountSection(
    account: AccountState,
    onLogoutClick: () -> Unit,
    onVanishClick: () -> Unit
) {
    SettingsSection(title = stringResource(R.string.preferences_account_heading)) {
        HorizontalDivider(color = WikipediaTheme.colors.borderColor)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(24.dp),
                painter = painterResource(if (account.isTemporary) R.drawable.ic_temp_account else R.drawable.ic_baseline_person_24),
                contentDescription = null,
                tint = WikipediaTheme.colors.placeholderColor
            )
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp),
                text = account.userName,
                style = MaterialTheme.typography.bodyLarge,
                color = WikipediaTheme.colors.primaryColor
            )
            TextButton(
                modifier = Modifier.padding(start = 8.dp),
                onClick = onLogoutClick
            ) {
                Text(
                    text = stringResource(if (account.isTemporary) R.string.temp_account_end_session else R.string.preference_title_logout),
                    color = WikipediaTheme.colors.destructiveColor
                )
            }
        }
        if (account.isTemporary) {
            Text(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                text = pluralStringResource(R.plurals.temp_account_expiry, account.tempAccountDaysLeft, account.tempAccountDaysLeft),
                style = MaterialTheme.typography.bodyMedium,
                color = WikipediaTheme.colors.secondaryColor
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onVanishClick)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_person_remove),
                    contentDescription = null,
                    tint = WikipediaTheme.colors.destructiveColor
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.account_vanish_request_title),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = WikipediaTheme.colors.destructiveColor
                )
            }
        }
        HorizontalDivider(color = WikipediaTheme.colors.borderColor)
    }
}
