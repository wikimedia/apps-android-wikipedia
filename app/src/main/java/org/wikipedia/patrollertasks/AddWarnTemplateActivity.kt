package org.wikipedia.patrollertasks

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextWatcher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.auth.AccountUtil
import org.wikipedia.commons.FilePageActivity
import org.wikipedia.databinding.ActivityAddWarnTemplateBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.history.HistoryEntry
import org.wikipedia.login.LoginActivity
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.LinkHandler
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.readinglist.AddToReadingListDialog
import org.wikipedia.talk.UserTalkPopupHelper
import org.wikipedia.util.ClipboardUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.ShareUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.views.UserMentionInputView

class AddWarnTemplateActivity : BaseActivity(), LinkPreviewDialog.Callback, UserMentionInputView.Listener {
    private lateinit var binding: ActivityAddWarnTemplateBinding
    private lateinit var textWatcher: TextWatcher

    private val viewModel: AddWarnTemplateViewModel by viewModels()
    private var userMentionScrolled = false
    private var savedSuccess = false

    private val wikiSite = WikiSite(WikipediaApp.instance.appOrSystemLanguageCode)

    private val requestLogin = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == LoginActivity.RESULT_LOGIN_SUCCESS) {
            updateEditLicenseText()
            FeedbackUtil.showMessage(this, R.string.login_success_toast)
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddWarnTemplateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.addWarnTemplateToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.patroller_warn_templates_new_message_title)

        textWatcher = binding.addWarnTemplateSubjectText.doOnTextChanged { _, _, _, _ ->
            binding.addWarnTemplateSubjectLayout.error = null
            binding.addWarnTemplateInputView.textInputLayout.error = null
            setSaveButtonEnabled(!binding.addWarnTemplateInputView.editText.text.isNullOrBlank())
        }
        binding.addWarnTemplateInputView.editText.addTextChangedListener(textWatcher)

        binding.addWarnTemplateSaveButton.setOnClickListener {
            onSaveClicked()
        }

        binding.addWarnTemplateInputView.wikiSite = wikiSite
        binding.addWarnTemplateInputView.listener = this

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collect {
                    when (it) {
                        // TODO: implement this
                    }
                }
            }
        }
    }

    public override fun onDestroy() {
        binding.addWarnTemplateSubjectText.removeTextChangedListener(textWatcher)
        binding.addWarnTemplateInputView.editText.removeTextChangedListener(textWatcher)
        super.onDestroy()
    }

    private fun onInitialLoad() {
        updateEditLicenseText()
        setSaveButtonEnabled(false)
        L10nUtil.setConditionalLayoutDirection(binding.addWarnTemplateScrollContainer, wikiSite.languageCode)
        binding.addWarnTemplateInputView.textInputLayout.hint = getString(R.string.talk_message_hint)
        binding.addWarnTemplateSubjectLayout.isVisible = true
        binding.addWarnTemplateSubjectLayout.requestFocus()
    }

    internal inner class TalkLinkHandler internal constructor(context: Context) : LinkHandler(context) {
        private var lastX: Int = 0
        private var lastY: Int = 0

        fun onUrlClick(url: String, title: String?, linkText: String, x: Int, y: Int) {
            lastX = x
            lastY = y
            super.onUrlClick(url, title, linkText)
        }

        override fun onMediaLinkClicked(title: PageTitle) {
            startActivity(FilePageActivity.newIntent(this@AddWarnTemplateActivity, title))
        }

        override fun onDiffLinkClicked(title: PageTitle, revisionId: Long) {
            // TODO
        }

        override lateinit var wikiSite: WikiSite

        override fun onPageLinkClicked(anchor: String, linkText: String) {
            // TODO
        }

        override fun onInternalLinkClicked(title: PageTitle) {
            UserTalkPopupHelper.show(this@AddWarnTemplateActivity, title, false, lastX, lastY,
                    Constants.InvokeSource.TALK_REPLY_ACTIVITY, HistoryEntry.SOURCE_TALK_TOPIC)
        }
    }

    private fun setSaveButtonEnabled(enabled: Boolean) {
        binding.addWarnTemplateSaveButton.isEnabled = enabled
        binding.addWarnTemplateSaveButton.setTextColor(ResourceUtil
            .getThemedColor(this, if (enabled) R.attr.progressive_color else R.attr.placeholder_color))
    }

    private fun onSaveClicked() {
        // TODO: show dialog of adding title

        val subject = binding.addWarnTemplateSubjectText.text.toString().trim()
        val body = binding.addWarnTemplateInputView.editText.getParsedText(wikiSite).trim()

        if (subject.isEmpty()) {
            binding.addWarnTemplateSubjectLayout.error = getString(R.string.talk_subject_empty)
            binding.addWarnTemplateSubjectLayout.requestFocus()
            return
        } else if (body.isEmpty()) {
            binding.addWarnTemplateInputView.textInputLayout.error = getString(R.string.talk_message_empty)
            binding.addWarnTemplateInputView.textInputLayout.requestFocus()
            return
        }

        setSaveButtonEnabled(false)

        // TODO: implement this
        // viewModel.saveTemplate(subject, body)
    }

    private fun onSaveSuccess() {
        setSaveButtonEnabled(true)
        finish()
    }

    private fun onSaveError(t: Throwable) {
        setSaveButtonEnabled(true)
        FeedbackUtil.showError(this, t)
    }

    private fun updateEditLicenseText() {
        binding.licenseText.text = StringUtil.fromHtml(getString(if (AccountUtil.isLoggedIn) R.string.edit_save_action_license_logged_in else R.string.edit_save_action_license_anon,
                getString(R.string.terms_of_use_url),
                getString(R.string.cc_by_sa_4_url)))
        binding.licenseText.movementMethod = LinkMovementMethodExt { url: String ->
            if (url == "https://#login") {
                val loginIntent = LoginActivity.newIntent(this, LoginActivity.SOURCE_EDIT)
                requestLogin.launch(loginIntent)
            } else {
                UriUtil.handleExternalLink(this, Uri.parse(url))
            }
        }
    }

    override fun onLinkPreviewLoadPage(title: PageTitle, entry: HistoryEntry, inNewTab: Boolean) {
        startActivity(if (inNewTab) PageActivity.newIntentForNewTab(this, entry, title) else
            PageActivity.newIntentForCurrentTab(this, entry, title, false))
    }

    override fun onLinkPreviewCopyLink(title: PageTitle) {
        ClipboardUtil.setPlainText(this, text = title.uri)
        FeedbackUtil.showMessage(this, R.string.address_copied)
    }

    override fun onLinkPreviewAddToList(title: PageTitle) {
        ExclusiveBottomSheetPresenter.show(supportFragmentManager,
                AddToReadingListDialog.newInstance(title, Constants.InvokeSource.TALK_REPLY_ACTIVITY))
    }

    override fun onLinkPreviewShareLink(title: PageTitle) {
        ShareUtil.shareText(this, title)
    }

    override fun onBackPressed() {
        if (!binding.addWarnTemplateSubjectText.text.isNullOrEmpty() || !binding.addWarnTemplateInputView.editText.text.isNullOrEmpty()) {
            MaterialAlertDialogBuilder(this)
                .setCancelable(false)
                .setTitle(R.string.talk_new_topic_exit_dialog_title)
                .setMessage(R.string.talk_new_topic_exit_dialog_message)
                .setPositiveButton(R.string.edit_abandon_confirm_yes) { _, _ -> super.onBackPressed() }
                .setNegativeButton(R.string.edit_abandon_confirm_no, null)
                .show()
        } else {
            super.onBackPressed()
        }
    }

    override fun onUserMentionListUpdate() {
        binding.licenseText.isVisible = false
        binding.addWarnTemplateScrollContainer.post {
            if (!isDestroyed && !userMentionScrolled) {
                binding.addWarnTemplateScrollContainer.smoothScrollTo(0, binding.root.height * 4)
                userMentionScrolled = true
            }
        }
    }

    override fun onUserMentionComplete() {
        userMentionScrolled = false
        binding.licenseText.isVisible = true
    }

    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, AddWarnTemplateActivity::class.java)
        }
    }
}
