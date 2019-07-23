package org.wikipedia.bridge

import android.content.Context
import org.wikipedia.R
import org.wikipedia.dataclient.RestService
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageViewModel
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.L10nUtil.formatDateRelative

object JavaScriptActionHandler {
    @JvmStatic
    fun setHandler(): String {
        return ("pagelib.c1.InteractionHandling.setInteractionHandler((interaction) => { marshaller.onReceiveMessage(JSON.stringify(interaction))})")
    }

    @JvmStatic
    fun setMargin(context: Context, top: Int, right: Int, bottom: Int, left: Int): String {
        return (context.getString(R.string.page_mh_set_margins_script, top, right, bottom, left))
    }

    @JvmStatic
    fun setScrollTop(context: Context, top: Int): String {
        return (context.getString(R.string.page_mh_set_scrollTop_script, top))
    }

    @JvmStatic
    fun setMulti(context: Context, theme: String, dimImages: Boolean, collapseTables: Boolean): String {
        return context.getString(R.string.page_mh_set_multi_script, theme, dimImages, collapseTables)
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
        val lastEditText = (context.getString(R.string.last_updated_text, formatDateRelative(model.page!!.pageProperties.lastModified)))
        val showTalkLink = !(model.page!!.title.namespace() === Namespace.TALK)
        val showMapLink = model.page!!.pageProperties.geo != null

        // TODO: page-library also supports showing disambiguation ("similar pages") links and
        // "page issues". We should be mindful that they exist, even if we don't want them for now.

        return "pagelib.c1.Footer.add(document," +
                "    '${model.page?.displayTitle}'," +
                "    [" +
                (if (showLanguagesLink) "pagelib.c1.Footer.MenuItemType.languages, " else "") +
                (if (showEditHistoryLink) "pagelib.c1.Footer.MenuItemType.lastEdited, " else "") +
                (if (showTalkLink) "pagelib.c1.Footer.MenuItemType.talkPage, " else "") +
                (if (showMapLink) "pagelib.c1.Footer.MenuItemType.coordinate " else "") +
                "],   {" +
                "        'readMoreHeading': '" + context.getString(R.string.read_more_section) + "'," +
                "        'menuDisambiguationTitle': '" + context.getString(R.string.page_similar_titles) + "'," +
                "        'menuLanguagesTitle': '" + context.getString(R.string.language_count_link_text, model.page?.pageProperties?.languageCount) + "'," +
                "        'menuHeading': '" + context.getString(R.string.about_article_section) + "'," +
                "        'menuLastEditedSubtitle': '" + context.getString(R.string.edit_history_link_text) + "'," +
                "        'menuLastEditedTitle': '${lastEditText}'," +
                "        'licenseString': '" + context.getString(R.string.content_license_html) + "'," +
                "        'menuTalkPageTitle': '" + context.getString(R.string.talk_page_link_text) + "'," +
                "        'viewInBrowserString': '" + context.getString(R.string.page_view_in_browser) + "'," +
                "        'licenseSubstitutionString': 'CC BY-SA 3.0'," +
                "        'menuCoordinateTitle': '" + context.getString(R.string.map_view_link_text) + "'" +
                "     }, 3," +
                "     '${model.title?.wikiSite?.url()}${RestService.REST_API_PREFIX}'" +
                ")"
    }
}
