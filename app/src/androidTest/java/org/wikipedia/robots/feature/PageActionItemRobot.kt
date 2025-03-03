package org.wikipedia.robots.feature

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import org.wikipedia.R
import org.wikipedia.base.TestConfig
import org.wikipedia.base.TestThemeColorType
import org.wikipedia.base.TestWikipediaColors
import org.wikipedia.base.base.BaseRobot
import org.wikipedia.theme.Theme

class PageActionItemRobot : BaseRobot() {

    fun clickShare() = apply {
        // we are doing this because if we open the share dialog which is a system dialog and
        // espresso cannot interact with it, so this tells the Espresso
        // when the share dialog opens, pretend the user immediately pressed back
        // or made a selection
        intending(hasAction(Intent.ACTION_CHOOSER)).respondWith(
            Instrumentation.ActivityResult(Activity.RESULT_OK, null)
        )
        click.onViewWithId(R.id.page_share)
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickWatch() = apply {
        click.onViewWithId(R.id.page_watch)
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickTalkPage() = apply {
        click.onViewWithId(R.id.page_view_talk_page)
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyTalkPageIsOpened() = apply {
        verify.viewWithTextDisplayed("Talk: Apple")
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickEditHistory() = apply {
        click.onViewWithId(R.id.page_view_edit_history)
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyEditHistoryIsOpened() = apply {
        verify.viewWithTextDisplayed("Revision history: Apple")
        delay(TestConfig.DELAY_SHORT)
    }

    fun assertViewOnMapIsGreyed(theme: Theme) = apply {
        val color = TestWikipediaColors.getGetColor(theme, TestThemeColorType.INACTIVE)
        verify.textViewColor(
            textViewId = R.id.page_view_on_map,
            colorResId = color,
        )
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickNewTab() = apply {
        click.onViewWithId(R.id.page_new_tab)
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickExplore() = apply {
        click.onViewWithId(R.id.page_explore)
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickCategories() = apply {
        click.onViewWithId(R.id.page_categories)
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyCategoryDialogAppears() = apply {
        verify.viewWithTextDisplayed("Categories")
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickEditArticles() = apply {
        click.onViewWithId(R.id.page_edit_article)
        delay(TestConfig.DELAY_SHORT)
    }

    fun clickCustomizeToolbar() = apply {
        click.onViewWithId(R.id.customize_toolbar)
        delay(TestConfig.DELAY_SHORT)
    }

    fun verifyCustomizeToolbarIsOpened() = apply {
        verify.viewWithTextDisplayed("Customize toolbar")
    }

    fun pressBack() = apply {
        goBack()
        delay(TestConfig.DELAY_SHORT)
    }
}
