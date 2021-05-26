package org.wikipedia.pageimages

import android.database.Cursor
import androidx.core.content.contentValuesOf
import org.wikipedia.database.DatabaseTable
import org.wikipedia.database.column.Column
import org.wikipedia.database.contract.PageImageHistoryContract
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.PageTitle

// todo: network caching preserves images. Remove this class and drop table?
class PageImageDatabaseTable : DatabaseTable<PageImage>(PageImageHistoryContract.TABLE, PageImageHistoryContract.Image.URI) {
    override fun fromCursor(cursor: Cursor): PageImage {
        val wiki = WikiSite(PageImageHistoryContract.Col.SITE.value(cursor), PageImageHistoryContract.Col.LANG.value(cursor))
        val title = PageTitle(PageImageHistoryContract.Col.NAMESPACE.value(cursor), PageImageHistoryContract.Col.API_TITLE.value(cursor), wiki)
        val imageName = PageImageHistoryContract.Col.IMAGE_NAME.value(cursor)
        title.setDisplayText(PageImageHistoryContract.Col.DISPLAY_TITLE.value(cursor))
        return PageImage(title, imageName)
    }

    override fun toContentValues(obj: PageImage) = contentValuesOf(
            PageImageHistoryContract.Col.SITE.name to obj.title.wikiSite.authority(),
            PageImageHistoryContract.Col.LANG.name to obj.title.wikiSite.languageCode(),
            PageImageHistoryContract.Col.NAMESPACE.name to obj.title.namespace,
            PageImageHistoryContract.Col.API_TITLE.name to obj.title.prefixedText,
            PageImageHistoryContract.Col.DISPLAY_TITLE.name to obj.title.displayText,
            PageImageHistoryContract.Col.IMAGE_NAME.name to obj.imageName
    )

    override fun getColumnsAdded(version: Int): Array<Column<*>> {
        return when (version) {
            INITIAL_DB_VERSION -> arrayOf(PageImageHistoryContract.Col.ID, PageImageHistoryContract.Col.SITE, PageImageHistoryContract.Col.API_TITLE, PageImageHistoryContract.Col.IMAGE_NAME)
            DB_VER_NAMESPACE_ADDED -> arrayOf(PageImageHistoryContract.Col.NAMESPACE)
            DB_VER_LANG_ADDED -> arrayOf(PageImageHistoryContract.Col.LANG)
            DB_VER_DISPLAY_TITLE_ADDED -> arrayOf(PageImageHistoryContract.Col.DISPLAY_TITLE)
            else -> super.getColumnsAdded(version)
        }
    }

    override fun getPrimaryKeySelection(obj: PageImage, selectionArgs: Array<String>): String {
        return super.getPrimaryKeySelection(obj, PageImageHistoryContract.Col.SELECTION)
    }

    override fun getUnfilteredPrimaryKeySelectionArgs(obj: PageImage): Array<String?> {
        return arrayOf(obj.title.wikiSite.authority(), obj.title.wikiSite.languageCode(), obj.title.namespace, obj.title.text)
    }

    override val dBVersionIntroducedAt
        get() = INITIAL_DB_VERSION

    companion object {
        private const val DB_VER_NAMESPACE_ADDED = 7
        private const val DB_VER_LANG_ADDED = 10
        private const val DB_VER_DISPLAY_TITLE_ADDED = 19
    }
}
