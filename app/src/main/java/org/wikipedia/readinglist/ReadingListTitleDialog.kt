package org.wikipedia.readinglist

import android.app.Activity
import org.wikipedia.R
import org.wikipedia.views.TextInputDialog

object ReadingListTitleDialog {
    fun interface Callback {
        fun onSuccess(text: String, description: String)
    }

    @JvmStatic
    fun readingListTitleDialog(activity: Activity,
                               title: String,
                               description: String?,
                               otherTitles: List<String?>,
                               callback: Callback?): TextInputDialog {
        return TextInputDialog(activity).let { textInputDialog ->
            textInputDialog.callback = object : TextInputDialog.Callback {
                override fun onShow(dialog: TextInputDialog) {
                    dialog.setHint(R.string.reading_list_name_hint)
                    dialog.setSecondaryHint(R.string.reading_list_description_hint)
                    dialog.setText(title, true)
                    dialog.setSecondaryText(description.orEmpty())
                }

                override fun onTextChanged(text: CharSequence, dialog: TextInputDialog) {
                    text.toString().trim().let {
                        when {
                            it.isEmpty() -> {
                                dialog.setError(null)
                                dialog.setPositiveButtonEnabled(false)
                            }
                            otherTitles.contains(it) -> {
                                dialog.setError(dialog.context.getString(R.string.reading_list_title_exists, it))
                                dialog.setPositiveButtonEnabled(false)
                            }
                            else -> {
                                dialog.setError(null)
                                dialog.setPositiveButtonEnabled(true)
                            }
                        }
                    }
                }

                override fun onSuccess(text: CharSequence, secondaryText: CharSequence) {
                    callback?.onSuccess(text.toString().trim(), secondaryText.toString().trim())
                }

                override fun onCancel() {}
            }
            textInputDialog.showSecondaryText(true)
        }
    }
}
