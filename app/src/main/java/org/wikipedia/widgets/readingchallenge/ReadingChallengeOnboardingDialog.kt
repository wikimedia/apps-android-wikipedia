package org.wikipedia.widgets.readingchallenge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.google.android.material.bottomsheet.BottomSheetBehavior
import org.wikipedia.R
import org.wikipedia.auth.AccountUtil
import org.wikipedia.compose.components.OnboardingItem
import org.wikipedia.compose.components.OnboardingListItem
import org.wikipedia.compose.components.TwoButtonBottomBar
import org.wikipedia.compose.components.WikipediaAlertDialog
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.login.LoginActivity
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.settings.Prefs
import org.wikipedia.theme.Theme
import org.wikipedia.util.UriUtil

class ReadingChallengeOnboardingDialog : ExtendedBottomSheetDialogFragment() {

    private val loginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == LoginActivity.RESULT_LOGIN_SUCCESS) {
            Prefs.readingChallengeEnrolled = true
            dismiss()
        }
    }

    private val onboardingItems = listOf(
        OnboardingItem(
            icon = R.drawable.ic_contract_24dp,
            title = R.string.reading_challenge_onboarding_read_title,
            subTitle = R.string.reading_challenge_onboarding_read_description
        ),
        OnboardingItem(
            icon = R.drawable.ic_featured_seasonal_and_gifts_24dp,
            title = R.string.reading_challenge_onboarding_win_title,
            subTitle = R.string.reading_challenge_onboarding_win_description
        ),
        OnboardingItem(
            icon = R.drawable.dashboard_customize_24dp,
            title = R.string.reading_challenge_onboarding_install_title,
            subTitle = R.string.reading_challenge_onboarding_install_description
        )
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme {

                    var showLoginDialog by remember { mutableStateOf(false) }
                    if (showLoginDialog) {
                        WikipediaAlertDialog(
                            title = stringResource(R.string.reading_challenge_onboarding_prompt_title),
                            message = stringResource(R.string.reading_challenge_onboarding_prompt_message),
                            confirmButtonText = stringResource(R.string.reading_challenge_onboarding_prompt_login),
                            dismissButtonText = stringResource(R.string.reading_challenge_onboarding_prompt_no_thanks),
                            dismissButtonColor = WikipediaTheme.colors.secondaryColor,
                            onDismissRequest = {
                                showLoginDialog = false
                            },
                            onConfirmButtonClick = {
                                loginLauncher.launch(LoginActivity.newIntent(requireActivity(), LoginActivity.SOURCE_READING_CHALLENGE))
                            },
                            onDismissButtonClick = {
                                dismiss()
                            }
                        )
                    }

                    OnboardingScreen(
                        modifier = Modifier.fillMaxSize(),
                        onboardingItems = onboardingItems,
                        onCloseClick = {
                            dismiss()
                        },
                        onLearnMoreClick = {
                            UriUtil.visitInExternalBrowser(
                                context = requireContext(),
                                uri = getString(R.string.reading_challenge_learn_more).toUri()
                            )
                        },
                        onJoinClick = {
                            if (!AccountUtil.isLoggedIn) {
                                showLoginDialog = true
                            } else {
                                Prefs.readingChallengeEnrolled = true
                                dismiss()
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // expand by default
        dialog?.let {
            val bottomSheet = it.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                BottomSheetBehavior.from(sheet).state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
    }

    @Composable
    fun OnboardingScreen(
        modifier: Modifier = Modifier,
        onboardingItems: List<OnboardingItem>,
        onCloseClick: () -> Unit,
        onLearnMoreClick: () -> Unit,
        onJoinClick: () -> Unit
    ) {
        Scaffold(
            modifier = modifier
                .safeDrawingPadding(),
            containerColor = WikipediaTheme.colors.paperColor,
            bottomBar = {
                TwoButtonBottomBar(
                    primaryButtonText = stringResource(R.string.reading_challenge_onboarding_join_button),
                    secondaryButtonText = stringResource(R.string.reading_challenge_onboarding_learn_more_button),
                    onPrimaryOnClick = onJoinClick,
                    onSecondaryOnClick = onLearnMoreClick
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                IconButton(
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(bottom = 12.dp)
                        .offset(x = 12.dp),
                    onClick = {
                        onCloseClick()
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(24.dp),
                        painter = painterResource(R.drawable.ic_close_black_24dp),
                        contentDescription = stringResource(R.string.search_clear_query_content_description),
                        tint = WikipediaTheme.colors.primaryColor
                    )
                }

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    textAlign = TextAlign.Center,
                    text = stringResource(R.string.reading_challenge_onboarding_header),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
                    color = WikipediaTheme.colors.primaryColor
                )

                onboardingItems.forEach { onboardingItem ->
                    OnboardingListItem(
                        modifier = Modifier.padding(bottom = 16.dp),
                        item = onboardingItem
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    text = stringResource(R.string.reading_challenge_onboarding_note),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = WikipediaTheme.colors.placeholderColor
                )
            }
        }
    }

    @Preview
    @Composable
    private fun OnboardingScreenPreview() {
        BaseTheme(
            currentTheme = Theme.LIGHT
        ) {
            OnboardingScreen(
                onboardingItems = onboardingItems,
                onCloseClick = {},
                onLearnMoreClick = {},
                onJoinClick = {}
            )
        }
    }

    companion object {
        fun newInstance(): ReadingChallengeOnboardingDialog {
            return ReadingChallengeOnboardingDialog()
        }
    }
}
