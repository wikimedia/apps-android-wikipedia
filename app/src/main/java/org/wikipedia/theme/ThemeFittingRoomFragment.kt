package org.wikipedia.theme

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.concurrency.FlowEventBus
import org.wikipedia.databinding.FragmentThemeFittingRoomBinding
import org.wikipedia.events.ChangeTextSizeEvent
import org.wikipedia.events.WebViewInvalidateEvent
import org.wikipedia.settings.Prefs

class ThemeFittingRoomFragment : Fragment() {
    private var _binding: FragmentThemeFittingRoomBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentThemeFittingRoomBinding.inflate(inflater, container, false)
        binding.themeTestImage.loadImage(R.drawable.w_nav_mark)
        updateTextSize()
        updateFontFamily()

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                FlowEventBus.events.collectLatest { event ->
                    if (event is ChangeTextSizeEvent) {
                        updateTextSize()
                        binding.themeTestText.post { FlowEventBus.post(WebViewInvalidateEvent()) }
                    }
                }
            }
        }

        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun updateTextSize() {
        val titleMultiplier = 1.6f
        val fontSize = WikipediaApp.instance.getFontSize(requireActivity().window)
        binding.themeTestText.textSize = fontSize
        binding.themeTestTitle.textSize = fontSize * titleMultiplier
    }

    private fun updateFontFamily() {
        val currentTypeface = if (Prefs.fontFamily == resources.getString(R.string.font_family_sans_serif)) Typeface.SANS_SERIF else Typeface.SERIF
        binding.themeTestTitle.typeface = currentTypeface
        binding.themeTestText.typeface = currentTypeface
    }

    companion object {
        fun newInstance(): ThemeFittingRoomFragment {
            return ThemeFittingRoomFragment()
        }
    }
}
