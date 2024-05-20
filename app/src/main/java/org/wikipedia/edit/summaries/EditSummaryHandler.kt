package org.wikipedia.edit.summaries

import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.wikipedia.database.AppDatabase
import org.wikipedia.edit.db.EditSummary
import org.wikipedia.page.PageTitle
import org.wikipedia.util.L10nUtil.setConditionalTextDirection

class EditSummaryHandler(private val container: View,
                         private val summaryEdit: AutoCompleteTextView,
                         title: PageTitle) {

    init {
        container.setOnClickListener { summaryEdit.requestFocus() }
        setConditionalTextDirection(summaryEdit, title.wikiSite.languageCode)

        CoroutineScope(Dispatchers.Main).launch {
            val summaries = AppDatabase.instance.editSummaryDao().getEditSummaries()
            if (container.isAttachedToWindow) {
                updateAutoCompleteList(summaries)
            }
        }
    }

    private fun updateAutoCompleteList(editSummaries: List<EditSummary>) {
        val adapter = ArrayAdapter(container.context, android.R.layout.simple_list_item_1, editSummaries)
        summaryEdit.setAdapter(adapter)
    }

    fun show() {
        container.visibility = View.VISIBLE
    }

    fun persistSummary() {
        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.instance.editSummaryDao().insertEditSummary(EditSummary(summary = summaryEdit.text.toString()))
        }
    }

    fun handleBackPressed(): Boolean {
        if (container.visibility == View.VISIBLE) {
            container.visibility = View.GONE
            return true
        }
        return false
    }
}
