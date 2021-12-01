package org.wikipedia.edit

import android.app.Activity
import android.content.Intent
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.bridge.CommunicationBridge
import org.wikipedia.bridge.CommunicationBridge.JSEventListener
import org.wikipedia.descriptions.DescriptionEditUtil
import org.wikipedia.page.Page
import org.wikipedia.page.PageFragment
import org.wikipedia.util.log.L

class EditHandler(private val fragment: PageFragment, bridge: CommunicationBridge) : JSEventListener {

    private var currentPage: Page? = null

    init {
        bridge.addListener(TYPE_EDIT_SECTION, this)
        bridge.addListener(TYPE_ADD_TITLE_DESCRIPTION, this)
    }

    override fun onMessage(messageType: String, messagePayload: JsonObject?) {
        if (!fragment.isAdded) {
            return
        }

        currentPage?.let {
            if (messageType == TYPE_EDIT_SECTION) {
                val sectionId = messagePayload?.run { this[PAYLOAD_SECTION_ID]?.jsonPrimitive?.int } ?: 0
                if (sectionId == 0 && DescriptionEditUtil.isEditAllowed(it)) {
                    val tempView = View(fragment.requireContext())
                    tempView.x = fragment.webView.touchStartX
                    tempView.y = fragment.webView.touchStartY
                    (fragment.view as ViewGroup).addView(tempView)
                    val menu = PopupMenu(fragment.requireContext(), tempView, 0, 0, R.style.PagePopupMenu)
                    menu.menuInflater.inflate(R.menu.menu_page_header_edit, menu.menu)
                    menu.setOnMenuItemClickListener(EditMenuClickListener())
                    menu.setOnDismissListener { (fragment.view as ViewGroup).removeView(tempView) }
                    menu.show()
                } else {
                    startEditingSection(sectionId, null)
                }
            } else if (messageType == TYPE_ADD_TITLE_DESCRIPTION && DescriptionEditUtil.isEditAllowed(it)) {
                fragment.verifyBeforeEditingDescription(null)
            }
        }
    }

    private inner class EditMenuClickListener : PopupMenu.OnMenuItemClickListener {
        override fun onMenuItemClick(item: MenuItem): Boolean {
            return when (item.itemId) {
                R.id.menu_page_header_edit_description -> {
                    fragment.verifyBeforeEditingDescription(null)
                    true
                }
                R.id.menu_page_header_edit_lead_section -> {
                    startEditingSection(0, null)
                    true
                }
                else -> false
            }
        }
    }

    fun startEditingSection(sectionID: Int, highlightText: String?) {
        startEditingSection(fragment.requireActivity(), currentPage, sectionID, highlightText)
    }

    fun setPage(page: Page?) {
        page?.let {
            currentPage = it
        }
    }

    companion object {
        private const val TYPE_EDIT_SECTION = "edit_section"
        private const val TYPE_ADD_TITLE_DESCRIPTION = "add_title_description"
        private const val PAYLOAD_SECTION_ID = "sectionId"
        const val RESULT_REFRESH_PAGE = 1

        fun startEditingSection(activity: Activity, page: Page?, sectionID: Int, highlightText: String?) {
            page?.let {
                if (sectionID < 0 || sectionID >= it.sections.size) {
                    L.d("it.sections " + it.sections)
                    L.w("Attempting to edit a mismatched section ID.")
                    return
                }
                val section = it.sections[sectionID]
                val intent = Intent(activity, EditSectionActivity::class.java)
                intent.putExtra(EditSectionActivity.EXTRA_SECTION_ID, section.id)
                intent.putExtra(EditSectionActivity.EXTRA_SECTION_ANCHOR, section.anchor)
                intent.putExtra(EditSectionActivity.EXTRA_TITLE, it.title)
                intent.putExtra(EditSectionActivity.EXTRA_PAGE_PROPS, it.pageProperties)
                intent.putExtra(EditSectionActivity.EXTRA_HIGHLIGHT_TEXT, highlightText)
                activity.startActivityForResult(intent, Constants.ACTIVITY_REQUEST_EDIT_SECTION)
            }
        }
    }
}
