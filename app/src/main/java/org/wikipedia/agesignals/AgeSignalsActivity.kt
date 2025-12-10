package org.wikipedia.agesignals

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.android.play.agesignals.AgeSignalsManagerFactory
import com.google.android.play.agesignals.AgeSignalsRequest
import com.google.android.play.agesignals.AgeSignalsResult
import com.google.android.play.agesignals.model.AgeSignalsVerificationStatus
import com.google.android.play.agesignals.testing.FakeAgeSignalsManager
import kotlinx.coroutines.launch
import org.wikipedia.WikipediaApp
import org.wikipedia.compose.components.WikiTopAppBar
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale

class AgeSignalsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var resultText by remember { mutableStateOf("No result yet") }

            AgeSignalsScreen(
                resultText = resultText,
                onNavigationClick = {
                    finish()
                },
                onGooglePlayAgeSignalClick = {
                    resultText = "Current date: ${LocalDate.now()} Testing from: ${Locale.getDefault().country}\n"
                    val ageSignalsManager = AgeSignalsManagerFactory.create(WikipediaApp.instance)
                    ageSignalsManager
                        .checkAgeSignals(AgeSignalsRequest.builder().build())
                        .addOnSuccessListener { ageSignalsResult ->
                            resultText += "Google Play Age Signals Live API Success:\n$ageSignalsResult\n"
                        }
                        .addOnFailureListener { error ->
                            resultText += "Live Age Signals API Called"
                            resultText += "Error:\n$error\n"
                        }

                    val fakeVerifiedUser =
                        AgeSignalsResult.builder()
                            .setUserStatus(AgeSignalsVerificationStatus.VERIFIED)
                            .build()
                    val manager = FakeAgeSignalsManager()
                    manager.setNextAgeSignalsResult(fakeVerifiedUser)
                    manager.checkAgeSignals(AgeSignalsRequest.builder().build())
                        .addOnSuccessListener { ageSignalsResult ->
                            resultText += "VERIFIED user response :\n$ageSignalsResult\n\n"
                        }

                    val fakeSupervisedUser =
                        AgeSignalsResult.builder()
                            .setUserStatus(AgeSignalsVerificationStatus.SUPERVISED)
                            .setAgeLower(13)
                            .setAgeUpper(17)
                            .setInstallId("fake_install_id")
                            .build()
                    manager.setNextAgeSignalsResult(fakeSupervisedUser)
                    manager.checkAgeSignals(AgeSignalsRequest.builder().build())
                        .addOnSuccessListener { ageSignalsResult ->
                            resultText += "SUPERVISED user age 13 - 17 response :\n$ageSignalsResult\n\n"
                        }

                    val fakeSupervisedApprovalPendingUser =
                        AgeSignalsResult.builder()
                            .setUserStatus(AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_PENDING)
                            .setAgeLower(13)
                            .setAgeUpper(17)
                            .setInstallId("fake_install_id")
                            .build()
                    manager.setNextAgeSignalsResult(fakeSupervisedApprovalPendingUser)
                    manager.checkAgeSignals(AgeSignalsRequest.builder().build())
                        .addOnSuccessListener { ageSignalsResult ->
                            resultText += "SUPERVISED_APPROVAL_PENDING user age 13 - 17 response :\n$ageSignalsResult\n\n"
                        }
                    val fakeSupervisedApprovalDeniedUser =
                        AgeSignalsResult.builder()
                            .setUserStatus(AgeSignalsVerificationStatus.SUPERVISED_APPROVAL_DENIED)
                            .setAgeLower(13)
                            .setAgeUpper(17)
                            .setMostRecentApprovalDate(
                                Date.from(LocalDate.of(2025, 2, 1).atStartOfDay(ZoneOffset.UTC).toInstant())
                            )
                            .setInstallId("fake_install_id")
                            .build()
                    manager.setNextAgeSignalsResult(fakeSupervisedApprovalDeniedUser)
                    manager.checkAgeSignals(AgeSignalsRequest.builder().build())
                        .addOnSuccessListener { ageSignalsResult ->
                            resultText += "SUPERVISED_APPROVAL_DENIED user age 13 - 17 response :\n$ageSignalsResult\n\n"
                        }
                    val fakeUnknownUser =
                        AgeSignalsResult.builder().setUserStatus(AgeSignalsVerificationStatus.UNKNOWN).build()
                    manager.setNextAgeSignalsResult(fakeUnknownUser)
                    manager.checkAgeSignals(AgeSignalsRequest.builder().build())
                        .addOnSuccessListener { ageSignalsResult ->
                            resultText += "UNKNOWN user response :\n$ageSignalsResult\n\n"
                        }
                },
                onAmazonAgeSignalClick = {
                    lifecycleScope.launch {
                        val client = AmazonUserDataClient(this@AgeSignalsActivity)
                        resultText = "Amazon Age Signals\n\n"
                        for (testOption in 1..6) {
                            val userData = client.getUserData(testOption)
                            if (userData != null) {
                                resultText += "$userData\n\n"
                            } else {
                                resultText = "Error: No data returned\n\n"
                            }
                        }
                    }
                },
                onSamsungAgeSignalClick = {
                    lifecycleScope.launch {
                        val client = SamsungAgeSignalsClient(this@AgeSignalsActivity)
                        val result = client.getAgeSignals()
                        resultText = "Samsung Age Signals\n\n"
                        resultText += when (result) {
                            is SamsungAgeSignalResult.Success -> {
                                "${result.data}\n\n"
                            }

                            is SamsungAgeSignalResult.Failure -> {
                                "Failure: $result\n\n"
                            }

                            SamsungAgeSignalResult.ProviderNotAvailable -> {
                                "Galaxy Store not available or outdated\n\n"
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun AgeSignalsScreen(
    resultText: String,
    modifier: Modifier = Modifier,
    onNavigationClick: () -> Unit,
    onGooglePlayAgeSignalClick: () -> Unit,
    onAmazonAgeSignalClick: () -> Unit,
    onSamsungAgeSignalClick: () -> Unit
) {
    Scaffold(
        topBar = {
            WikiTopAppBar(
                title = "Age Signals",
                onNavigationClick = onNavigationClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Select an Age Signal Provider:",
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = onGooglePlayAgeSignalClick,
                    label = { Text("Google Play") }
                )

                AssistChip(onAmazonAgeSignalClick, { Text("Amazon") }
                )

                AssistChip(
                    onClick = onSamsungAgeSignalClick,
                    label = { Text("Samsung") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Result:",
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = resultText,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            )
        }
    }
}
