package org.wikipedia.edit.summaries

import android.content.Context
import android.database.Cursor
import android.os.RemoteException
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.FilterQueryProvider
import android.widget.TextView
import androidx.cursoradapter.widget.CursorAdapter
import org.wikipedia.WikipediaApp
import org.wikipedia.database.contract.EditHistoryContract
import org.wikipedia.page.PageTitle
import org.wikipedia.util.ContentProviderClientCompat.close
import org.wikipedia.util.L10nUtil.setConditionalTextDirection
import java.util.*

class EditSummaryHandler(private val container: View,
                         private val summaryEdit: AutoCompleteTextView,
                         title: PageTitle) {

    init {
        container.setOnClickListener { summaryEdit.requestFocus() }
        val adapter = EditSummaryAdapter(container.context, null, true)
        summaryEdit.setAdapter(adapter)
        adapter.filterQueryProvider = FilterQueryProvider { charSequence ->
            val client = EditSummary.DATABASE_TABLE.acquireClient(container.context.applicationContext)!!
            val uri = EditHistoryContract.Summary.URI
            val projection: Array<String>? = null
            val selection = EditHistoryContract.Summary.SUMMARY.qualifiedName() + " like ?"
            val selectionArgs = arrayOf("$charSequence%")
            val order = EditHistoryContract.Summary.ORDER_MRU
            try {
                return@FilterQueryProvider client.query(uri, projection, selection, selectionArgs, order)
            } catch (e: RemoteException) {
                throw RuntimeException(e)
            } finally {
                close(client)
            }
        }
        setConditionalTextDirection(summaryEdit, title.wikiSite.languageCode)
    }

    fun show() {
        container.visibility = View.VISIBLE
    }

    fun persistSummary() {
        val summary = EditSummary(summaryEdit.text.toString(), Date())
        WikipediaApp.getInstance().getDatabaseClient(EditSummary::class.java).upsert(summary, EditHistoryContract.Summary.SELECTION)
    }

    fun handleBackPressed(): Boolean {
        if (container.visibility == View.VISIBLE) {
            container.visibility = View.GONE
            return true
        }
        return false
    }

    private inner class EditSummaryAdapter constructor(context: Context?, c: Cursor?, autoReQuery: Boolean) : CursorAdapter(context, c, autoReQuery) {
        override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
            return LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false)
        }

        override fun bindView(convertView: View, context: Context, cursor: Cursor) {
            (convertView as TextView).text = convertToString(cursor)
        }

        override fun convertToString(cursor: Cursor): CharSequence {
            return EditSummary.DATABASE_TABLE.fromCursor(cursor).summary
        }
    }
}
