import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import org.wikipedia.R
import org.wikipedia.views.AppTextView
import kotlin.jvm.java


val centralImageView = listOf(
    AppCompatImageView::class.java,
    R.id.imageViewCentered,
    null
)

val mainSloganTextView = listOf(
    AppTextView::class.java,
    R.id.primaryTextView,
    R.string.onboarding_welcome_title_v2
)
val suggestAddLanguageTextView = listOf(
    AppTextView::class.java,
    R.id.secondaryTextView,
    null
)

val languagesArea = listOf(
    RecyclerView::class.java,
    R.id.languagesList,
    null
)

val languageTitleOptional = listOf(
    AppTextView::class.java,
    R.id.option_label,
    null
)

val addAndEditLanguagesButton = listOf(
    MaterialButton::class.java,
    R.id.addLanguageButton,
    R.string.onboarding_multilingual_add_language_text
)

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
val tabLayout = listOf(
    TabLayout::class.java,
    R.id.view_onboarding_page_indicator,
    null
)

val newWaysToExploreText = listOf(
    AppTextView::class.java,
    R.id.primaryTextView,
    R.string.onboarding_explore_title
)