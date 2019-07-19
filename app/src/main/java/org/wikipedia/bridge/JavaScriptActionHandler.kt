package org.wikipedia.bridge

import android.content.Context
import org.wikipedia.R
import org.wikipedia.dataclient.RestService
import org.wikipedia.page.PageTitle

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
    fun setFooter(context: Context, title: PageTitle): String {
        return "pagelib.c1.Footer.add(document," +
                "    '" + title.displayText + "'," +
                "    [pagelib.c1.Footer.MenuItemType.languages, pagelib.c1.Footer.MenuItemType.lastEdited, pagelib.c1.Footer.MenuItemType.pageIssues, " +
                "pagelib.c1.Footer.MenuItemType.disambiguation, pagelib.c1.Footer.MenuItemType.talkPage, pagelib.c1.Footer.MenuItemType.coordinate]," +
                "    {" +
                "        'readMoreHeading': '" + context.getString(R.string.read_more_section) + "'," +
                "        'menuDisambiguationTitle': '" + context.getString(R.string.page_similar_titles) + "'," +
                "        'menuLanguagesTitle': 'Available in FIXME other languages'," +
                "        'menuHeading': '" + context.getString(R.string.about_article_section) + "'," +
                "        'menuLastEditedSubtitle': '" + context.getString(R.string.edit_history_link_text) + "'," +
                "        'menuLastEditedTitle': 'Edited today'," +
                "        'licenseString': '" + context.getString(R.string.content_license_html) + "'," +
                "        'menuTalkPageTitle': '" + context.getString(R.string.talk_page_link_text) + "'," +
                "        'menuPageIssuesTitle': 'Page issues'," +
                "        'viewInBrowserString': '" + context.getString(R.string.page_view_in_browser) + "'," +
                "        'licenseSubstitutionString': 'CC BY-SA 3.0'," +
                "        'menuCoordinateTitle': '" + context.getString(R.string.map_view_link_text) + "'" +
                "     }, 3," +
                "     '" + title.wikiSite.url() + RestService.REST_API_PREFIX + "'" +
                ")"
    }
}
