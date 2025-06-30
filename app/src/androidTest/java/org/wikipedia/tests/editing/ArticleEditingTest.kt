package org.wikipedia.tests.editing

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Test
import org.junit.runner.RunWith
import org.wikipedia.base.BaseTest
import org.wikipedia.main.MainActivity
import org.wikipedia.robots.DialogRobot
import org.wikipedia.robots.feature.EditorRobot
import org.wikipedia.robots.feature.LoginRobot
import org.wikipedia.robots.feature.PageActionItemRobot
import org.wikipedia.robots.feature.PageRobot
import org.wikipedia.robots.feature.SearchRobot
import org.wikipedia.robots.feature.SettingsRobot
import org.wikipedia.robots.navigation.BottomNavRobot
import org.wikipedia.robots.screen.LanguageListRobot
import java.util.UUID

@LargeTest
@RunWith(AndroidJUnit4::class)
class ArticleEditingTest : BaseTest<MainActivity>(
 activityClass = MainActivity::class.java) {

    private val editorRobot = EditorRobot()
    private val dialogRobot = DialogRobot()
    private val pageRobot = PageRobot(context)
    private val pageActionItemRobot = PageActionItemRobot()
    private val bottomNavRobot = BottomNavRobot()
    private val loginRobot = LoginRobot()
    private val searchRobot = SearchRobot()
    private val settingsRobot = SettingsRobot()
    private val languageListRobot = LanguageListRobot()

    val boldText = "Bold text ${UUID.randomUUID()}"
    val italicText = "Italic text ${UUID.randomUUID()}"
    val underlineText = "Underline text"
    val strikeThroughText = "Strikethrough text ${UUID.randomUUID()}"
    val superScript = "2"
    val subScript = "10"
    val largeText = "Large Text"
    val smallText = "Small Text"
    val code = "fun main() { println(\"Hello World!!\")}"
    val h2 = "Heading 2 ${UUID.randomUUID()}"
    val h3 = "Heading 3"
    val h4 = "Heading 4"
    val h5 = "Heading 5"
    val textFormattingTexts = "What is Espresso Test?\n\n" +
            "$boldText\n\n" +
            "$italicText\n\n" +
            "$underlineText\n\n" +
            "$strikeThroughText\n\n" +
            "X$superScript\n\n" +
            "X$subScript\n\n" +
            "$largeText\n\n" +
            "$smallText\n\n" +
            "$code\n\n" +
            "$h2\n\n" +
            "$h3\n\n" +
            "$h4\n\n" +
            "$h5\n\n"
    @Test
    fun runTest() {
        proceedToTestArticle()
        when (EditingType.entries.random()) {
            EditingType.TEXT_FORMAT -> startTextFormatEditing()
            EditingType.LIST_AND_MEDIA -> startListAndMediaEditing()
        }
    }

    private fun proceedToTestArticle() {
        bottomNavRobot
            .navigateToMoreMenu()
            .clickLoginMenuItem()
        loginRobot
            .logInUser()
        bottomNavRobot
            .navigateToMoreMenu()
            .goToSettings()
        settingsRobot
            .clickLanguages()
        languageListRobot
            .addNewLanguage()
            .scrollToLanguageAndClick("Test")
            .pressBack()
            .pressBack()
        searchRobot
            .tapSearchView()
            .test()
            .typeTextInView("What is Espresso")
            .clickOnItemFromSearchList(0)
    }

    private fun startTextFormatEditing() {
        pageRobot
            .clickOverFlowMenuToolbar()
        pageActionItemRobot
            .clickEditArticles()
        dialogRobot
            .click("Got it")
        editorRobot
            .closeEditNotice()
            .replaceTextInEditWindow("")
            .typeInEditWindow(textFormattingTexts)
            // Text Formatting
            .clickTextFormatButton()
            .selectSpecificText(boldText)
            .applyBoldFormat()
            .selectSpecificText(italicText)
            .applyItalicFormat()
            .selectSpecificText(underlineText)
            .applyUnderlineFormat()
            .selectSpecificText(strikeThroughText)
            .applyStrikeThroughFormat()
            .selectSpecificText(superScript)
            .applySuperScriptFormat()
            .selectSpecificText(subScript)
            .applySubScriptFormat()
            .selectSpecificText(largeText)
            .applyLargeTextFormat()
            .selectSpecificText(smallText)
            .applySmallTextFormat()
            .scrollToEndOfTextFormatting()
            .selectSpecificText(code)
            .applyCodeFormat()
            .closeTextFormatting()
            // Headline Formatting
            .clickHeadingFormats()
            .selectSpecificText(h2)
            .applyH2()
            .selectSpecificText(h3)
            .applyH3()
            .selectSpecificText(h4)
            .applyH4()
            .selectSpecificText(h5)
            .applyH5()
            .closeHeadlinesFormatting()
            .closeKeyboard()
            .clickNext()
            // publishing screen
            .clickNext()
            .checkMinorEdit()
            .clickPublish()
        editorRobot
            .verifyEditPublished(context)
    }

    private fun startListAndMediaEditing() {
        pageRobot
            .clickOverFlowMenuToolbar()
        pageActionItemRobot
            .clickEditArticles()
        dialogRobot
            .click("Got it")
        editorRobot
            .closeEditNotice()
            .replaceTextInEditWindow("")
            .typeInEditWindow("* Apple\n*Orange\n#Bread\n#Peanut Butter\n")
            .clickUndoButton()
            .clickRedoButton()
            .clickInsertMediaButton()
            .insertImageFrom(0)
            .clickNext()
            .clickInsert()
            .clickEditWindow()
            .typeInEditWindow("\n")
            .clickInsertLinkButton()
        searchRobot
            .typeTextInView("What is Espresso")
            .clickOnItemFromSearchList(0)
        editorRobot
            .clickNext()
            // publishing screen
            .clickNext()
            .checkMinorEdit()
            .clickPublish()
        editorRobot
            .verifyEditPublished(context)
    }

    enum class EditingType {
        TEXT_FORMAT, LIST_AND_MEDIA
    }
}
