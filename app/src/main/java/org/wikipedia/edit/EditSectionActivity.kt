package org.wikipedia.edit

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.text.TextWatcher
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.core.os.postDelayed
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.BreadCrumbLogEvent
import org.wikipedia.analytics.eventplatform.EditAttemptStepEvent
import org.wikipedia.analytics.eventplatform.ImageRecommendationsEvent
import org.wikipedia.auth.AccountUtil
import org.wikipedia.captcha.CaptchaHandler
import org.wikipedia.captcha.CaptchaResult
import org.wikipedia.databinding.ActivityEditSectionBinding
import org.wikipedia.databinding.DialogWithCheckboxBinding
import org.wikipedia.databinding.ItemEditActionbarButtonBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.dataclient.mwapi.MwServiceError
import org.wikipedia.edit.insertmedia.InsertMediaActivity
import org.wikipedia.edit.insertmedia.InsertMediaViewModel
import org.wikipedia.edit.preview.EditPreviewFragment
import org.wikipedia.edit.richtext.SyntaxHighlighter
import org.wikipedia.edit.summaries.EditSummaryFragment
import org.wikipedia.extensions.parcelableExtra
import org.wikipedia.history.HistoryEntry
import org.wikipedia.login.LoginActivity
import org.wikipedia.notifications.AnonymousNotificationHelper
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.page.Namespace
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.settings.Prefs
import org.wikipedia.theme.ThemeChooserDialog
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.EditNoticesDialog
import org.wikipedia.views.ViewUtil
import java.io.IOException
import java.util.concurrent.TimeUnit

class EditSectionActivity : BaseActivity(), ThemeChooserDialog.Callback, EditPreviewFragment.Callback, LinkPreviewDialog.LoadPageCallback, LinkPreviewDialog.DismissCallback {
    private val viewModel: EditSectionViewModel by viewModels { EditSectionViewModel.Factory(intent.extras!!) }

    private lateinit var binding: ActivityEditSectionBinding
    private lateinit var textWatcher: TextWatcher
    private lateinit var captchaHandler: CaptchaHandler
    private lateinit var editPreviewFragment: EditPreviewFragment
    private lateinit var editSummaryFragment: EditSummaryFragment
    private lateinit var syntaxHighlighter: SyntaxHighlighter

    private var sectionTextModified = false
    private var sectionTextFirstLoad = true

    private var actionMode: ActionMode? = null

    private val movementMethodWithLogin = LinkMovementMethodExt { url: String ->
        if (url == "https://#login") {
            val loginIntent = LoginActivity.newIntent(this@EditSectionActivity, LoginActivity.SOURCE_EDIT)
            requestLogin.launch(loginIntent)
        } else {
            UriUtil.handleExternalLink(this@EditSectionActivity, url.toUri())
        }
    }

