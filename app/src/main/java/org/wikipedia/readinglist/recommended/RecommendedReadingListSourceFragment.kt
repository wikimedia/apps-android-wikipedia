package org.wikipedia.readinglist.recommended

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.wikipedia.databinding.FragmentRecommendedReadingListSourceBinding

class RecommendedReadingListSourceFragment : Fragment() {
    private var _binding: FragmentRecommendedReadingListSourceBinding? = null

    private val binding get() = _binding!!


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentRecommendedReadingListSourceBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(): RecommendedReadingListSourceFragment {
            return RecommendedReadingListSourceFragment()
        }
    }
}
