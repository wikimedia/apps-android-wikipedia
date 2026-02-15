package org.wikipedia.lesson03.homeworks

import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import org.wikipedia.views.AppTextView
import org.wikipedia.R as R

val skipButton = listOf(
    MaterialButton::class.java,
    R.id.fragment_onboarding_skip_button,
    R.string.onboarding_skip
)

val continueButton = listOf(
    MaterialButton::class.java,
    R.id.fragment_onboarding_forward_button,
    R.string.onboarding_continue
)

val getStartedButton = listOf(
    MaterialButton::class.java,
    R.id.fragment_onboarding_done_button,
    R.string.onboarding_get_started
)

val addLanguageButton = listOf(
    MaterialButton::class.java,
    R.id.addLanguageButton,
    R.string.onboarding_multilingual_add_language_text
)

val imageStartOnboarding = listOf(
    AppCompatImageView::class.java,
    R.id.imageViewCentered,
    R.drawable.illustration_onboarding_explore
)

val textStartOnboardingFreeWikipedia = listOf(
    AppTextView::class.java,
    R.id.primaryTextView,
    R.string.onboarding_welcome_title_v2
)

val pageIndicatorOnboarding = listOf(
    TabLayout::class.java,
    R.id.view_onboarding_page_indicator
)

val secondarytextStartOnboarding = listOf(
    AppTextView::class.java,
    R.id.secondaryTextView,
    R.string.onboarding_multilingual_secondary_text
)

