package org.wikipedia.edit

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.text.TextWatcher
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.net.toUri
import androidx.core.os.postDelayed
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.core.widget.doAfterTextChanged
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.EditFunnel
import org.wikipedia.analytics.LoginFunnel
import org.wikipedia.auth.AccountUtil.isLoggedIn
import org.wikipedia.captcha.CaptchaHandler
import org.wikipedia.captcha.CaptchaResult
import org.wikipedia.csrf.CsrfTokenClient
import org.wikipedia.databinding.ActivityEditSectionBinding
import org.wikipedia.databinding.ItemEditActionbarButtonBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwException
import org.wikipedia.dataclient.mwapi.MwParseResponse
import org.wikipedia.dataclient.mwapi.MwServiceError
import org.wikipedia.dataclient.okhttp.OkHttpConnectionFactory
import org.wikipedia.edit.preview.EditPreviewFragment
import org.wikipedia.edit.richtext.SyntaxHighlighter
import org.wikipedia.edit.summaries.EditSummaryFragment
import org.wikipedia.history.HistoryEntry
import org.wikipedia.login.LoginActivity
import org.wikipedia.notifications.AnonymousNotificationHelper
import org.wikipedia.page.*
import org.wikipedia.page.linkpreview.LinkPreviewDialog
import org.wikipedia.settings.Prefs
import org.wikipedia.util.*
import org.wikipedia.util.log.L
import org.wikipedia.views.*
import java.io.IOException
import java.util.concurrent.TimeUnit

class EditSectionActivity : BaseActivity() {
    private lateinit var binding: ActivityEditSectionBinding
    private lateinit var funnel: EditFunnel
    private lateinit var textWatcher: TextWatcher
    private lateinit var captchaHandler: CaptchaHandler
    private lateinit var editPreviewFragment: EditPreviewFragment
    private lateinit var editSummaryFragment: EditSummaryFragment
    private lateinit var syntaxHighlighter: SyntaxHighlighter
    lateinit var pageTitle: PageTitle
        private set

    private var sectionID = 0
    private var sectionAnchor: String? = null
    private var textToHighlight: String? = null
    private var sectionWikitext: String? = null
    private val editNotices = mutableListOf<String>()

    private var sectionTextModified = false
    private var sectionTextFirstLoad = true
    private var editingAllowed = false

    // Current revision of the article, to be passed back to the server to detect possible edit conflicts.
    private var currentRevision: Long = 0
    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()
    private var actionMode: ActionMode? = null
    private val disposables = CompositeDisposable()

