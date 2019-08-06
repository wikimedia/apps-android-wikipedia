package org.wikipedia.bridge

import android.content.Context
import org.wikipedia.BuildConfig
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.dataclient.RestService
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageViewModel
import org.wikipedia.settings.Prefs
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.L10nUtil.formatDateRelative

object JavaScriptActionHandler {
    @JvmStatic
    fun setHandler(): String {
        return ("pagelib.c1.InteractionHandling.setInteractionHandler((interaction) => { marshaller.onReceiveMessage(JSON.stringify(interaction))})")
    }

    @JvmStatic
    fun setMargin(top: Int, right: Int, bottom: Int, left: Int): String {
        return String.format("pagelib.c1.Page.setMargins({ top:'%dpx', right:'%dpx', bottom:'%dpx', left:'%dpx' })", top, right, bottom, left)
    }

    @JvmStatic
    fun setScrollTop(top: Int): String {
        return String.format("pagelib.c1.Page.setScrollTop(%d)", top)
    }

    @JvmStatic
    fun getOffsets(): String {
        return String.format("pagelib.c1.Sections.getOffsets(document.body);")
    }

    @JvmStatic
    fun setUp(): String {
        val app: WikipediaApp = WikipediaApp.getInstance()
        return String.format("pagelib.c1.Page.setup({" +
                "platform: pagelib.c1.Platforms.ANDROID," +
                "clientVersion: '%s'," +
                "theme: pagelib.c1.Themes.%s," +
                "dimImages: %b," +
                "areTablesInitiallyExpanded: %b," +
                "textSizeAdjustmentPercentage: '100%%'," +
                "loadImages: %b" +
                "})", BuildConfig.VERSION_NAME, app.getCurrentTheme().getFunnelName().toUpperCase(),
                (app.getCurrentTheme().isDark() && Prefs.shouldDimDarkModeImages()),
                !Prefs.isCollapseTablesEnabled(), Prefs.isImageDownloadEnabled())
    }

    @JvmStatic
    fun setUpEditButtons(isEditable: Boolean, isProtected: Boolean): String {
        return String.format("pagelib.c1.Page.setEditButtons(%b, %b)", isEditable, isProtected)
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

        return "pagelib.c1.Footer.add({" +
                "platform: pagelib.c1.Platforms.ANDROID," +
                "clientVersion: '" + BuildConfig.VERSION_NAME + "'," +
                "title: '${model.page?.displayTitle}'," +
                "menuItems: [" +
                (if (showLanguagesLink) "pagelib.c1.Footer.MenuItemType.languages, " else "") +
                (if (showEditHistoryLink) "pagelib.c1.Footer.MenuItemType.lastEdited, " else "") +
                (if (showTalkLink) "pagelib.c1.Footer.MenuItemType.talkPage, " else "") +
                (if (showMapLink) "pagelib.c1.Footer.MenuItemType.coordinate, " else "") +
                "pagelib.c1.Footer.MenuItemType.referenceList " +
                "], l10n: {" +
                "        'readMoreHeading': '${context.getString(R.string.read_more_section)}'," +
                "        'menuDisambiguationTitle': '${context.getString(R.string.page_similar_titles)}'," +
                "        'menuLanguagesTitle': '${context.getString(R.string.language_count_link_text, languageCount)}'," +
                "        'menuHeading': '${context.getString(R.string.about_article_section)}'," +
                "        'menuLastEditedSubtitle': '${context.getString(R.string.edit_history_link_text)}'," +
                "        'menuLastEditedTitle': '${context.getString(R.string.last_updated_text, lastModifiedDate)}'," +
                "        'licenseString': '${context.getString(R.string.content_license_html)}'," +
                "        'menuTalkPageTitle': '${context.getString(R.string.talk_page_link_text)}'," +
                "        'viewInBrowserString': '${context.getString(R.string.page_view_in_browser)}'," +
                "        'licenseSubstitutionString': 'CC BY-SA 3.0'," +
                "        'menuCoordinateTitle': '${context.getString(R.string.map_view_link_text)}'," +
                "        'menuReferenceListTitle': '${context.getString(R.string.reference_list_title)}'" +
                "     }," +
                "readMore: { " +
                "    itemCount: 3," +
                "    baseURL: '${model.title?.wikiSite?.url() + RestService.REST_API_PREFIX}'" +
                "}" +
                "})"
    }
}
