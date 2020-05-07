package org.wikipedia.bridge

import android.content.Context
import org.wikipedia.BuildConfig
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageTitle
import org.wikipedia.page.PageViewModel
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.DimenUtil.getDensityScalar
import org.wikipedia.util.DimenUtil.leadImageHeightForDevice
import org.wikipedia.util.L10nUtil
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object JavaScriptActionHandler {
    @JvmStatic
    fun setTopMargin(top: Int): String {
        return String.format("pcs.c1.Page.setMargins({ top:'%dpx', right:'%dpx', bottom:'%dpx', left:'%dpx' })", top + 16, 16, 48, 16)
    }

    @JvmStatic
    fun getTextSelection(): String {
        return "pcs.c1.InteractionHandling.getSelectionInfo()"
    }

    @JvmStatic
    fun getOffsets(): String {
        return "pcs.c1.Sections.getOffsets(document.body);"
    }

    @JvmStatic
    fun getSections(): String {
        return "pcs.c1.Page.getTableOfContents()"
    }

    @JvmStatic
    fun getProtection(): String {
        return "pcs.c1.Page.getProtection()"
    }

    @JvmStatic
    fun getRevision(): String {
        return "pcs.c1.Page.getRevision();"
    }

    @JvmStatic
    fun scrollToFooter(context: Context): String {
        return "window.scrollTo(0, document.getElementById('pcs-footer-container-menu').offsetTop - ${DimenUtil.getNavigationBarHeight(context)});"
    }

    @JvmStatic
    fun scrollToAnchor(anchorLink: String): String {
        val anchor = if (anchorLink.contains("#")) anchorLink.substring(anchorLink.indexOf("#") + 1) else anchorLink
        return "var el = document.getElementById('$anchor');" +
                "window.scrollTo(0, el.offsetTop - (screen.height / 2));" +
                "setTimeout(function(){ el.style.backgroundColor='#fc3';" +
                "    setTimeout(function(){ el.style.backgroundColor=null; }, 500);" +
                "}, 250);"
    }

    @JvmStatic
    fun prepareToScrollTo(anchorLink: String, highlight: Boolean): String {
        return "pcs.c1.Page.prepareForScrollToAnchor(\"${anchorLink.replace("\"", "\\\"")}\", { highlight: $highlight } )"
    }

    @JvmStatic
    fun setUp(context: Context, title: PageTitle, isPreview: Boolean): String {
        val app: WikipediaApp = WikipediaApp.getInstance()
        val topActionBarHeight = if (isPreview) 0 else (app.resources.getDimensionPixelSize(R.dimen.lead_no_image_top_offset_dp) / getDensityScalar()).roundToInt()
        val res = L10nUtil.getStringsForArticleLanguage(title, intArrayOf(R.string.description_edit_add_description,
                R.string.table_infobox, R.string.table_other, R.string.table_close))
        val leadImageHeight = if (isPreview) 0 else
            (if (DimenUtil.isLandscape(context) || !Prefs.isImageDownloadEnabled()) 0 else (leadImageHeightForDevice(context) / getDensityScalar()).roundToInt() - topActionBarHeight)
        val topMargin = topActionBarHeight + 16

        return String.format("{" +
                "   \"platform\": \"android\"," +
                "   \"clientVersion\": \"${BuildConfig.VERSION_NAME}\"," +
                "   \"l10n\": {" +
                "       \"addTitleDescription\": \"${res[R.string.description_edit_add_description]}\"," +
                "       \"tableInfobox\": \"${res[R.string.table_infobox]}\"," +
                "       \"tableOther\": \"${res[R.string.table_other]}\"," +
                "       \"tableClose\": \"${res[R.string.table_close]}\"" +
                "   }," +
                "   \"theme\": \"${app.currentTheme.funnelName}\"," +
                "   \"dimImages\": ${(app.currentTheme.isDark && Prefs.shouldDimDarkModeImages())}," +
                "   \"margins\": { \"top\": \"%dpx\", \"right\": \"%dpx\", \"bottom\": \"%dpx\", \"left\": \"%dpx\" }," +
                "   \"leadImageHeight\": \"%dpx\"," +
                "   \"areTablesInitiallyExpanded\": ${!Prefs.isCollapseTablesEnabled()}," +
                "   \"textSizeAdjustmentPercentage\": \"100%%\"," +
                "   \"loadImages\": ${Prefs.isImageDownloadEnabled()}," +
                "   \"userGroups\": \"${AccountUtil.getGroups()}\"" +
                "}", topMargin, 16, 48, 16, leadImageHeight)
    }

    @JvmStatic
    fun setUpEditButtons(isEditable: Boolean, isProtected: Boolean): String {
        return "pcs.c1.Page.setEditButtons($isEditable, $isProtected)"
    }

    @JvmStatic
    fun setFooter(model: PageViewModel): String {
        if (model.page == null) {
            return ""
        }
        val showTalkLink = !(model.page!!.title.namespace() === Namespace.TALK)
        val showMapLink = model.page!!.pageProperties.geo != null
        val editedDaysAgo = TimeUnit.MILLISECONDS.toDays(Date().time - model.page!!.pageProperties.lastModified.time)

        // TODO: page-library also supports showing disambiguation ("similar pages") links and
        // "page issues". We should be mindful that they exist, even if we don't want them for now.
        val baseURL = ServiceFactory.getRestBasePath(model.title?.wikiSite!!).trimEnd('/')
        return "pcs.c1.Footer.add({" +
                "   platform: \"android\"," +
                "   clientVersion: \"${BuildConfig.VERSION_NAME}\"," +
                "   title: \"${model.title!!.prefixedText}\"," +
                "   menu: {" +
                "       items: [" +
                                "pcs.c1.Footer.MenuItemType.lastEdited, " +
                                (if (showTalkLink) "pcs.c1.Footer.MenuItemType.talkPage, " else "") +
                                (if (showMapLink) "pcs.c1.Footer.MenuItemType.coordinate, " else "") +
                "               pcs.c1.Footer.MenuItemType.referenceList " +
                "              ]," +
                "       fragment: \"pcs-menu\"," +
                "       editedDaysAgo: $editedDaysAgo" +
                "   }," +
                "   readMore: { " +
                "       itemCount: 3," +
                "       baseURL: \"${baseURL}\"," +
                "       fragment: \"pcs-read-more\"" +
                "   }" +
                "})"
    }
}
