package org.wikipedia.views

import android.content.Context
import android.graphics.Color
import android.view.ActionProvider
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.view.WindowInsetsCompat
import org.wikipedia.R
import org.wikipedia.databinding.GroupFindInPageBinding
import org.wikipedia.ktx.windowInsetsControllerCompat
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

    private var _binding: GroupFindInPageBinding? = null
    private val binding get() = _binding!!

    private var lastOccurrenceSearchFlag = false
    private var isFirstOccurrence = false
    private var isLastOccurrence = false

    var listener: FindInPageListener? = null
    var enableLastOccurrenceSearchFlag = false

    override fun overridesItemVisibility(): Boolean {
        return true
    }

    override fun onCreateActionView(): View {
        _binding = GroupFindInPageBinding.inflate(LayoutInflater.from(context))

        setFindInPageChevronsEnabled(false)
        setInputFieldStyle()
        setButtonClickListeners()
        setButtonLongClickListeners()
        DeviceUtil.setContextClickAsLongClick(binding.findInPageNext, binding.findInPagePrev)
        return binding.root
    }

    private fun setInputFieldStyle() {
        binding.findInPageInput.queryHint = context.getString(R.string.menu_page_find_in_page)
        binding.findInPageInput.isFocusable = true
        binding.findInPageInput.setOnQueryTextListener(searchQueryListener)
        binding.findInPageInput.isIconified = false
        binding.findInPageInput.maxWidth = Int.MAX_VALUE
        binding.findInPageInput.inputType = EditorInfo.TYPE_CLASS_TEXT
        binding.findInPageInput.isSubmitButtonEnabled = false
        // remove focus line from search plate
        val searchEditPlate = binding.findInPageInput.findViewById<View>(androidx.appcompat.R.id.search_plate)
        searchEditPlate.setBackgroundColor(Color.TRANSPARENT)
        // remove the close icon in search view
        val searchCloseButton = binding.findInPageInput.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)
        searchCloseButton.isEnabled = false
        searchCloseButton.setImageDrawable(null)
    }

    private fun setButtonClickListeners() {
        binding.findInPagePrev.setOnClickListener {
            it.windowInsetsControllerCompat?.hide(WindowInsetsCompat.Type.ime())
            listener?.onFindPrevClicked()
        }
        binding.findInPageNext.setOnClickListener {
            it.windowInsetsControllerCompat?.hide(WindowInsetsCompat.Type.ime())
            listener?.onFindNextClicked()
        }
        binding.closeButton.setOnClickListener {
            listener?.onCloseClicked()
        }
    }

    private fun setButtonLongClickListeners() {
        binding.findInPagePrev.setOnLongClickListener {
            if (isFirstOccurrence) {
                Toast.makeText(context, context.getString(R.string.find_first_occurence), Toast.LENGTH_SHORT).show()
            } else {
                it.windowInsetsControllerCompat?.hide(WindowInsetsCompat.Type.ime())
                listener?.onFindPrevLongClicked()
            }
            true
        }
        binding.findInPageNext.setOnLongClickListener {
            if (isLastOccurrence) {
                Toast.makeText(context, context.getString(R.string.find_last_occurence), Toast.LENGTH_SHORT).show()
            } else {
                it.windowInsetsControllerCompat?.hide(WindowInsetsCompat.Type.ime())
                listener?.onFindNextLongClicked()
                lastOccurrenceSearchFlag = true
            }
            true
        }
    }

    fun setSearchViewQuery(searchQuery: String?) {
        binding.findInPageInput.setQuery(searchQuery, true)
    }

    fun setMatchesResults(activeMatchOrdinal: Int, numberOfMatches: Int) {
        if (numberOfMatches > 0) {
            binding.findInPageMatch.text = context.getString(R.string.find_in_page_result,
                    activeMatchOrdinal + 1, numberOfMatches)
            binding.findInPageMatch.setTextColor(ResourceUtil.getThemedColor(context, R.attr.material_theme_de_emphasised_color))
            setFindInPageChevronsEnabled(true)
            isFirstOccurrence = activeMatchOrdinal == 0
            isLastOccurrence = activeMatchOrdinal + 1 == numberOfMatches
        } else {
            binding.findInPageMatch.text = "0/0"
            binding.findInPageMatch.setTextColor(ResourceUtil.getThemedColor(context, R.attr.colorError))
            setFindInPageChevronsEnabled(false)
            isFirstOccurrence = false
            isLastOccurrence = false
        }
        if (enableLastOccurrenceSearchFlag && lastOccurrenceSearchFlag) {
            // Go one occurrence back from the first one so it shows the last one.
            lastOccurrenceSearchFlag = false
            listener?.onFindPrevClicked()
        }
        binding.findInPageMatch.visibility = View.VISIBLE
    }

    private val searchQueryListener: SearchView.OnQueryTextListener = object : SearchView.OnQueryTextListener {
        override fun onQueryTextSubmit(text: String): Boolean {
            return false
        }

        override fun onQueryTextChange(text: String): Boolean {
            if (text.isEmpty()) {
                binding.findInPageMatch.visibility = View.GONE
                setFindInPageChevronsEnabled(false)
            }
            listener?.onSearchTextChanged(text)
            return true
        }
    }

    private fun setFindInPageChevronsEnabled(enabled: Boolean) {
        binding.findInPageNext.isEnabled = enabled
        binding.findInPagePrev.isEnabled = enabled
        binding.findInPageNext.alpha = if (enabled) 1.0f else 0.5f
        binding.findInPagePrev.alpha = if (enabled) 1.0f else 0.5f
    }
}
