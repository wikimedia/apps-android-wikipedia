package org.wikipedia.edit.templates

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import org.wikipedia.databinding.FragmentInsertTemplateBinding
import org.wikipedia.dataclient.mwapi.TemplateDataResponse
import org.wikipedia.page.PageTitle
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil

class InsertTemplateFragment : Fragment() {

    private lateinit var activity: TemplatesSearchActivity
    private var _binding: FragmentInsertTemplateBinding? = null
    private val binding get() = _binding!!
    private val viewModel get() = activity.viewModel

    val isActive get() = binding.root.visibility == View.VISIBLE

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentInsertTemplateBinding.inflate(layoutInflater, container, false)
        activity = (requireActivity() as TemplatesSearchActivity)
        activity.supportActionBar?.title = null

        // TODO: check all required params
//        binding.mediaAlternativeText.addTextChangedListener {
//            if (!activity.isDestroyed) {
//                activity.invalidateOptionsMenu()
//            }
//        }
        return binding.root
    }

    private fun buildParamsInputFields() {
//        binding.templateParamsContainer.addView()
    }

    fun show(pageTitle: PageTitle, templateData: TemplateDataResponse.TemplateData) {
        binding.root.isVisible = true
        binding.templateDataTitle.text = StringUtil.removeNamespace(pageTitle.displayText)
        binding.templateDataDescription.text = pageTitle.description
        binding.templateDataDescription.isVisible = !pageTitle.description.isNullOrEmpty()
        binding.templateDataLearnMoreButton.setOnClickListener {
            UriUtil.visitInExternalBrowser(requireContext(), Uri.parse(pageTitle.uri))
        }
    }

    fun hide() {
        binding.root.isVisible = false
        activity.invalidateOptionsMenu()
    }

    fun handleBackPressed(): Boolean {
        if (isActive) {
            hide()
            return true
        }
        return false
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
