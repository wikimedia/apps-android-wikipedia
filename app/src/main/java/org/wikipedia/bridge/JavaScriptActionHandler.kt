package org.wikipedia.bridge

import android.content.Context
import org.wikipedia.BuildConfig
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.auth.AccountUtil
import org.wikipedia.dataclient.RestService
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageViewModel
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.DimenUtil.getDensityScalar
import org.wikipedia.util.DimenUtil.leadImageHeightForDevice
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.L10nUtil.formatDateRelative
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
    fun scrollToFooter(context: Context): String {
        return "window.scrollTo(0, document.getElementById('pcs-footer-container-menu').offsetTop - ${DimenUtil.getNavigationBarHeight(context)});"
    }

    @JvmStatic
    fun setUp(): String {
        val app: WikipediaApp = WikipediaApp.getInstance()
        val topActionBarHeight = (Math.round(app.getResources().getDimensionPixelSize(R.dimen.lead_no_image_top_offset_dp) / getDensityScalar()))
        return String.format("{" +
                "   \"platform\": \"pcs.c1.Platforms.ANDROID\"," +
                "   \"clientVersion\": \"${BuildConfig.VERSION_NAME}\"," +
                "   \"theme\": \"${app.currentTheme.funnelName}\"," +
                "   \"dimImages\": ${(app.currentTheme.isDark && Prefs.shouldDimDarkModeImages())}," +
                "   \"margins\": { \"top\": \"%dpx\", \"right\": \"%dpx\", \"bottom\": \"%dpx\", \"left\": \"%dpx\" }," +
                "   \"leadImageHeight\": \"%dpx\"," +
                "   \"areTablesInitiallyExpanded\": ${!Prefs.isCollapseTablesEnabled()}," +
                "   \"textSizeAdjustmentPercentage\": \"100%%\"," +
                "   \"loadImages\": ${Prefs.isImageDownloadEnabled()}," +
                "   \"userGroups\": \"${AccountUtil.getGroups()}\"" +
                "}", topActionBarHeight + 16, 16, 48, 16, (leadImageHeightForDevice() / getDensityScalar()).roundToInt() - topActionBarHeight)
    }

    @JvmStatic
    fun setUpEditButtons(isEditable: Boolean, isProtected: Boolean): String {
        return "pcs.c1.Page.setEditButtons($isEditable, $isProtected)"
    }

    @JvmStatic
    fun setFooter(context: Context, model: PageViewModel): String {
        if (model.page == null) {
            return ""
        }

        val languageCount = L10nUtil.getUpdatedLanguageCountIfNeeded(model.page!!.title.wikiSite.languageCode(),
                model.page!!.pageProperties.languageCount)
        val showLanguagesLink = languageCount > 0
        val showEditHistoryLink = !(model.page!!.isMainPage || model.page!!.isFilePage)
        val lastModifiedDate = formatDateRelative(model.page!!.pageProperties.lastModified)
        val showTalkLink = !(model.page!!.title.namespace() === Namespace.TALK)
        val showMapLink = model.page!!.pageProperties.geo != null

        // TODO: page-library also supports showing disambiguation ("similar pages") links and
        // "page issues". We should be mindful that they exist, even if we don't want them for now.

        return "pcs.c1.Footer.add({" +
                "   platform: pcs.c1.Platforms.ANDROID," +
                "   clientVersion: '${BuildConfig.VERSION_NAME}'," +
                "   title: '${model.page?.convertedTitle}'," +
                "   menu: {" +
                "       items: [" +
                                (if (showLanguagesLink) "pcs.c1.Footer.MenuItemType.languages, " else "") +
                                (if (showEditHistoryLink) "pcs.c1.Footer.MenuItemType.lastEdited, " else "") +
                                (if (showTalkLink) "pcs.c1.Footer.MenuItemType.talkPage, " else "") +
                                (if (showMapLink) "pcs.c1.Footer.MenuItemType.coordinate, " else "") +
                "               pcs.c1.Footer.MenuItemType.referenceList " +
                "              ]" +
                "   }," +
                "   l10n: {" +
                "           'readMoreHeading': '${context.getString(R.string.read_more_section)}'," +
                "           'menuDisambiguationTitle': '${context.getString(R.string.page_similar_titles)}'," +
                "           'menuLanguagesTitle': '${context.getString(R.string.language_count_link_text, languageCount)}'," +
                "           'menuHeading': '${context.getString(R.string.about_article_section)}'," +
                "           'menuLastEditedSubtitle': '${context.getString(R.string.edit_history_link_text)}'," +
                "           'menuLastEditedTitle': '${context.getString(R.string.last_updated_text, lastModifiedDate)}'," +
                "           'licenseString': '${context.getString(R.string.content_license_html)}'," +
                "           'menuTalkPageTitle': '${context.getString(R.string.talk_page_link_text)}'," +
                "           'viewInBrowserString': '${context.getString(R.string.page_view_in_browser)}'," +
                "           'licenseSubstitutionString': 'CC BY-SA 3.0'," +
                "           'menuCoordinateTitle': '${context.getString(R.string.map_view_link_text)}'," +
                "           'menuReferenceListTitle': '${context.getString(R.string.reference_list_title)}'" +
                "       }," +
                "   readMore: { " +
                "       itemCount: 3," +
                "       baseURL: '${model.title?.wikiSite?.url() + RestService.REST_API_PREFIX}'" +
                "   }" +
                "})"
    }
}
