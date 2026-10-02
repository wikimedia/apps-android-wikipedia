package org.wikipedia.bridge

import android.content.Context
import kotlinx.serialization.Serializable
import org.json.JSONObject
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
import org.wikipedia.util.StringUtil
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt

object JavaScriptActionHandler {

    fun setTopMargin(top: Int): String {
        return setMargins(top + 16, 48)
    }

    fun setMargins(top: Int, bottom: Int): String {
        return "pcs.c1.Page.setMargins({ top:'${top}px', bottom:'${bottom}px' })"
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

    fun setUp(context: Context, title: PageTitle, isPreview: Boolean, toolbarMargin: Int, messageCardHeight: Float): String {
        val app = WikipediaApp.instance
        val topActionBarHeight = if (isPreview) 0 else DimenUtil.roundedPxToDp(toolbarMargin.toFloat())
        val res = context.getStrings(title, intArrayOf(R.string.description_edit_add_description,
                R.string.table_infobox, R.string.table_other, R.string.table_close))
        var leadImageHeight = if (isPreview) 0 else
            (if (DimenUtil.isLandscape(context) || !Prefs.isImageDownloadEnabled) 0 else (DimenUtil.leadImageHeightForDevice(context) / DimenUtil.densityScalar).roundToInt() - topActionBarHeight)
        leadImageHeight += DimenUtil.roundedPxToDp(messageCardHeight)
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
        val showMapLink = model.page!!.summary.coordinates != null
        val editedDaysAgo = ChronoUnit.DAYS.between(model.page!!.lastModified, LocalDateTime.now())
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

    fun semanticSearchTextHighlight(headingId: String?, snippet: String?): String? {
        if (headingId == null) {
            return null
        }
        val searchString = extractSemanticSearchString(snippet) ?: return null
        return highlightTextInSectionAndScroll(headingId, searchString)
    }

    private fun extractSemanticSearchString(snippet: String?): String? {
        val searchString = Regex("""<span\s+class=["']searchmatch["']\s*>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
            .find(snippet.orEmpty())
            ?.groupValues
            ?.getOrNull(1)
            ?: return null

        return StringUtil.fromHtml(searchString).toString()
    }

    private fun highlightTextInSectionAndScroll(headingId: String, searchString: String): String {
        return """
            (function(headingId, searchString) {
            
                const NAME = 'semantic-search-highlight';
                const style = document.createElement('style');
                style.textContent = '::highlight(' + NAME + ') { background-color: yellow; }';
                document.head.appendChild(style);
                
                const normalizeCharForMatch = ch => ch
                    .toLowerCase()
                    .normalize('NFKD')
                    .replace(/[\s\p{Cf}]/gu, '')
                    .replace(/\u03c2/g, '\u03c3')
                    .replace(/[\u2018\u2019\u201a\u201b\u02bc\u2032]/g, "'")
                    .replace(/[\u201c\u201d\u201e\u201f\u2033]/g, '"');

                const root = document.getElementById(headingId)?.closest('section')
                const query = Array.from(searchString, normalizeCharForMatch).join('');
                if (!root || !query) {
                    return false;
                }

                const walker = document.createTreeWalker(
                    root, 
                    NodeFilter.SHOW_TEXT,
                    (node) => node.parentElement.closest('script, style, sup.reference, .mw-ref, .mwe-math-mathml-a11y')
                        ? NodeFilter.FILTER_REJECT 
                        : NodeFilter.FILTER_ACCEPT
                );
        
                let normalizedSectionText = '';
                const sources = [];
                let node;
                while ((node = walker.nextNode())) {
                    let charIndexInTextNode = 0;
                    Array.from(node.nodeValue).forEach((rawChar) => {
                        const normalizedChar = normalizeCharForMatch(rawChar);
                        normalizedSectionText += normalizedChar;
                        for (let i = 0; i < normalizedChar.length; i++) {
                            sources.push([node, charIndexInTextNode, charIndexInTextNode + rawChar.length]);
                        }
                        charIndexInTextNode += rawChar.length;
                    });
                }

                const index = normalizedSectionText.indexOf(query);
                if (index === -1) {
                    return false;
                }
                const [startNode, startIndex] = sources[index];
                const [endNode, , endIndex] = sources[index + query.length - 1];
                const range = document.createRange();
                range.setStart(startNode, startIndex);
                range.setEnd(endNode, endIndex);
                CSS.highlights.set(NAME, new Highlight(range));

                const rect = range.getBoundingClientRect();
                window.scrollTo({ top: window.scrollY + rect.top + rect.height / 2 - window.innerHeight / 2, behavior: 'instant' });
                return true;
                
            })(${JSONObject.quote(headingId)}, ${JSONObject.quote(searchString)});
        """
    }

    @Serializable
    class ImageHitInfo(val left: Float = 0f, val top: Float = 0f, val width: Float = 0f, val height: Float = 0f,
                       val src: String = "")
}