    private val editTokenThenSave: Unit
        get() {
            cancelCalls()
            binding.editSectionCaptchaContainer.visibility = View.GONE
            captchaHandler.hideCaptcha()
            editSummaryFragment.saveSummary()
            disposables.add(CsrfTokenClient(pageTitle.wikiSite).token
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ doSave(it) }) { showError(it) })
        }

    private val movementMethod = LinkMovementMethodExt { urlStr ->
        UriUtil.visitInExternalBrowser(this, Uri.parse(UriUtil.resolveProtocolRelativeUrl(pageTitle.wikiSite, urlStr)))
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditSectionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setNavigationBarColor(ResourceUtil.getThemedColor(this, android.R.attr.colorBackground))

        pageTitle = intent.getParcelableExtra(EXTRA_TITLE)!!
        sectionID = intent.getIntExtra(EXTRA_SECTION_ID, 0)
        sectionAnchor = intent.getStringExtra(EXTRA_SECTION_ANCHOR)
        textToHighlight = intent.getStringExtra(EXTRA_HIGHLIGHT_TEXT)
        supportActionBar?.title = ""
        syntaxHighlighter = SyntaxHighlighter(this, binding.editSectionText)
        binding.editSectionScroll.isSmoothScrollingEnabled = false
        captchaHandler = CaptchaHandler(this, pageTitle.wikiSite, binding.captchaContainer.root,
                binding.editSectionText, "", null)
        editPreviewFragment = supportFragmentManager.findFragmentById(R.id.edit_section_preview_fragment) as EditPreviewFragment
        editSummaryFragment = supportFragmentManager.findFragmentById(R.id.edit_section_summary_fragment) as EditSummaryFragment
        editSummaryFragment.title = pageTitle
        funnel = WikipediaApp.getInstance().funnelManager.getEditFunnel(pageTitle)

        // Only send the editing start log event if the activity is created for the first time
        if (savedInstanceState == null) {
            funnel.logStart()
        }
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(EXTRA_KEY_TEMPORARY_WIKITEXT_STORED)) {
                sectionWikitext = Prefs.temporaryWikitext
            }
            editingAllowed = savedInstanceState.getBoolean(EXTRA_KEY_EDITING_ALLOWED, false)
            sectionTextModified = savedInstanceState.getBoolean(EXTRA_KEY_SECTION_TEXT_MODIFIED, false)
        }
        L10nUtil.setConditionalTextDirection(binding.editSectionText, pageTitle.wikiSite.languageCode)
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
        binding.editKeyboardOverlay.editText = binding.editSectionText
        binding.editKeyboardOverlay.callback = WikiTextKeyboardView.Callback {
            bottomSheetPresenter.show(supportFragmentManager,
                    LinkPreviewDialog.newInstance(HistoryEntry(PageTitle(it, pageTitle.wikiSite), HistoryEntry.SOURCE_INTERNAL_LINK), null))
        }
        binding.editSectionText.setOnClickListener { finishActionMode() }
        updateTextSize()

        // set focus to the EditText, but keep the keyboard hidden until the user changes the cursor location:
        binding.editSectionText.requestFocus()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
    }

    public override fun onStart() {
        super.onStart()
        updateEditLicenseText()
    }

    public override fun onDestroy() {
        captchaHandler.dispose()
        cancelCalls()
        binding.editSectionText.removeTextChangedListener(textWatcher)
        syntaxHighlighter.cleanup()
        super.onDestroy()
    }

    private fun updateEditLicenseText() {
        val editLicenseText = ActivityCompat.requireViewById<TextView>(this, R.id.edit_section_license_text)
        editLicenseText.text = StringUtil.fromHtml(getString(if (isLoggedIn) R.string.edit_save_action_license_logged_in else R.string.edit_save_action_license_anon,
                getString(R.string.terms_of_use_url),
                getString(R.string.cc_by_sa_3_url)))
        editLicenseText.movementMethod = LinkMovementMethodExt { url: String ->
            if (url == "https://#login") {
                funnel.logLoginAttempt()
                val loginIntent = LoginActivity.newIntent(this@EditSectionActivity,
                        LoginFunnel.SOURCE_EDIT, funnel.sessionToken)
                startActivityForResult(loginIntent, Constants.ACTIVITY_REQUEST_LOGIN)
            } else {
                UriUtil.handleExternalLink(this@EditSectionActivity, url.toUri())
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constants.ACTIVITY_REQUEST_LOGIN) {
            if (resultCode == LoginActivity.RESULT_LOGIN_SUCCESS) {
                updateEditLicenseText()
                funnel.logLoginSuccess()
                FeedbackUtil.showMessage(this, R.string.login_success_toast)
            } else {
                funnel.logLoginFailure()
            }
        }
    }

    private fun cancelCalls() {
        disposables.clear()
    }

    private fun doSave(token: String) {
        val sectionAnchor = StringUtil.addUnderscores(StringUtil.removeHTMLTags(sectionAnchor.orEmpty()))
        var summaryText = if (sectionAnchor.isEmpty() || sectionAnchor == pageTitle.prefixedText) "/* top */"
        else "/* ${StringUtil.removeUnderscores(sectionAnchor)} */ "
        summaryText += editPreviewFragment.summary
        // Summaries are plaintext, so remove any HTML that's made its way into the summary
        summaryText = StringUtil.removeHTMLTags(summaryText)
        if (!isFinishing) {
            showProgressBar(true)
        }
        disposables.add(ServiceFactory.get(pageTitle.wikiSite).postEditSubmit(pageTitle.prefixedText,
                sectionID.toString(), null, summaryText, if (isLoggedIn) "user" else null,
                binding.editSectionText.text.toString(), null, currentRevision, token,
                if (captchaHandler.isActive) captchaHandler.captchaId() else "null",
                if (captchaHandler.isActive) captchaHandler.captchaWord() else "null")
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ result ->
                    result.edit?.run {
                        when {
                            editSucceeded -> waitForUpdatedRevision(newRevId)
                            hasCaptchaResponse -> onEditSuccess(CaptchaResult(captchaId))
                            hasSpamBlacklistResponse -> onEditFailure(MwException(MwServiceError(code, spamblacklist)))
                            hasEditErrorCode -> onEditFailure(MwException(MwServiceError(code, info)))
                            else -> onEditFailure(IOException("Received unrecognized edit response"))
                        }
                    } ?: run {
                        onEditFailure(IOException("An unknown error occurred."))
                    }
                }) { onEditFailure(it) }
        )
    }

    @Suppress("SameParameterValue")
    private fun waitForUpdatedRevision(newRevision: Long) {
        AnonymousNotificationHelper.onEditSubmitted()
        disposables.add(ServiceFactory.getRest(pageTitle.wikiSite)
            .getSummaryResponse(pageTitle.prefixedText, null, OkHttpConnectionFactory.CACHE_CONTROL_FORCE_NETWORK.toString(), null, null, null)
            .delay(2, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .map { response ->
                if (response.body()!!.revision < newRevision) {
                    throw IllegalStateException()
                }
                response.body()!!.revision
            }
            .retry(10) { it is IllegalStateException }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                onEditSuccess(EditSuccessResult(it))
            }, {
                onEditSuccess(EditSuccessResult(newRevision))
            })
        )
    }

    private fun onEditSuccess(result: EditResult) {
        if (result is EditSuccessResult) {
            funnel.logSaved(result.revID)
            // TODO: remove the artificial delay and use the new revision
            // ID returned to request the updated version of the page once
            // revision support for mobile-sections is added to RESTBase
            // See https://github.com/wikimedia/restbase/pull/729
            Handler(mainLooper).postDelayed(TimeUnit.SECONDS.toMillis(2)) {
                showProgressBar(false)

                // Build intent that includes the section we were editing, so we can scroll to it later
                val data = Intent()
                data.putExtra(EXTRA_SECTION_ID, sectionID)
                setResult(EditHandler.RESULT_REFRESH_PAGE, data)
                DeviceUtil.hideSoftKeyboard(this@EditSectionActivity)
                finish()
            }
            return
        }
        showProgressBar(false)
        if (result is CaptchaResult) {
            if (captchaHandler.isActive) {
                // Captcha entry failed!
                funnel.logCaptchaFailure()
            }
            binding.editSectionCaptchaContainer.visibility = View.VISIBLE
            captchaHandler.handleCaptcha(null, result)
            funnel.logCaptchaShown()
        } else {
            funnel.logError(result.result)
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
        val retryDialog = AlertDialog.Builder(this@EditSectionActivity)
                .setTitle(R.string.dialog_message_edit_failed)
                .setMessage(t.localizedMessage)
                .setPositiveButton(R.string.dialog_message_edit_failed_retry) { dialog, _ ->
                    editTokenThenSave
                    dialog.dismiss()
                }
                .setNegativeButton(R.string.dialog_message_edit_failed_cancel) { dialog, _ -> dialog.dismiss() }.create()
        retryDialog.show()
    }

    /**
     * Processes API error codes encountered during editing, and handles them as appropriate.
     * @param caught The MwException to handle.
     */
    private fun handleEditingException(caught: MwException) {
        val code = caught.title

        // In the case of certain AbuseFilter responses, they are sent as a code, instead of a
        // fully parsed response. We need to make one more API call to get the parsed message:
        if (code.startsWith("abusefilter-") && caught.message.contains("abusefilter-") && caught.message.length < 100) {
            disposables.add(ServiceFactory.get(pageTitle.wikiSite).parsePage("MediaWiki:" + StringUtil.sanitizeAbuseFilterCode(caught.message))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ response: MwParseResponse -> showError(MwException(MwServiceError(code, response.text))) }) { showError(it) })
        } else if ("editconflict" == code) {
            AlertDialog.Builder(this@EditSectionActivity)
                    .setTitle(R.string.edit_conflict_title)
                    .setMessage(R.string.edit_conflict_message)
                    .setPositiveButton(R.string.edit_conflict_dialog_ok_button_text, null)
                    .show()
            resetToStart()
        } else {
            showError(caught)
        }
    }

    /**
     * Executes a click of the actionbar button, and performs the appropriate action
     * based on the current state of the button.
     */
    fun clickNextButton() {
        when {
            editSummaryFragment.isActive -> {
                // we're showing the custom edit summary window, so close it and
                // apply the provided summary.
                editSummaryFragment.hide()
                editPreviewFragment.setCustomSummary(editSummaryFragment.summary)
            }
            editPreviewFragment.isActive -> {
                // we're showing the Preview window, which means that the next step is to save it!
                editTokenThenSave
                funnel.logSaveAttempt()
            }
            else -> {
                // we must be showing the editing window, so show the Preview.
                DeviceUtil.hideSoftKeyboard(this)
                editPreviewFragment.showPreview(pageTitle, binding.editSectionText.text.toString())
                funnel.logPreview()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_save_section -> {
                clickNextButton()
                true
            }
            R.id.menu_edit_zoom_in -> {
                Prefs.editingTextSizeExtra = Prefs.editingTextSizeExtra + 1
                updateTextSize()
                true
            }
            R.id.menu_edit_zoom_out -> {
                Prefs.editingTextSizeExtra = Prefs.editingTextSizeExtra - 1
                updateTextSize()
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
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit_section, menu)
        val item = menu.findItem(R.id.menu_save_section)

        menu.findItem(R.id.menu_edit_notices).isVisible = editNotices.isNotEmpty() && !editPreviewFragment.isActive
        menu.findItem(R.id.menu_edit_zoom_in).isVisible = !editPreviewFragment.isActive
        menu.findItem(R.id.menu_edit_zoom_out).isVisible = !editPreviewFragment.isActive
        menu.findItem(R.id.menu_find_in_editor).isVisible = !editPreviewFragment.isActive
        item.title = getString(if (editPreviewFragment.isActive) R.string.edit_done else R.string.edit_next)
        if (editingAllowed && binding.viewProgressBar.isGone) {
            item.isEnabled = sectionTextModified
        } else {
            item.isEnabled = false
        }
        val actionBarButtonBinding = ItemEditActionbarButtonBinding.inflate(layoutInflater)
        item.actionView = actionBarButtonBinding.root
        actionBarButtonBinding.editActionbarButtonText.text = item.title
        actionBarButtonBinding.editActionbarButtonText.setTextColor(ResourceUtil.getThemedColor(this,
                if (item.isEnabled) R.attr.colorAccent else R.attr.material_theme_de_emphasised_color))
        actionBarButtonBinding.root.tag = item
        actionBarButtonBinding.root.isEnabled = item.isEnabled
        actionBarButtonBinding.root.setOnClickListener { onOptionsItemSelected(it.tag as MenuItem) }
        return true
    }

    override fun onActionModeStarted(mode: ActionMode) {
        super.onActionModeStarted(mode)
        if (mode.tag == null) {
            // since we disabled the close button in the AndroidManifest.xml, we need to manually setup a close button when in an action mode if long pressed on texts.
            ViewUtil.setCloseButtonInActionMode(this@EditSectionActivity, mode)
        }
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
                binding.editSectionText.clearMatches(syntaxHighlighter)
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
        outState.putBoolean(EXTRA_KEY_EDITING_ALLOWED, editingAllowed)
        Prefs.temporaryWikitext = sectionWikitext.orEmpty()
    }

    private fun updateTextSize() {
        val extra = Prefs.editingTextSizeExtra
        binding.editSectionText.textSize = WikipediaApp.getInstance().getFontSize(window) + extra.toFloat()
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
            editPreviewFragment.hide(binding.editSectionContainer)
        }
    }

    private fun fetchSectionText() {
        if (sectionWikitext == null) {
            disposables.add(ServiceFactory.get(pageTitle.wikiSite).getWikiTextForSectionWithInfo(pageTitle.prefixedText, sectionID)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ response ->
                        val firstPage = response.query?.firstPage()!!
                        val rev = firstPage.revisions[0]

                        pageTitle = PageTitle(firstPage.title, pageTitle.wikiSite)
                        sectionWikitext = rev.content
                        currentRevision = rev.revId

                        val editError = response.query?.firstPage()!!.getErrorForAction("edit")
                        if (editError.isEmpty()) {
                            editingAllowed = true
                        } else {
                            val error = editError[0]
                            FeedbackUtil.showError(this, MwException(error))
                        }
                        displaySectionText()
                        maybeShowEditSourceDialog()
                    }) { throwable ->
                        showProgressBar(false)
                        showError(throwable)
                        L.e(throwable)
                    })
            disposables.add(ServiceFactory.get(pageTitle.wikiSite).getVisualEditorMetadata(pageTitle.prefixedText)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        editNotices.clear()
                        // Populate edit notices, but filter out anonymous edit warnings, since
                        // we show that type of warning ourselves when previewing.
                        editNotices.addAll(it.visualeditor?.notices.orEmpty()
                                .filterKeys { key -> key != "anoneditwarning" }.values)
                        invalidateOptionsMenu()
                        if (Prefs.autoShowEditNotices) {
                            showEditNotices()
                        } else {
                            maybeShowEditNoticesTooltip()
                        }
                    }, {
                        L.e(it)
                    }))
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
        if (editNotices.isEmpty()) {
            return
        }
        EditNoticesDialog(pageTitle.wikiSite, editNotices, this).show()
    }

    private fun maybeShowEditSourceDialog() {
        if (pageTitle.namespace() !== Namespace.USER && pageTitle.namespace() !== Namespace.USER_TALK) {
            return
        }
        AlertDialog.Builder(this@EditSectionActivity)
            .setMessage(R.string.talk_edit_disclaimer)
            .setPositiveButton(R.string.onboarding_got_it) { dialog, _ -> dialog.dismiss() }
            .show()
            .findViewById<TextView>(android.R.id.message)?.movementMethod = movementMethod
    }

    private fun displaySectionText() {
        binding.editSectionText.setText(sectionWikitext)
        ViewAnimations.crossFade(binding.viewProgressBar, binding.editSectionContainer)
        scrollToHighlight(textToHighlight)
        binding.editSectionText.isEnabled = editingAllowed
        binding.editKeyboardOverlay.isVisible = editingAllowed
    }

    private fun scrollToHighlight(highlightText: String?) {
        if (highlightText == null || !TextUtils.isGraphic(highlightText)) {
            return
        }
        binding.editSectionText.post {
            binding.editSectionScroll.fullScroll(View.FOCUS_DOWN)
            binding.editSectionText.postDelayed(500) {
                StringUtil.highlightEditText(binding.editSectionText, sectionWikitext!!, highlightText)
            }
        }
    }

    fun showProgressBar(enable: Boolean) {
        binding.viewProgressBar.isVisible = enable
        invalidateOptionsMenu()
    }

    /**
     * Shows the custom edit summary input fragment, where the user may enter a summary
     * that's different from the standard summary tags.
     */
    fun showCustomSummary() {
        editSummaryFragment.show()
    }

    override fun onBackPressed() {
        if (binding.viewProgressBar.isVisible) {
            // If it is visible, it means we should wait until all the requests are done.
            return
        }
        showProgressBar(false)
        if (captchaHandler.isActive) {
            captchaHandler.cancelCaptcha()
            binding.editSectionCaptchaContainer.visibility = View.GONE
        }
        if (binding.viewEditSectionError.isVisible) {
            binding.viewEditSectionError.visibility = View.GONE
        }
        if (editSummaryFragment.handleBackPressed()) {
            return
        }
        if (editPreviewFragment.isActive) {
            editPreviewFragment.hide(binding.editSectionContainer)
            return
        }
        DeviceUtil.hideSoftKeyboard(this)
        if (sectionTextModified) {
            val alert = AlertDialog.Builder(this)
            alert.setMessage(getString(R.string.edit_abandon_confirm))
            alert.setPositiveButton(getString(R.string.edit_abandon_confirm_yes)) { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            alert.setNegativeButton(getString(R.string.edit_abandon_confirm_no)) { dialog, _ -> dialog.dismiss() }
            alert.create().show()
        } else {
            finish()
        }
    }

    companion object {
        private const val EXTRA_KEY_SECTION_TEXT_MODIFIED = "sectionTextModified"
        private const val EXTRA_KEY_TEMPORARY_WIKITEXT_STORED = "hasTemporaryWikitextStored"
        private const val EXTRA_KEY_EDITING_ALLOWED = "editingAllowed"
        const val EXTRA_TITLE = "org.wikipedia.edit_section.title"
        const val EXTRA_SECTION_ID = "org.wikipedia.edit_section.sectionid"
        const val EXTRA_SECTION_ANCHOR = "org.wikipedia.edit_section.anchor"
        const val EXTRA_HIGHLIGHT_TEXT = "org.wikipedia.edit_section.highlight"

        fun newIntent(context: Context, sectionId: Int, sectionAnchor: String?, title: PageTitle, highlightText: String? = null): Intent {
            return Intent(context, EditSectionActivity::class.java)
                .putExtra(EXTRA_SECTION_ID, sectionId)
                .putExtra(EXTRA_SECTION_ANCHOR, sectionAnchor)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_HIGHLIGHT_TEXT, highlightText)
        }
    }
}
