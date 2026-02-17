package org.wikipedia.bridge

import android.content.Context
import kotlinx.serialization.Serializable
import org.wikipedia.BuildConfig
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.extensions.getStrings
import org.wikipedia.json.JsonUtil
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.page.PageViewModel
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DimenUtil
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object JavaScriptActionHandler {

    fun setTopMargin(top: Int): String {
        return setMargins(top + 16, 48)
    }

    fun setMargins(top: Int, bottom: Int): String {
        return "pcs.c1.Page.setMargins({ top:'${top}px', bottom:'${bottom}px' })"
    }

    private fun buildTextFormattingCss(): String {
        val sb = StringBuilder()
        val selectors = "body, .pcs-body, .mw-parser-output, .mw-parser-output p, .mw-parser-output li," +
                " .mw-parser-output dd, .mw-parser-output td"
        val rules = mutableListOf<String>()
        if (Prefs.isTextJustifyEnabled) {
            rules.add("text-align: justify !important")
        }
        if (Prefs.isTextHyphenationEnabled) {
            rules.add("-webkit-hyphens: auto !important")
            rules.add("hyphens: auto !important")
            rules.add("word-wrap: break-word !important")
            rules.add("overflow-wrap: break-word !important")
        }
        if (rules.isNotEmpty()) {
            sb.append("$selectors { ${rules.joinToString("; ")}; }")
        }
        return sb.toString()
    }

    fun setHorizontalMargins(multiplier: Int): String {
        // multiplier 10 = default PCS behavior (no margin override)
        // multiplier 9..0 = progressively wider content
        return "(function() {" +
                "var s = document.getElementById('app-margin-style');" +
                "if (!s) { s = document.createElement('style'); s.id = 'app-margin-style'; document.head.appendChild(s); }" +
                "if ($multiplier >= 10) { s.innerHTML = '${buildTextFormattingCss()}'; return; }" +
                "s.innerHTML = '" +
                "html, body { overflow-x: hidden !important; }" +
                " body, .pcs-body, .content, .mw-body, #content, #bodyContent, .mw-parser-output, section" +
                " { max-width: 100% !important; box-sizing: border-box !important; }" +
                " body { padding-left: ${multiplier + 1}vw !important; padding-right: ${multiplier + 1}vw !important;" +
                " margin-left: auto !important; margin-right: auto !important; }" +
                " ${buildTextFormattingCss()}';" +
                "})();"
    }

    fun getTextSelection(): String {
        return "pcs.c1.InteractionHandling.getSelectionInfo()"
    }

    fun getOffsets(): String {
        return "pcs.c1.Sections.getOffsets(document.body);"
    }

    fun getSections(): String {
        return "pcs.c1.Page.getTableOfContents()"
    }

    fun getProtection(): String {
        return "pcs.c1.Page.getProtection()"
    }

    fun getRevision(): String {
        return "pcs.c1.Page.getRevision();"
    }

    fun expandCollapsedTables(expand: Boolean): String {
        return "pcs.c1.Page.expandOrCollapseTables($expand);" +
                "var hideableSections = document.getElementsByClassName('pcs-section-hideable-header'); " +
                "for (var i = 0; i < hideableSections.length; i++) { " +
                "  pcs.c1.Sections.setHidden(hideableSections[i].parentElement.getAttribute('data-mw-section-id'), ${!expand});" +
                "}"
    }

    fun scrollToFooter(context: Context): String {
        return "window.scrollTo(0, document.getElementById('pcs-footer-container-menu').offsetTop - ${DimenUtil.getNavigationBarHeight(context)});"
    }

    fun scrollToAnchor(anchorLink: String): String {
        val anchor = anchorLink.substringAfter('#')
        return "var el = document.getElementById('$anchor');" +
                "window.scrollTo(0, el.offsetTop - (screen.height / 2));" +
                "setTimeout(function(){ el.style.backgroundColor='#fc3';" +
                "    setTimeout(function(){ el.style.backgroundColor=null; }, 500);" +
                "}, 250);"
    }

    fun prepareToScrollTo(anchorLink: String, highlight: Boolean): String {
        return "pcs.c1.Page.prepareForScrollToAnchor(\"${anchorLink.replace("\"", "\\\"")}\", { highlight: $highlight } )"
    }

    fun removeHighlights(): String {
        return "pcs.c1.Page.removeHighlightsFromHighlightedElements()"
    }

    fun setUp(context: Context, title: PageTitle, isPreview: Boolean, toolbarMargin: Int, messageCardHeight: Int): String {
        val app = WikipediaApp.instance
        val topActionBarHeight = if (isPreview) 0 else DimenUtil.roundedPxToDp(toolbarMargin.toFloat())
        val res = context.getStrings(title, intArrayOf(R.string.description_edit_add_description,
                R.string.table_infobox, R.string.table_other, R.string.table_close))
        var leadImageHeight = if (isPreview) 0 else
            (if (DimenUtil.isLandscape(context) || !Prefs.isImageDownloadEnabled) 0 else (DimenUtil.leadImageHeightForDevice(context) / DimenUtil.densityScalar).roundToInt() - topActionBarHeight)
        leadImageHeight = leadImageHeight + messageCardHeight
        val topMargin = topActionBarHeight + 16

        var fontFamily = Prefs.fontFamily
        if (fontFamily == context.getString(R.string.font_family_serif)) {
            fontFamily = "'Linux Libertine',Georgia,Times,serif"
        }

        return String.format(Locale.ROOT, "{" +
                "   \"platform\": \"android\"," +
                "   \"clientVersion\": \"${BuildConfig.VERSION_NAME}\"," +
                "   \"l10n\": {" +
                "       \"addTitleDescription\": \"${res[R.string.description_edit_add_description]}\"," +
                "       \"tableInfobox\": \"${res[R.string.table_infobox]}\"," +
                "       \"tableOther\": \"${res[R.string.table_other]}\"," +
                "       \"tableClose\": \"${res[R.string.table_close]}\"" +
                "   }," +
                "   \"theme\": \"${app.currentTheme.tag}\"," +
                "   \"bodyFont\": \"$fontFamily\"," +
                "   \"dimImages\": ${(app.currentTheme.isDark && Prefs.dimDarkModeImages)}," +
                "   \"margins\": { \"top\": \"%dpx\", \"bottom\": \"%dpx\" }," +
                "   \"leadImageHeight\": \"%dpx\"," +
                "   \"areTablesInitiallyExpanded\": ${isPreview || !Prefs.isCollapseTablesEnabled}," +
                "   \"textSizeAdjustmentPercentage\": \"100%%\"," +
                "   \"loadImages\": ${Prefs.isImageDownloadEnabled}," +
                "   \"userGroups\": ${JsonUtil.encodeToString(AccountUtil.groups)}," +
                "   \"isEditable\": ${!Prefs.readingFocusModeEnabled}" +
                "}", topMargin, 48, leadImageHeight)
    }

    fun setUpEditButtons(isEditable: Boolean, isProtected: Boolean): String {
        return "pcs.c1.Page.setEditButtons($isEditable, $isProtected)"
    }

    fun setFooter(model: PageViewModel): String {
        if (model.page == null) {
            return ""
        }
        val showTalkLink = model.page!!.title.namespace() !== Namespace.TALK
        val showMapLink = model.page!!.pageProperties.geo != null
        val editedDaysAgo = TimeUnit.MILLISECONDS.toDays(Date().time - model.page!!.pageProperties.lastModified.time)
        val langCode = model.title?.wikiSite?.languageCode ?: WikipediaApp.instance.appOrSystemLanguageCode

        // TODO: page-library also supports showing disambiguation ("similar pages") links and
        // "page issues". We should be mindful that they exist, even if we don't want them for now.
        return "pcs.c1.Footer.add({" +
                "   platform: \"android\"," +
                "   clientVersion: \"${BuildConfig.VERSION_NAME}\"," +
                "   menu: {" +
                "       items: [" +
                                "pcs.c1.Footer.MenuItemType.lastEdited, " +
                                (if (showTalkLink) "pcs.c1.Footer.MenuItemType.talkPage, " else "") +
                                (if (showMapLink) "pcs.c1.Footer.MenuItemType.coordinate, " else "") +
                                "pcs.c1.Footer.MenuItemType.pageIssues, " +
                "               pcs.c1.Footer.MenuItemType.referenceList " +
                "              ]," +
                "       fragment: \"pcs-menu\"," +
                "       editedDaysAgo: $editedDaysAgo" +
                "   }," +
                "   readMore: { " +
                "       itemCount: 3," +
                "       readMoreLazy: true," +
                "       langCode: \"$langCode\"," +
                "       fragment: \"pcs-read-more\"" +
                "   }" +
                "})"
    }

    fun appendReadMode(model: PageViewModel): String {
        if (model.page == null) {
            return ""
        }
        val apiBaseURL = model.title?.wikiSite!!.scheme() + "://" + model.title?.wikiSite!!.uri.authority!!.trimEnd('/')
        val langCode = model.title?.wikiSite?.languageCode ?: WikipediaApp.instance.appOrSystemLanguageCode
        return "pcs.c1.Footer.appendReadMore({" +
                "   platform: \"android\"," +
                "   clientVersion: \"${BuildConfig.VERSION_NAME}\"," +
                "   readMore: { " +
                "       itemCount: 3," +
                "       apiBaseURL: \"$apiBaseURL\"," +
                "       langCode: \"$langCode\"," +
                "       fragment: \"pcs-read-more\"" +
                "   }" +
                "})"
    }

    fun mobileWebChromeShim(marginTop: Int, marginBottom: Int): String {
        return "(function() {" +
                "let style = document.createElement('style');" +
                "style.innerHTML = '.header-chrome { visibility: hidden; margin-top: ${marginTop}px; height: 0px; } #page-secondary-actions { display: none; } .mw-footer { padding-bottom: ${marginBottom}px; } .page-actions-menu { display: none; } .minerva__tab-container { display: none; } .banner-container { display: none; }';" +
                "document.head.appendChild(style);" +
                "})();"
    }

    fun mobileWebSetDarkMode(): String {
        return "(function() {" +
                "document.documentElement.classList.add('skin-theme-clientpref-night');" +
                "})();"
    }

    fun getElementAtPosition(x: Int, y: Int): String {
        return "(function() {" +
                "  let element = document.elementFromPoint($x, $y);" +
                "  let result = {};" +
                "  result.left = element.getBoundingClientRect().left;" +
                "  result.top = element.getBoundingClientRect().top;" +
                "  result.width = element.clientWidth;" +
                "  result.height = element.clientHeight;" +
                "  result.src = element.src;" +
                "  return result;" +
                "})();"
    }

    fun pauseAllMedia(): String {
        return "(function() {" +
                "var elements = document.getElementsByTagName('audio');" +
                "for(i=0; i<elements.length; i++) elements[i].pause();" +
                "})();"
    }

    @Serializable
    class ImageHitInfo(val left: Float = 0f, val top: Float = 0f, val width: Float = 0f, val height: Float = 0f,
                       val src: String = "")
}
