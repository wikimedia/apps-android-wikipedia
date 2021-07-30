package org.wikipedia.edit.summaries

import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.database.AppDatabase
import org.wikipedia.edit.db.EditSummary
import org.wikipedia.page.PageTitle
import org.wikipedia.util.L10nUtil.setConditionalTextDirection

class EditSummaryHandler(private val container: View,
                         private val summaryEdit: AutoCompleteTextView,
                         title: PageTitle) {

    init {
        container.setOnClickListener { summaryEdit.requestFocus() }
        setConditionalTextDirection(summaryEdit, title.wikiSite.languageCode())

        AppDatabase.instance.editSummaryDao().getEditSummaries()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { summaries ->
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
        AppDatabase.instance.editSummaryDao().insertEditSummary(EditSummary(summary = summaryEdit.text.toString()))
            .subscribeOn(Schedulers.io())
            .subscribe()
    }

    fun handleBackPressed(): Boolean {
        if (container.visibility == View.VISIBLE) {
            container.visibility = View.GONE
            return true
        }
        return false
    }
}
