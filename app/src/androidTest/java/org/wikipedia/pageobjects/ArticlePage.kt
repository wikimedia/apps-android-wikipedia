package org.wikipedia.pageobjects
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.web.assertion.WebViewAssertions
import androidx.test.espresso.web.model.Atom
import androidx.test.espresso.web.model.ElementReference
import androidx.test.espresso.web.sugar.Web
import androidx.test.espresso.web.sugar.Web.onWebView
import androidx.test.espresso.web.webdriver.DriverAtoms
import androidx.test.espresso.web.webdriver.Locator
import androidx.test.espresso.web.webdriver.DriverAtoms.findElement
import org.hamcrest.CoreMatchers

class ArticlePage: BasePage() {

    private val articleHeader = findElement(Locator.XPATH, "//h1[@data-id='0']/span[@class='mw-page-title-main']")

    fun checkArticleTitle(text: String): Boolean {
      return  checkWebArticleTitle(articleHeader,text)
    }

    private fun checkWebArticleTitle(articleHeader: Atom<ElementReference>, text: String): Boolean {
        return try {
            onWebView()
                .withElement(articleHeader).check(
                    WebViewAssertions.webMatches(
                        DriverAtoms.getText(),
                        CoreMatchers.containsString(text)
                    )
                )
            true
        } catch (ex: NoMatchingViewException) {
            false
        }
    }
}