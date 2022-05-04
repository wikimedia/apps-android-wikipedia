package org.wikipedia.page.shareafact

import android.os.Build
import android.view.ActionMode
import android.view.MenuItem
import kotlinx.serialization.Serializable
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.bridge.CommunicationBridge
import org.wikipedia.bridge.JavaScriptActionHandler
import org.wikipedia.json.JsonUtil
import org.wikipedia.page.PageFragment
import org.wikipedia.util.log.L
import org.wikipedia.wiktionary.WiktionaryDialog

class ShareHandler(private val fragment: PageFragment, private val bridge: CommunicationBridge) {
    private var webViewActionMode: ActionMode? = null

    private fun onEditHerePayload(sectionID: Int, text: String, isEditingDescription: Boolean) {
        if (sectionID == 0 && isEditingDescription) {
            fragment.verifyBeforeEditingDescription(text, Constants.InvokeSource.PAGE_EDIT_HIGHLIGHT)
        } else {
            if (sectionID >= 0) {
                fragment.editHandler.startEditingSection(sectionID, text)
            }
        }
    }

    private fun finishActionMode() {
        webViewActionMode?.run {
            finish()
            webViewActionMode = null
        }
    }

    fun showWiktionaryDefinition(text: String) {
        fragment.title?.let {
            fragment.showBottomSheet(WiktionaryDialog.newInstance(it, text))
        }
    }

    fun onTextSelected(mode: ActionMode) {
        webViewActionMode = mode
        mode.menu.findItem(R.id.menu_text_select_define)?.run {
            if (shouldEnableWiktionaryDialog()) {
                isVisible = true
                setOnMenuItemClickListener(RequestTextSelectOnMenuItemClickListener(PAYLOAD_PURPOSE_DEFINE))
            }
        }
        mode.menu.findItem(R.id.menu_text_edit_here)?.run {
            setOnMenuItemClickListener(RequestTextSelectOnMenuItemClickListener(PAYLOAD_PURPOSE_EDIT_HERE))
            fragment.page?.run {
                if (!isArticle) {
                    isVisible = false
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mode.invalidateContentRect()
        }
    }

    fun shouldEnableWiktionaryDialog(): Boolean {
        return fragment.title?.run { WiktionaryDialog.enabledLanguages.contains(wikiSite.languageCode) } ?: false
    }

    private inner class RequestTextSelectOnMenuItemClickListener constructor(private val purpose: String) : MenuItem.OnMenuItemClickListener {
        override fun onMenuItemClick(item: MenuItem): Boolean {
            // send an event to the WebView that will make it return the
            // selected text (or first paragraph) back to us...
            bridge.evaluate(JavaScriptActionHandler.getTextSelection()) { value ->
                if (!fragment.isAdded) {
                    return@evaluate
                }
                finishActionMode()
                try {
                    val message = JsonUtil.decodeFromString<TextSelectResponse>(value)!!
                    when (purpose) {
                        PAYLOAD_PURPOSE_DEFINE -> showWiktionaryDefinition(message.text)
                        PAYLOAD_PURPOSE_EDIT_HERE -> onEditHerePayload(message.section, message.text, message.isTitleDescription)
                        else -> L.d("Unknown purpose=$purpose")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            return true
        }
    }

    @Serializable
    private class TextSelectResponse {
        val text: String = ""
        val section: Int = 0
        val isTitleDescription: Boolean = false
    }

    companion object {
        private const val PAYLOAD_PURPOSE_DEFINE = "define"
        private const val PAYLOAD_PURPOSE_EDIT_HERE = "edit_here"
    }
}