    private val requestLogin = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == LoginActivity.RESULT_LOGIN_SUCCESS) {
            updateEditLicenseText()
            invalidateOptionsMenu()
            FeedbackUtil.showMessage(this, R.string.login_success_toast)
        }
    }

    private val requestInsertMedia = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == InsertMediaActivity.RESULT_INSERT_MEDIA_SUCCESS) {
            it.data?.let { data ->

                // pass the resulting data into our own current intent, so that we can pass it back
                // to the InsertImage workflow if the user navigates back to it.
                val imageTitle = data.parcelableExtra<PageTitle>(InsertMediaActivity.EXTRA_IMAGE_TITLE)
                val imageCaption = data.getStringExtra(InsertMediaActivity.RESULT_IMAGE_CAPTION)
                val imageAlt = data.getStringExtra(InsertMediaActivity.RESULT_IMAGE_ALT)
                val imageSize = data.getStringExtra(InsertMediaActivity.RESULT_IMAGE_SIZE)
                val imageType = data.getStringExtra(InsertMediaActivity.RESULT_IMAGE_TYPE)
                val imagePos = data.getStringExtra(InsertMediaActivity.RESULT_IMAGE_POS)

                intent.putExtra(InsertMediaActivity.EXTRA_IMAGE_TITLE, imageTitle)
                intent.putExtra(InsertMediaActivity.RESULT_IMAGE_CAPTION, imageCaption)
                intent.putExtra(InsertMediaActivity.RESULT_IMAGE_ALT, imageAlt)
                intent.putExtra(InsertMediaActivity.RESULT_IMAGE_SIZE, imageSize)
                intent.putExtra(InsertMediaActivity.RESULT_IMAGE_TYPE, imageType)
                intent.putExtra(InsertMediaActivity.RESULT_IMAGE_POS, imagePos)

                val newWikiText = InsertMediaViewModel.insertImageIntoWikiText(viewModel.pageTitle.wikiSite.languageCode,
                    viewModel.sectionWikitext.orEmpty(), imageTitle?.text.orEmpty(), imageCaption.orEmpty(),
                    imageAlt.orEmpty(), imageSize.orEmpty(), imageType.orEmpty(), imagePos.orEmpty(),
                    if (viewModel.invokeSource == Constants.InvokeSource.EDIT_ADD_IMAGE) 0 else binding.editSectionText.selectionStart,
                    viewModel.invokeSource == Constants.InvokeSource.EDIT_ADD_IMAGE,
                    intent.getBooleanExtra(InsertMediaActivity.EXTRA_ATTEMPT_INSERT_INTO_INFOBOX, false))

                binding.editSectionText.setText(newWikiText.first)
                intent.putExtra(InsertMediaActivity.EXTRA_INSERTED_INTO_INFOBOX, newWikiText.second)

                val insertPos = newWikiText.third
                binding.editSectionText.setSelection(insertPos.first, insertPos.first + insertPos.second)

                if (viewModel.invokeSource == Constants.InvokeSource.EDIT_ADD_IMAGE) {
                    // If we came from the Image Recommendation workflow, go directly to Preview.
                    clickNextButton()
                }
            }
        } else if (viewModel.invokeSource == Constants.InvokeSource.EDIT_ADD_IMAGE) {
            // If the user cancels image insertion, back out immediately.
            finish()
        }
    }

    private val movementMethod = LinkMovementMethodExt { urlStr ->
        UriUtil.visitInExternalBrowser(this, Uri.parse(UriUtil.resolveProtocolRelativeUrl(viewModel.pageTitle.wikiSite, urlStr)))
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditSectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setNavigationBarColor(ResourceUtil.getThemedColor(this, android.R.attr.colorBackground))

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = ""

        syntaxHighlighter = SyntaxHighlighter(this, binding.editSectionText, binding.editSectionScroll)
        binding.editSectionScroll.isSmoothScrollingEnabled = false
        captchaHandler = CaptchaHandler(this, viewModel.pageTitle.wikiSite, binding.captchaContainer.root,
                binding.editSectionText, "", null)
        editPreviewFragment = supportFragmentManager.findFragmentById(R.id.edit_section_preview_fragment) as EditPreviewFragment
        editSummaryFragment = supportFragmentManager.findFragmentById(R.id.edit_section_summary_fragment) as EditSummaryFragment
        editSummaryFragment.title = viewModel.pageTitle

        // Only send the editing start log event if the activity is created for the first time
        if (savedInstanceState == null) {
            EditAttemptStepEvent.logInit(viewModel.pageTitle)
        }
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(EXTRA_KEY_TEMPORARY_WIKITEXT_STORED)) {
                viewModel.sectionWikitext = Prefs.temporaryWikitext
            }
            viewModel.editingAllowed = savedInstanceState.getBoolean(EXTRA_KEY_EDITING_ALLOWED, false)
            sectionTextModified = savedInstanceState.getBoolean(EXTRA_KEY_SECTION_TEXT_MODIFIED, false)
        }
        L10nUtil.setConditionalTextDirection(binding.editSectionText, viewModel.pageTitle.wikiSite.languageCode)

        fetchSectionText()

        binding.viewEditSectionError.retryClickListener = View.OnClickListener {
            binding.viewEditSectionError.visibility = View.GONE
            captchaHandler.requestNewCaptcha()
            fetchSectionText()
        }
        binding.viewEditSectionError.backClickListener = View.OnClickListener {
            onBackPressed()
        }

        textWatcher = binding.editSectionText.doAfterTextChanged {
            if (sectionTextFirstLoad) {
                sectionTextFirstLoad = false
                return@doAfterTextChanged
            }
            if (!sectionTextModified) {
                sectionTextModified = true
                // update the actionbar menu, which will enable the Next button.
                invalidateOptionsMenu()
            }
        }

        SyntaxHighlightViewAdapter(this, viewModel.pageTitle, binding.root, binding.editSectionText,
            binding.editKeyboardOverlay, binding.editKeyboardOverlayFormatting, binding.editKeyboardOverlayHeadings,
            Constants.InvokeSource.EDIT_ACTIVITY, requestInsertMedia)

        binding.editSectionText.setOnClickListener { finishActionMode() }
        onEditingPrefsChanged()

        if (viewModel.invokeSource == Constants.InvokeSource.EDIT_ADD_IMAGE) {
            // If the intent is to add an image to the article, go directly to the image insertion flow.
            startInsertImageFlow()
        }

        // set focus to the EditText, but keep the keyboard hidden until the user changes the cursor location:
        binding.editSectionText.requestFocus()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.fetchSectionTextState.collectLatest {
                        when (it) {
                            is Resource.Loading -> {
                                showProgressBar(true)
                            }
                            is Resource.Success -> {
                                showProgressBar(false)
                                displaySectionText()
                                maybeShowEditSourceDialog()
                                invalidateOptionsMenu()
                                if (!maybeShowTempAccountDialog()) {
                                    if (Prefs.autoShowEditNotices) {
                                        showEditNotices()
                                    } else {
                                        maybeShowEditNoticesTooltip()
                                    }
                                }
                                it.data?.let { error ->
                                    FeedbackUtil.showError(this@EditSectionActivity, MwException(error), viewModel.pageTitle.wikiSite)
                                }
                            }
                            is Resource.Error -> {
                                showProgressBar(false)
                                showError(it.throwable)
                            }
                        }
                    }
                }

                launch {
                    viewModel.postEditState.collectLatest {
                        when (it) {
                            is Resource.Loading -> {
                                showProgressBar(true)
                                binding.editSectionCaptchaContainer.visibility = View.GONE
                                captchaHandler.hideCaptcha()
                                editSummaryFragment.saveSummary()
                            }
                            is Resource.Success -> {
                                it.data.edit?.run {
                                    when {
                                        editSucceeded -> {
                                            AnonymousNotificationHelper.onEditSubmitted()
                                            viewModel.waitForRevisionUpdate(newRevId)
                                        }
                                        hasCaptchaResponse -> onEditSuccess(CaptchaResult(captchaId))
                                        hasSpamBlacklistResponse -> onEditFailure(MwException(MwServiceError(code, spamblacklist)))
                                        hasEditErrorCode -> onEditFailure(MwException(MwServiceError(code, info)))
                                        else -> onEditFailure(IOException("Received unrecognized edit response"))
                                    }
                                } ?: run {
                                    onEditFailure(IOException("An unknown error occurred."))
                                }
                            }
                            is Resource.Error -> {
                                onEditFailure(it.throwable)
                            }
                        }
                    }
                }

                launch {
                    viewModel.waitForRevisionState.collect {
                        when (it) {
                            is Resource.Success -> {
                                onEditSuccess(EditSuccessResult(it.data))
                            }
                        }
                    }
                }
            }
        }
    }

    public override fun onStart() {
        super.onStart()
        updateEditLicenseText()
    }

    public override fun onDestroy() {
        captchaHandler.dispose()
        binding.editSectionText.removeTextChangedListener(textWatcher)
        syntaxHighlighter.cleanup()
        super.onDestroy()
    }

    public override fun onPause() {
        super.onPause()
        viewModel.sectionWikitext = binding.editSectionText.text.toString()
    }

    fun getInvokeSource(): Constants.InvokeSource {
        return viewModel.invokeSource
    }

    private fun updateEditLicenseText() {
        val editLicenseText = ActivityCompat.requireViewById<TextView>(this, R.id.licenseText)
        editLicenseText.text = StringUtil.fromHtml(getString(R.string.edit_save_action_license_logged_in,
                getString(R.string.terms_of_use_url),
                getString(R.string.cc_by_sa_4_url)))
        editLicenseText.movementMethod = movementMethodWithLogin
    }

    private fun doSave() {
        val sectionAnchor = StringUtil.addUnderscores(StringUtil.removeHTMLTags(viewModel.sectionAnchor.orEmpty()))
        val isMinorEdit = if (editSummaryFragment.isMinorEdit) true else null
        val watchThisPage = if (editSummaryFragment.watchThisPage) "watch" else "unwatch"
        var summaryText = if (sectionAnchor.isEmpty() || sectionAnchor == viewModel.pageTitle.prefixedText) {
            if (viewModel.pageTitle.wikiSite.languageCode == "en") "/* top */" else ""
        } else "/* ${StringUtil.removeUnderscores(sectionAnchor)} */ "
         summaryText += editSummaryFragment.summary

        // Summaries are plaintext, so remove any HTML that's made its way into the summary
        summaryText = StringUtil.removeHTMLTags(summaryText)
        if (!isFinishing) {
            showProgressBar(true)
        }

        viewModel.postEdit(
            isMinorEdit = isMinorEdit,
            watchThisPage = watchThisPage,
            summaryText = summaryText,
            editSectionText = binding.editSectionText.text.toString(),
            captchaId = captchaHandler.captchaId().toString(),
            captchaWord = captchaHandler.captchaWord().toString(),
            editTags = getEditTag()
        )

        BreadCrumbLogEvent.logInputField(this, editSummaryFragment.summaryText)
    }

    private fun getEditTag(): String {
        return when {
            viewModel.invokeSource == Constants.InvokeSource.TALK_TOPIC_ACTIVITY -> EditTags.APP_TALK_SOURCE
            viewModel.invokeSource == Constants.InvokeSource.EDIT_ADD_IMAGE -> if (intent.getBooleanExtra(InsertMediaActivity.EXTRA_INSERTED_INTO_INFOBOX, false)) EditTags.APP_IMAGE_ADD_INFOBOX else EditTags.APP_IMAGE_ADD_TOP
            !viewModel.textToHighlight.isNullOrEmpty() -> EditTags.APP_SELECT_SOURCE
            viewModel.sectionID >= 0 -> EditTags.APP_SECTION_SOURCE
            else -> EditTags.APP_FULL_SOURCE
        }
    }

    private fun onEditSuccess(result: EditResult) {
        if (result is EditSuccessResult) {
            EditAttemptStepEvent.logSaveSuccess(viewModel.pageTitle)
            // TODO: remove the artificial delay and use the new revision
            // ID returned to request the updated version of the page once
            // revision support for mobile-sections is added to RESTBase
            // See https://github.com/wikimedia/restbase/pull/729
            Handler(mainLooper).postDelayed(TimeUnit.SECONDS.toMillis(2)) {
                showProgressBar(false)

                // Build intent that includes the section we were editing, so we can scroll to it later
                val data = Intent()
                data.putExtra(EXTRA_SECTION_ID, viewModel.sectionID)
                data.putExtra(EXTRA_REV_ID, result.revID)
                setResult(EditHandler.RESULT_REFRESH_PAGE, data)
                DeviceUtil.hideSoftKeyboard(this@EditSectionActivity)
                finish()
            }
            return
        }
        showProgressBar(false)
        if (result is CaptchaResult) {
            binding.editSectionCaptchaContainer.visibility = View.VISIBLE
            captchaHandler.handleCaptcha(null, result)
        } else {
            EditAttemptStepEvent.logSaveFailure(viewModel.pageTitle)
            // Expand to do everything.
            onEditFailure(Throwable())
        }
    }

    private fun onEditFailure(caught: Throwable) {
        showProgressBar(false)
        if (caught is MwException) {
            handleEditingException(caught)
        } else {
            showRetryDialog(caught)
        }
        L.e(caught)
    }

    private fun showRetryDialog(t: Throwable) {
        MaterialAlertDialogBuilder(this@EditSectionActivity)
                .setTitle(R.string.dialog_message_edit_failed)
                .setMessage(t.localizedMessage)
                .setPositiveButton(R.string.dialog_message_edit_failed_retry) { dialog, _ ->
                    doSave()
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.dialog_message_edit_failed_cancel) { dialog, _ -> dialog.dismiss() }.show()
    }

    /**
     * Processes API error codes encountered during editing, and handles them as appropriate.
     * @param caught The MwException to handle.
     */
    private fun handleEditingException(caught: MwException) {
        val code = caught.title
        lifecycleScope.launch(CoroutineExceptionHandler { _, t ->
            showError(t)
        }) {
            // In the case of certain AbuseFilter responses, they are sent as a code, instead of a
            // fully parsed response. We need to make one more API call to get the parsed message:
            if (code.startsWith("abusefilter-") && caught.message.contains("abusefilter-") && caught.message.length < 100) {
                val response = ServiceFactory.get(viewModel.pageTitle.wikiSite).parsePage("MediaWiki:" + StringUtil.sanitizeAbuseFilterCode(caught.message))
                showError(MwException(MwServiceError(code, response.text)))
            } else if ("editconflict" == code) {
                MaterialAlertDialogBuilder(this@EditSectionActivity)
                    .setTitle(R.string.edit_conflict_title)
                    .setMessage(R.string.edit_conflict_message)
                    .setPositiveButton(R.string.edit_conflict_dialog_ok_button_text, null)
                    .show()
                resetToStart()
            } else {
                showError(caught)
            }
        }
    }

    /**
     * Executes a click of the actionbar button, and performs the appropriate action
     * based on the current state of the button.
     */
    fun clickNextButton() {
        val addImageTitle = intent.parcelableExtra<PageTitle>(InsertMediaActivity.EXTRA_IMAGE_TITLE)
        val addImageSource = intent.getStringExtra(InsertMediaActivity.EXTRA_IMAGE_SOURCE)
        val addImageSourceProjects = intent.getStringExtra(InsertMediaActivity.EXTRA_IMAGE_SOURCE_PROJECTS)
        when {
            editSummaryFragment.isActive -> {
                if (viewModel.invokeSource == Constants.InvokeSource.EDIT_ADD_IMAGE) {
                    ImageRecommendationsEvent.logAction("editsummary_save", "editsummary_dialog", ImageRecommendationsEvent.getActionDataString(
                        filename = addImageTitle?.prefixedText.orEmpty(), recommendationSource = addImageSource.orEmpty(), recommendationSourceProjects = addImageSourceProjects.orEmpty(),
                        acceptanceState = "accepted", captionAdd = !intent.getStringExtra(InsertMediaActivity.RESULT_IMAGE_CAPTION).isNullOrEmpty(),
                        altTextAdd = !intent.getStringExtra(InsertMediaActivity.RESULT_IMAGE_ALT).isNullOrEmpty()), addImageTitle?.wikiSite?.languageCode.orEmpty())
                }
                doSave()
                EditAttemptStepEvent.logSaveAttempt(viewModel.pageTitle)
                supportActionBar?.title = getString(R.string.preview_edit_summarize_edit_title)
            }
            editPreviewFragment.isActive -> {
                if (viewModel.invokeSource == Constants.InvokeSource.EDIT_ADD_IMAGE) {
                    ImageRecommendationsEvent.logAction("caption_preview_accept", "caption_preview", ImageRecommendationsEvent.getActionDataString(
                        filename = addImageTitle?.prefixedText.orEmpty(), recommendationSource = addImageSource.orEmpty(),
                        recommendationSourceProjects = addImageSourceProjects.orEmpty(), acceptanceState = "accepted",
                        captionAdd = !intent.getStringExtra(InsertMediaActivity.RESULT_IMAGE_CAPTION).isNullOrEmpty(),
                        altTextAdd = !intent.getStringExtra(InsertMediaActivity.RESULT_IMAGE_ALT).isNullOrEmpty()), viewModel.pageTitle.wikiSite.languageCode)
                    ImageRecommendationsEvent.logImpression("editsummary_dialog", ImageRecommendationsEvent.getActionDataString(
                        filename = addImageTitle?.prefixedText.orEmpty(), recommendationSource = addImageSource.orEmpty(),
                        recommendationSourceProjects = addImageSourceProjects.orEmpty(), acceptanceState = "accepted",
                        captionAdd = !intent.getStringExtra(InsertMediaActivity.RESULT_IMAGE_CAPTION).isNullOrEmpty(),
                        altTextAdd = !intent.getStringExtra(InsertMediaActivity.RESULT_IMAGE_ALT).isNullOrEmpty()), viewModel.pageTitle.wikiSite.languageCode)
                }
                editSummaryFragment.show()
                supportActionBar?.title = getString(R.string.preview_edit_summarize_edit_title)
            }
            else -> {
                // we must be showing the editing window, so show the Preview.
                DeviceUtil.hideSoftKeyboard(this)
                binding.editSectionContainer.isVisible = false
                editPreviewFragment.showPreview(viewModel.pageTitle, binding.editSectionText.text.toString())
                EditAttemptStepEvent.logSaveIntent(viewModel.pageTitle)
                supportActionBar?.title = getString(R.string.edit_preview)
                setNavigationBarColor(ResourceUtil.getThemedColor(this, R.attr.paper_color))
                if (viewModel.invokeSource == Constants.InvokeSource.EDIT_ADD_IMAGE) {
                    ImageRecommendationsEvent.logImpression("caption_preview", ImageRecommendationsEvent.getActionDataString(
                        filename = addImageTitle?.prefixedText.orEmpty(), recommendationSource = addImageSource.orEmpty(),
                        recommendationSourceProjects = addImageSourceProjects.orEmpty(), acceptanceState = "accepted",
                        captionAdd = !intent.getStringExtra(InsertMediaActivity.RESULT_IMAGE_CAPTION).isNullOrEmpty(),
                        altTextAdd = !intent.getStringExtra(InsertMediaActivity.RESULT_IMAGE_ALT).isNullOrEmpty()), viewModel.pageTitle.wikiSite.languageCode)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_save_section -> {
                clickNextButton()
                true
            }
            R.id.menu_edit_theme -> {
                binding.editSectionText.enqueueNoScrollingLayoutChange()
                ExclusiveBottomSheetPresenter.show(supportFragmentManager, ThemeChooserDialog.newInstance(Constants.InvokeSource.EDIT_ACTIVITY, true))
                true
            }
            R.id.menu_find_in_editor -> {
                showFindInEditor()
                true
            }
            R.id.menu_edit_notices -> {
                showEditNotices()
                true
            }
            R.id.menu_temp_account -> {
                maybeShowTempAccountDialog(true)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit_section, menu)

        menu.findItem(R.id.menu_temp_account).apply {
            isVisible = !AccountUtil.isLoggedIn || AccountUtil.isTemporaryAccount
            setIcon(if (AccountUtil.isTemporaryAccount) R.drawable.ic_temp_account else R.drawable.ic_anon_account)
        }

        val item = menu.findItem(R.id.menu_save_section)
        supportActionBar?.elevation = if (editPreviewFragment.isActive) 0f else DimenUtil.dpToPx(4f)
        menu.findItem(R.id.menu_edit_notices).isVisible = viewModel.editNotices.isNotEmpty() && !editPreviewFragment.isActive
        menu.findItem(R.id.menu_edit_theme).isVisible = !editPreviewFragment.isActive
        menu.findItem(R.id.menu_find_in_editor).isVisible = !editPreviewFragment.isActive
        item.title = getString(if (editSummaryFragment.isActive) R.string.edit_done else (if (viewModel.invokeSource == Constants.InvokeSource.EDIT_ADD_IMAGE) R.string.onboarding_continue else R.string.edit_next))
        if (viewModel.editingAllowed && binding.viewProgressBar.isGone) {
            item.isEnabled = sectionTextModified
        } else {
            item.isEnabled = false
        }
        val summaryFilledOrNotActive = if (editSummaryFragment.isActive) editSummaryFragment.summary.isNotEmpty() else true
        applyActionBarButtonStyle(item, item.isEnabled && summaryFilledOrNotActive)
        return true
    }

    override fun onActionModeStarted(mode: ActionMode) {
        super.onActionModeStarted(mode)
        if (mode.tag == null) {
            // since we disabled the close button in the AndroidManifest.xml, we need to manually setup a close button when in an action mode if long pressed on texts.
            ViewUtil.setCloseButtonInActionMode(this@EditSectionActivity, mode)
        }
    }

    private fun applyActionBarButtonStyle(menuItem: MenuItem, emphasize: Boolean) {
        val actionBarButtonBinding = ItemEditActionbarButtonBinding.inflate(layoutInflater)
        menuItem.actionView = actionBarButtonBinding.root
        actionBarButtonBinding.editActionbarButtonText.text = menuItem.title
        actionBarButtonBinding.editActionbarButtonText.setTextColor(
            ResourceUtil.getThemedColor(this,
                if (emphasize) R.attr.progressive_color else R.attr.placeholder_color))
        actionBarButtonBinding.root.tag = menuItem
        actionBarButtonBinding.root.isEnabled = menuItem.isEnabled
        actionBarButtonBinding.root.setOnClickListener { onOptionsItemSelected(it.tag as MenuItem) }
    }

    fun showError(caught: Throwable?) {
        DeviceUtil.hideSoftKeyboard(this)
        binding.viewEditSectionError.setError(caught)
        binding.viewEditSectionError.visibility = View.VISIBLE
    }

    private fun showFindInEditor() {
        startActionMode(object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                actionMode = mode
                val menuItem = menu.add(R.string.edit_section_find_in_page)
                menuItem.actionProvider = FindInEditorActionProvider(binding.editSectionScroll,
                        binding.editSectionText, syntaxHighlighter, actionMode!!)
                menuItem.expandActionView()
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                mode.tag = "actionModeFindInEditor"
                return false
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode) {
                syntaxHighlighter.clearSearchQueryInfo()
                binding.editSectionText.setSelection(binding.editSectionText.selectionStart,
                        binding.editSectionText.selectionStart)
            }
        })
    }

    private fun finishActionMode() {
        actionMode?.finish()
        actionMode = null
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(EXTRA_KEY_TEMPORARY_WIKITEXT_STORED, true)
        outState.putBoolean(EXTRA_KEY_SECTION_TEXT_MODIFIED, sectionTextModified)
        outState.putBoolean(EXTRA_KEY_EDITING_ALLOWED, viewModel.editingAllowed)
        Prefs.temporaryWikitext = viewModel.sectionWikitext.orEmpty()
    }

    private fun updateTextSize() {
        binding.editSectionText.textSize = WikipediaApp.instance.getFontSize(editing = true)
    }

    private fun resetToStart() {
        if (captchaHandler.isActive) {
            captchaHandler.cancelCaptcha()
            binding.editSectionCaptchaContainer.visibility = View.GONE
        }
        if (editSummaryFragment.isActive) {
            editSummaryFragment.hide()
        }
        if (editPreviewFragment.isActive) {
            editPreviewFragment.hide()
            binding.editSectionContainer.isVisible = true
        }
    }

    private fun fetchSectionText() {
        if (viewModel.sectionWikitext != null) {
            viewModel.fetchSectionText()
        } else {
            displaySectionText()
        }
    }

    private fun maybeShowEditNoticesTooltip() {
        if (!Prefs.autoShowEditNotices && !Prefs.isEditNoticesTooltipShown) {
            Prefs.isEditNoticesTooltipShown = true
            binding.root.postDelayed({
                val anchorView = findViewById<View>(R.id.menu_edit_notices)
                if (!isDestroyed && anchorView != null) {
                    FeedbackUtil.showTooltip(this, anchorView, getString(R.string.edit_notices_tooltip), aboveOrBelow = false, autoDismiss = false)
                }
            }, 100)
        }
    }

    private fun showEditNotices() {
        if (viewModel.editNotices.isEmpty()) {
            return
        }
        EditNoticesDialog(viewModel.pageTitle.wikiSite, viewModel.editNotices, this).show()
    }

    private fun maybeShowEditSourceDialog() {
        if (!Prefs.showEditTalkPageSourcePrompt || (viewModel.pageTitle.namespace() !== Namespace.TALK && viewModel.pageTitle.namespace() !== Namespace.USER_TALK)) {
            return
        }
        val binding = DialogWithCheckboxBinding.inflate(layoutInflater)
        binding.dialogMessage.text = StringUtil.fromHtml(getString(R.string.talk_edit_disclaimer))
        binding.dialogMessage.movementMethod = movementMethod
        MaterialAlertDialogBuilder(this@EditSectionActivity)
            .setView(binding.root)
            .setPositiveButton(R.string.onboarding_got_it) { dialog, _ -> dialog.dismiss() }
            .setOnDismissListener {
                Prefs.showEditTalkPageSourcePrompt = !binding.dialogCheckbox.isChecked
            }
            .show()
    }

    private fun displaySectionText() {
        showProgressBar(false)
        binding.editSectionText.setText(viewModel.sectionWikitext)
        binding.editSectionContainer.isVisible = true
        binding.editSectionText.isEnabled = viewModel.editingAllowed
        binding.editKeyboardOverlay.isVisible = viewModel.editingAllowed
        scrollToHighlight(viewModel.textToHighlight)
    }

    private fun scrollToHighlight(highlightText: String?) {
        if (highlightText == null || !TextUtils.isGraphic(highlightText)) {
            return
        }
        binding.editSectionText.highlightText(highlightText)
    }

    override fun getParentPageTitle(): PageTitle {
        return viewModel.pageTitle
    }

    override fun showProgressBar(visible: Boolean) {
        binding.viewProgressBar.isVisible = visible
        invalidateOptionsMenu()
    }

    override fun isNewPage(): Boolean {
        return false
    }

    override fun onBackPressed() {
        val addImageTitle = intent.parcelableExtra<PageTitle>(InsertMediaActivity.EXTRA_IMAGE_TITLE)
        val addImageSource = intent.getStringExtra(InsertMediaActivity.EXTRA_IMAGE_SOURCE)
        val addImageSourceProjects = intent.getStringExtra(InsertMediaActivity.EXTRA_IMAGE_SOURCE_PROJECTS)
        if (binding.viewProgressBar.isVisible) {
            // If it is visible, it means we should wait until all the requests are done.
            return
        }
        showProgressBar(false)
        if (captchaHandler.isActive) {
            captchaHandler.cancelCaptcha()
            binding.editSectionCaptchaContainer.visibility = View.GONE
        }
        binding.viewEditSectionError.isVisible = false
        if (editSummaryFragment.handleBackPressed()) {
            ImageRecommendationsEvent.logAction("back", "editsummary_dialog", ImageRecommendationsEvent.getActionDataString(
                filename = addImageTitle?.prefixedText.orEmpty(), recommendationSource = addImageSource.orEmpty(), recommendationSourceProjects = addImageSourceProjects.orEmpty(), acceptanceState = "accepted",
                captionAdd = !intent.getStringExtra(InsertMediaActivity.RESULT_IMAGE_CAPTION).isNullOrEmpty(),
                altTextAdd = !intent.getStringExtra(InsertMediaActivity.RESULT_IMAGE_ALT).isNullOrEmpty()), viewModel.pageTitle.wikiSite.languageCode)
            supportActionBar?.title = getString(R.string.edit_preview)
            return
        }
        if (editPreviewFragment.isActive) {
            ImageRecommendationsEvent.logAction("back", "caption_preview", ImageRecommendationsEvent.getActionDataString(
                filename = addImageTitle?.prefixedText.orEmpty(), recommendationSource = addImageSource.orEmpty(),
                recommendationSourceProjects = addImageSourceProjects.orEmpty(), acceptanceState = "accepted",
                captionAdd = !intent.getStringExtra(InsertMediaActivity.RESULT_IMAGE_CAPTION).isNullOrEmpty(),
                altTextAdd = !intent.getStringExtra(InsertMediaActivity.RESULT_IMAGE_ALT).isNullOrEmpty()), viewModel.pageTitle.wikiSite.languageCode)
            editPreviewFragment.hide()
            binding.editSectionContainer.isVisible = true
            supportActionBar?.title = null

            // If we came from the Image Recommendations workflow, bring back the Add Image activity.
            if (viewModel.invokeSource == Constants.InvokeSource.EDIT_ADD_IMAGE) {
                // ...and reset the wikitext to the original, since the Add Image flow will re-
                // modify it when the user returns to it.
                viewModel.sectionWikitext = viewModel.sectionWikitextOriginal
                binding.editSectionText.setText(viewModel.sectionWikitext)

                startInsertImageFlow()
            }
            return
        }
        setNavigationBarColor(ResourceUtil.getThemedColor(this, android.R.attr.colorBackground))
        DeviceUtil.hideSoftKeyboard(this)
        if (sectionTextModified) {
            doExitActionWithConfirmationDialog { finish() }
            return
        }
        super.onBackPressed()
    }

    private fun doExitActionWithConfirmationDialog(action: () -> Unit) {
        if (sectionTextModified) {
            val alert = MaterialAlertDialogBuilder(this)
            alert.setMessage(getString(R.string.edit_abandon_confirm))
            alert.setPositiveButton(getString(R.string.edit_abandon_confirm_yes)) { dialog, _ ->
                dialog.dismiss()
                action()
            }
            alert.setNegativeButton(getString(R.string.edit_abandon_confirm_no)) { dialog, _ -> dialog.dismiss() }
            alert.create().show()
        } else {
            action()
        }
    }

    private fun maybeShowTempAccountDialog(fromToolbar: Boolean = false): Boolean {
        if (fromToolbar || (!Prefs.tempAccountDialogShown && (!AccountUtil.isLoggedIn || AccountUtil.isTemporaryAccount))) {
            val dialog = MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme_Icon_NegativeInactive)
                .setIcon(if (AccountUtil.isTemporaryAccount) R.drawable.ic_temp_account else R.drawable.ic_anon_account)
                .setTitle(if (AccountUtil.isTemporaryAccount) R.string.temp_account_using_title else R.string.temp_account_not_logged_in)
                .setMessage(StringUtil.fromHtml(if (AccountUtil.isTemporaryAccount) getString(R.string.temp_account_temp_dialog_body, AccountUtil.userName)
                else getString(R.string.temp_account_anon_dialog_body)))
                .setPositiveButton(getString(if (fromToolbar) R.string.temp_account_dialog_ok else R.string.create_account_button)) { dialog, _ ->
                    dialog.dismiss()
                    if (!fromToolbar) {
                        launchLogin()
                    }
                }
                .setNegativeButton(getString(if (fromToolbar) R.string.create_account_login else R.string.temp_account_dialog_ok)) { dialog, _ ->
                    dialog.dismiss()
                    if (fromToolbar) {
                        launchLogin()
                    }
                }
                .show()
            dialog.window?.let {
                it.decorView.findViewById<TextView>(android.R.id.message)?.movementMethod = LinkMovementMethodExt { link ->
                    launchLogin(link.contains("#createaccount"))
                    dialog.dismiss()
                }
            }
            Prefs.tempAccountDialogShown = true
            return true
        }
        return false
    }

    private fun launchLogin(createAccountFirst: Boolean = true) {
        requestLogin.launch(LoginActivity.newIntent(this, LoginActivity.SOURCE_EDIT, createAccountFirst))
    }

    private fun startInsertImageFlow() {
        val addImageTitle = intent.parcelableExtra<PageTitle>(InsertMediaActivity.EXTRA_IMAGE_TITLE)!!
        val addImageSource = intent.getStringExtra(InsertMediaActivity.EXTRA_IMAGE_SOURCE)!!
        val addImageIntent = InsertMediaActivity.newIntent(this, viewModel.pageTitle.wikiSite,
            viewModel.pageTitle.displayText, viewModel.invokeSource, addImageTitle, addImageSource)

        // implicitly add any saved parameters from the previous insertion.
        addImageIntent.putExtra(InsertMediaActivity.EXTRA_IMAGE_TITLE, intent.parcelableExtra<PageTitle>(InsertMediaActivity.EXTRA_IMAGE_TITLE))
        addImageIntent.putExtra(InsertMediaActivity.RESULT_IMAGE_CAPTION, intent.getStringExtra(InsertMediaActivity.RESULT_IMAGE_CAPTION))
        addImageIntent.putExtra(InsertMediaActivity.RESULT_IMAGE_ALT, intent.getStringExtra(InsertMediaActivity.RESULT_IMAGE_ALT))
        addImageIntent.putExtra(InsertMediaActivity.RESULT_IMAGE_SIZE, intent.getStringExtra(InsertMediaActivity.RESULT_IMAGE_SIZE))
        addImageIntent.putExtra(InsertMediaActivity.RESULT_IMAGE_TYPE, intent.getStringExtra(InsertMediaActivity.RESULT_IMAGE_TYPE))
        addImageIntent.putExtra(InsertMediaActivity.RESULT_IMAGE_POS, intent.getStringExtra(InsertMediaActivity.RESULT_IMAGE_POS))
        addImageIntent.putExtra(InsertMediaActivity.EXTRA_IMAGE_SOURCE, intent.getStringExtra(InsertMediaActivity.EXTRA_IMAGE_SOURCE))
        addImageIntent.putExtra(InsertMediaActivity.EXTRA_IMAGE_SOURCE_PROJECTS, intent.getStringExtra(InsertMediaActivity.EXTRA_IMAGE_SOURCE_PROJECTS))

        requestInsertMedia.launch(addImageIntent)
    }

    override fun onToggleDimImages() { }

    override fun onToggleReadingFocusMode() { }

    override fun onCancelThemeChooser() { }

    override fun onEditingPrefsChanged() {
        binding.editSectionText.enqueueNoScrollingLayoutChange()
        updateTextSize()
        syntaxHighlighter.enabled = Prefs.editSyntaxHighlightEnabled
        binding.editSectionText.enableTypingSuggestions(Prefs.editTypingSuggestionsEnabled)
        binding.editSectionText.typeface = if (Prefs.editMonoSpaceFontEnabled) Typeface.MONOSPACE else Typeface.DEFAULT
        binding.editSectionText.showLineNumbers = Prefs.editLineNumbersEnabled
        binding.editSectionText.invalidate()
    }

    override fun onLinkPreviewLoadPage(title: PageTitle, entry: HistoryEntry, inNewTab: Boolean) {
        doExitActionWithConfirmationDialog {
            startActivity(if (inNewTab) PageActivity.newIntentForNewTab(this, entry, title) else
                PageActivity.newIntentForCurrentTab(this, entry, title, false))
        }
    }

    override fun onLinkPreviewDismiss() {
        if (!isDestroyed) {
            binding.editSectionText.postDelayed({
                DeviceUtil.showSoftKeyboard(binding.editSectionText)
            }, 200)
        }
    }

    companion object {
        const val EXTRA_KEY_SECTION_TEXT_MODIFIED = "sectionTextModified"
        const val EXTRA_KEY_TEMPORARY_WIKITEXT_STORED = "hasTemporaryWikitextStored"
        const val EXTRA_KEY_EDITING_ALLOWED = "editingAllowed"
        const val EXTRA_SECTION_ID = "sectionId"
        const val EXTRA_SECTION_ANCHOR = "sectionAnchor"
        const val EXTRA_HIGHLIGHT_TEXT = "sectionHighlightText"
        const val EXTRA_REV_ID = "revId"

        fun newIntent(context: Context, sectionId: Int, sectionAnchor: String?, title: PageTitle,
                      invokeSource: Constants.InvokeSource, highlightText: String? = null,
                      addImageTitle: PageTitle? = null, addImageSource: String = "", addImageSourceProjects: String = ""): Intent {
            return Intent(context, EditSectionActivity::class.java)
                .putExtra(EXTRA_SECTION_ID, sectionId)
                .putExtra(EXTRA_SECTION_ANCHOR, sectionAnchor)
                .putExtra(Constants.ARG_TITLE, title)
                .putExtra(EXTRA_HIGHLIGHT_TEXT, highlightText)
                .putExtra(Constants.INTENT_EXTRA_INVOKE_SOURCE, invokeSource)
                .putExtra(InsertMediaActivity.EXTRA_IMAGE_TITLE, addImageTitle)
                .putExtra(InsertMediaActivity.EXTRA_IMAGE_SOURCE, addImageSource)
                .putExtra(InsertMediaActivity.EXTRA_IMAGE_SOURCE_PROJECTS, addImageSourceProjects)
        }
    }
}
