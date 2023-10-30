package org.wikipedia.usercontrib

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.analytics.eventplatform.PatrollerExperienceEvent
import org.wikipedia.databinding.DialogUserInformationBinding
import org.wikipedia.suggestededits.SuggestedEditsRecentEditsActivity
import org.wikipedia.suggestededits.SuggestionsActivity
import org.wikipedia.util.DateUtil
import org.wikipedia.util.StringUtil
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

class UserInformationDialog : DialogFragment() {

    private val viewModel: UserInformationDialogViewModel by viewModels { UserInformationDialogViewModel.Factory(requireArguments()) }

    private var _binding: DialogUserInformationBinding? = null
    private val binding get() = _binding!!

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        _binding = DialogUserInformationBinding.inflate(layoutInflater)
        return MaterialAlertDialogBuilder(requireActivity())
            .setTitle(R.string.patroller_tasks_edits_list_user_information_dialog_title)
            .setView(binding.root)
            .setPositiveButton(R.string.patroller_tasks_edits_list_user_information_dialog_close) { _, _ ->
                dismiss()
            }.run {
                setupDialog()
                this
            }.create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupDialog() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collect {
                    when (it) {
                        is UserInformationDialogViewModel.UiState.Loading -> onLoading()
                        is UserInformationDialogViewModel.UiState.Success -> onSuccess(it.editCount, it.registrationDate)
                        is UserInformationDialogViewModel.UiState.Error -> onError(it.throwable)
                    }
                }
            }
        }
        binding.dialogErrorView.backClickListener = View.OnClickListener {
            dismiss()
        }
        binding.dialogErrorView.retryClickListener = View.OnClickListener {
            dismiss()
        }
    }

    private fun onLoading() {
        binding.userInformationContainer.isVisible = false
        binding.dialogProgressBar.isVisible = true
        binding.dialogErrorView.isVisible = false
    }

    private fun onSuccess(editCount: String, registrationDate: Date) {
        sendPatrollerExperienceEvent()
        binding.userInformationContainer.isVisible = true
        binding.dialogProgressBar.isVisible = false
        binding.dialogErrorView.isVisible = false
        val localDate = LocalDateTime.ofInstant(registrationDate.toInstant(), ZoneId.systemDefault()).toLocalDate()
        val dateStr = DateUtil.getShortDateString(localDate)
        binding.userTenure.text = StringUtil.fromHtml(getString(R.string.patroller_tasks_edits_list_user_information_dialog_joined_date_text, dateStr))
        binding.editCount.text = StringUtil.fromHtml(getString(R.string.patroller_tasks_edits_list_user_information_dialog_edit_count_text, editCount))
    }

    private fun sendPatrollerExperienceEvent() {
        var activity: Context? = context
        while (activity !is Activity && activity is ContextWrapper) {
            activity = activity.baseContext
        }
        activity?.let {
            PatrollerExperienceEvent.logAction("user_info_impression",
                when (it) {
                    is SuggestedEditsRecentEditsActivity -> "pt_recent_changes"
                    is SuggestionsActivity -> "pt_edit"
                    else -> ""
                }
            )
        }
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
