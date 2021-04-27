package org.wikipedia.page.shareafact

import android.view.ActionMode
import android.view.MenuItem
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.json.JSONException
import org.json.JSONObject
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.analytics.ShareAFactFunnel
import org.wikipedia.bridge.CommunicationBridge
import org.wikipedia.bridge.JavaScriptActionHandler
import org.wikipedia.page.PageFragment
import org.wikipedia.util.log.L
import org.wikipedia.wiktionary.WiktionaryDialog
import java.util.*

class ShareHandler(private val fragment: PageFragment, private val bridge: CommunicationBridge) {
    private var webViewActionMode: ActionMode? = null
    private var funnel: ShareAFactFunnel? = null
    private val disposables = CompositeDisposable()

    private fun createFunnel() {
        fragment.page?.let {
            funnel = ShareAFactFunnel(WikipediaApp.getInstance(), it.title, it.pageProperties.pageId,
                    it.pageProperties.revisionId)
        }
    }

    private fun onEditHerePayload(sectionID: Int, text: String, isEditingDescription: Boolean) {
        if (sectionID == 0 && isEditingDescription) {
            fragment.verifyBeforeEditingDescription(text)
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

    fun dispose() {
        disposables.clear()
    }

    fun showWiktionaryDefinition(text: String) {
        fragment.showBottomSheet(WiktionaryDialog.newInstance(fragment.title, text))
    }

    fun onTextSelected(mode: ActionMode) {
        webViewActionMode = mode
        mode.menu.findItem(R.id.menu_text_select_define).run {
            if (shouldEnableWiktionaryDialog()) {
                isVisible = true
                setOnMenuItemClickListener(RequestTextSelectOnMenuItemClickListener(PAYLOAD_PURPOSE_DEFINE))
            }
        }
        mode.menu.findItem(R.id.menu_text_edit_here).run {
            setOnMenuItemClickListener(RequestTextSelectOnMenuItemClickListener(PAYLOAD_PURPOSE_EDIT_HERE))
            fragment.page?.run {
                if (!isArticle) {
                    isVisible = false
                }
            }
        }
        if (funnel == null) {
            createFunnel()
        }
        funnel?.logHighlight()
    }

    fun shouldEnableWiktionaryDialog(): Boolean {
        return WiktionaryDialog.enabledLanguages.contains(fragment.title.wikiSite.languageCode())
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
                val messagePayload: JSONObject
                try {
                    messagePayload = JSONObject(value)
                    val text = messagePayload.optString(PAYLOAD_TEXT_KEY, "")
                    when (purpose) {
                        PAYLOAD_PURPOSE_DEFINE -> showWiktionaryDefinition(text.toLowerCase(Locale.getDefault()))
                        PAYLOAD_PURPOSE_EDIT_HERE -> onEditHerePayload(messagePayload.optInt("section", 0), text, messagePayload.optBoolean("isTitleDescription", false))
                        else -> L.d("Unknown purpose=$purpose")
                    }
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
            return true
        }
    }

    companion object {
        private const val PAYLOAD_PURPOSE_DEFINE = "define"
        private const val PAYLOAD_PURPOSE_EDIT_HERE = "edit_here"
        private const val PAYLOAD_TEXT_KEY = "text"
    }
}
