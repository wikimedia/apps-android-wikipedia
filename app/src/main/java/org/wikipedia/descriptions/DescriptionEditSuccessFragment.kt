package org.wikipedia.descriptions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.wikipedia.activity.FragmentUtil.getCallback
import org.wikipedia.databinding.FragmentDescriptionEditSuccessBinding

class DescriptionEditSuccessFragment : Fragment(), DescriptionEditSuccessView.Callback {
    private var _binding: FragmentDescriptionEditSuccessBinding? = null
    private val binding get() = _binding!!

    interface Callback {
        fun onDismissClick()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)
        _binding = FragmentDescriptionEditSuccessBinding.inflate(inflater, container, false)
        binding.fragmentDescriptionEditSuccessView.callback = this
        return binding.root
    }

    override fun onDismissClick() {
        callback()?.onDismissClick()
    }

    override fun onDestroyView() {
        binding.fragmentDescriptionEditSuccessView.callback = null
        _binding = null
        super.onDestroyView()
    }

    private fun callback(): Callback? {
        return getCallback(this, Callback::class.java)
    }

    companion object {
        fun newInstance(): DescriptionEditSuccessFragment {
            return DescriptionEditSuccessFragment()
        }
    }
}
