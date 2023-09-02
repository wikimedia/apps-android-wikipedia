package org.wikipedia.usercontrib

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.databinding.DialogUserInformationBinding
import org.wikipedia.util.StringUtil

class UserInformationDialog : DialogFragment() {

    private val viewModel: UserInformationDialogViewModel by viewModels { UserInformationDialogViewModel.Factory(requireArguments()) }

    private var _binding: DialogUserInformationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogUserInformationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collect {
                    when (it) {
                        is UserInformationDialogViewModel.UiState.Loading -> onLoading()
                        is UserInformationDialogViewModel.UiState.Success -> onSuccess(it.editCount, it.diffDays)
                        is UserInformationDialogViewModel.UiState.Error -> onError(it.throwable)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun onLoading() {
        binding.userInformationContainer.isVisible = false
        binding.dialogProgressBar.isVisible = true
        binding.dialogErrorView.isVisible = false
    }

    private fun onSuccess(editCount: String, diffDays: String) {
        binding.userInformationContainer.isVisible = true
        binding.dialogProgressBar.isVisible = false
        binding.dialogErrorView.isVisible = false
        binding.userTenure.text = StringUtil.fromHtml(getString(R.string.patroller_tasks_edits_list_user_information_dialog_tenure_v2, diffDays))
        binding.editCount.text = StringUtil.fromHtml(getString(R.string.patroller_tasks_edits_list_user_information_dialog_edit_count_v2, editCount))
    }

    private fun onError(t: Throwable) {
        binding.userInformationContainer.isVisible = false
        binding.dialogProgressBar.isVisible = false
        binding.dialogErrorView.setError(t)
        binding.dialogErrorView.isVisible = true
    }

    companion object {

        const val USERNAME_ARG = "username"

        fun newInstance(username: String): UserInformationDialog {
            val dialog = UserInformationDialog()
            dialog.arguments = bundleOf(USERNAME_ARG to username)
            return dialog
        }
    }
}
