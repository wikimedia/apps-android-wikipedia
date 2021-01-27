package org.wikipedia.views

import android.content.Context
import android.graphics.Color
import android.view.ActionProvider
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.OnLongClick
import org.wikipedia.R
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.ResourceUtil

open class FindInPageActionProvider(private val context: Context) : ActionProvider(context) {
    interface FindInPageListener {
        fun onFindNextClicked()
        fun onFindNextLongClicked()
        fun onFindPrevClicked()
        fun onFindPrevLongClicked()
        fun onCloseClicked()
        fun onSearchTextChanged(text: String?)
    }

    @BindView(R.id.find_in_page_next)
    var findInPageNext: View? = null

    @BindView(R.id.find_in_page_prev)
    var findInPagePrev: View? = null

    @BindView(R.id.find_in_page_match)
    var findInPageMatch: TextView? = null

    @BindView(R.id.find_in_page_input)
    var searchView: SearchView? = null
    private var listener: FindInPageListener? = null
    private var enableLastOccurrenceSearchFlag = false
    private var lastOccurrenceSearchFlag = false
    private var isFirstOccurrence = false
    private var isLastOccurrence = false
    override fun overridesItemVisibility(): Boolean {
        return true
    }

    override fun onCreateActionView(): View {
        val view = View.inflate(context, R.layout.group_find_in_page, null)
        ButterKnife.bind(this, view)
        setFindInPageChevronsEnabled(false)
        searchView!!.queryHint = context.getString(R.string.menu_page_find_in_page)
        searchView!!.isFocusable = true
        searchView!!.setOnQueryTextListener(searchQueryListener)
        searchView!!.isIconified = false
        searchView!!.maxWidth = Int.MAX_VALUE
        searchView!!.inputType = EditorInfo.TYPE_CLASS_TEXT
        searchView!!.isSubmitButtonEnabled = false
        // remove focus line from search plate
        val searchEditPlate = searchView!!.findViewById<View>(androidx.appcompat.R.id.search_plate)
        searchEditPlate.setBackgroundColor(Color.TRANSPARENT)
        // remove the close icon in search view
        val searchCloseButton = searchView!!.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        searchCloseButton.isEnabled = false
        searchCloseButton.setImageDrawable(null)
        DeviceUtil.setContextClickAsLongClick(findInPageNext!!)
        DeviceUtil.setContextClickAsLongClick(findInPagePrev!!)
        return view
    }

    fun setListener(listener: FindInPageListener?) {
        this.listener = listener
    }

    fun setSearchViewQuery(searchQuery: String) {
        searchView!!.setQuery(searchQuery, true)
    }

    fun setEnableLastOccurrenceSearchFlag(enable: Boolean) {
        enableLastOccurrenceSearchFlag = enable
    }

    fun setMatchesResults(activeMatchOrdinal: Int, numberOfMatches: Int) {
        if (numberOfMatches > 0) {
            findInPageMatch!!.text = context.getString(R.string.find_in_page_result,
                    activeMatchOrdinal + 1, numberOfMatches)
            findInPageMatch!!.setTextColor(ResourceUtil.getThemedColor(context, R.attr.material_theme_de_emphasised_color))
            setFindInPageChevronsEnabled(true)
            isFirstOccurrence = activeMatchOrdinal == 0
            isLastOccurrence = activeMatchOrdinal + 1 == numberOfMatches
        } else {
            findInPageMatch!!.text = "0/0"
            findInPageMatch!!.setTextColor(ResourceUtil.getThemedColor(context, R.attr.colorError))
            setFindInPageChevronsEnabled(false)
            isFirstOccurrence = false
            isLastOccurrence = false
        }
        if (enableLastOccurrenceSearchFlag && lastOccurrenceSearchFlag) {
            // Go one occurrence back from the first one so it shows the last one.
            lastOccurrenceSearchFlag = false
            listener!!.onFindPrevClicked()
        }
        findInPageMatch!!.visibility = View.VISIBLE
    }

    @OnClick(R.id.find_in_page_next)
    fun onFindInPageNextClicked(v: View?) {
        DeviceUtil.hideSoftKeyboard(v)
        listener!!.onFindNextClicked()
    }

    @OnLongClick(R.id.find_in_page_next)
    fun onFindInPageNextLongClicked(v: View?): Boolean {
        if (isLastOccurrence) {
            Toast.makeText(context, context.getString(R.string.find_last_occurence), Toast.LENGTH_SHORT).show()
        } else {
            DeviceUtil.hideSoftKeyboard(v)
            listener!!.onFindNextLongClicked()
            lastOccurrenceSearchFlag = true
        }
        return true
    }

    @OnClick(R.id.find_in_page_prev)
    fun onFindInPagePrevClicked(v: View?) {
        DeviceUtil.hideSoftKeyboard(v)
        listener!!.onFindPrevClicked()
    }

    @OnLongClick(R.id.find_in_page_prev)
    fun onFindInPagePrevLongClicked(v: View?): Boolean {
        if (isFirstOccurrence) {
            Toast.makeText(context, context.getString(R.string.find_first_occurence), Toast.LENGTH_SHORT).show()
        } else {
            DeviceUtil.hideSoftKeyboard(v)
            listener!!.onFindPrevLongClicked()
        }
        return true
    }

    @OnClick(R.id.close_button)
    fun onCloseClicked(v: View?) {
        listener!!.onCloseClicked()
    }

    private val searchQueryListener: SearchView.OnQueryTextListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(s: String): Boolean {
            return false
        }

        override fun onQueryTextChange(s: String): Boolean {
            if (s.length <= 0) {
                findInPageMatch!!.visibility = View.GONE
                setFindInPageChevronsEnabled(false)
            }
            listener!!.onSearchTextChanged(s)
            return true
        }
    }

    fun setFindInPageChevronsEnabled(enabled: Boolean) {
        findInPageNext!!.isEnabled = enabled
        findInPagePrev!!.isEnabled = enabled
        findInPageNext!!.alpha = if (enabled) 1.0f else 0.5f
        findInPagePrev!!.alpha = if (enabled) 1.0f else 0.5f
    }
}