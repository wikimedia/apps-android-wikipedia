package org.wikipedia

import android.annotation.SuppressLint
import android.net.Uri
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.MotionEvent
import android.view.View
import android.view.View.OnCreateContextMenuListener
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebView.HitTestResult
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.LongPressMenu
import org.wikipedia.util.DeviceUtil.hideSoftKeyboard
import org.wikipedia.util.UriUtil.isValidPageLink

class LongPressHandler(view: View, private val historySource: Int, private val callback: LongPressMenu.Callback) : OnCreateContextMenuListener, OnTouchListener {
    interface WebViewMenuCallback : LongPressMenu.Callback {
        val wikiSite: WikiSite?
        val referrer: String?
    }

    private var referrer: String? = null
    private var title: PageTitle? = null
    private var clickPositionX = 0f
    private var clickPositionY = 0f

    constructor(view: View, pageTitle: PageTitle, historySource: Int, callback: LongPressMenu.Callback) : this(view, historySource, callback) {
        title = pageTitle
    }

    init {
        view.setOnCreateContextMenuListener(this)
        view.setOnTouchListener(this)
    }

    override fun onCreateContextMenu(menu: ContextMenu, view: View, menuInfo: ContextMenuInfo?) {
        if (view is WebView) {
            title = null
            val result = view.hitTestResult
            if (result.type == HitTestResult.SRC_ANCHOR_TYPE) {
                val uri = Uri.parse(result.extra)
                if (isValidPageLink(uri)) {
                    var wikiSite = WikiSite(uri)
                    // the following logic keeps the correct language code if the domain has multiple variants (e.g. zh).
                    (callback as WebViewMenuCallback).wikiSite?.run {
                        if (wikiSite.dbName() == dbName() && wikiSite.languageCode != languageCode) {
                            wikiSite = this
                        }
                    }
                    title = wikiSite.titleForInternalLink(uri.path)
                    referrer = callback.referrer
                    showPopupMenu(view, true)
                }
            }
        } else {
            showPopupMenu(view, false)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {
        if (motionEvent.action == MotionEvent.ACTION_DOWN) {
            clickPositionX = motionEvent.x
            clickPositionY = motionEvent.y
        }
        return false
    }

    private fun showPopupMenu(view: View, createAnchorView: Boolean) {
        title?.let {
            if (!it.isSpecial && view.isAttachedToWindow) {
                hideSoftKeyboard(view)
                HistoryEntry(it, historySource).let { entry ->
                    entry.referrer = referrer
                    var anchorView = view
                    if (createAnchorView) {
                        val tempView = View(view.context)
                        tempView.x = clickPositionX
                        tempView.y = clickPositionY
                        (view.rootView as ViewGroup).addView(tempView)
                        anchorView = tempView
                    }
                    LongPressMenu(anchorView, true, callback).show(entry)
                }
            }
        }
    }
}
