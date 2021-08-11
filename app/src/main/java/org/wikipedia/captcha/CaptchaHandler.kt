package org.wikipedia.captcha

import android.app.Activity
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.R
import org.wikipedia.databinding.GroupCaptchaBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.views.ViewAnimations
import org.wikipedia.views.ViewUtil

class CaptchaHandler(private val activity: Activity, private val wiki: WikiSite,
                     captchaView: View, private val primaryView: View,
                     private val prevTitle: String, submitButtonText: String?) {
    private val binding = GroupCaptchaBinding.bind(captchaView)
    private val disposables = CompositeDisposable()
    private var captchaResult: CaptchaResult? = null
    var token: String? = null
    val isActive get() = captchaResult != null

    init {
        if (submitButtonText != null) {
            binding.captchaSubmitButton.text = submitButtonText
            binding.captchaSubmitButton.visibility = View.VISIBLE
        }
        binding.requestAccountText.text = StringUtil.fromHtml(activity.getString(R.string.edit_section_captcha_request_an_account_message))
        binding.requestAccountText.movementMethod = LinkMovementMethodExt { _ -> FeedbackUtil.showAndroidAppRequestAnAccount(activity) }
        binding.captchaImage.setOnClickListener { requestNewCaptcha() }
    }

    fun captchaId(): String? {
        return captchaResult?.captchaId
    }

    fun captchaWord(): String {
        return binding.captchaText.editText?.text.toString()
    }

    fun dispose() {
        disposables.clear()
    }

    fun handleCaptcha(token: String?, captchaResult: CaptchaResult) {
        this.token = token
        this.captchaResult = captchaResult
        handleCaptcha(false)
    }

    fun requestNewCaptcha() {
        binding.captchaImageProgress.visibility = View.VISIBLE
        disposables.add(ServiceFactory.get(wiki).newCaptcha
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .doAfterTerminate { binding.captchaImageProgress.visibility = View.GONE }
            .subscribe({ response ->
                captchaResult = CaptchaResult(response.captchaId)
                handleCaptcha(true)
            }) { caught ->
                cancelCaptcha()
                FeedbackUtil.showError(activity, caught)
            })
    }

    private fun handleCaptcha(isReload: Boolean) {
        if (captchaResult == null) {
            return
        }
        DeviceUtil.hideSoftKeyboard(activity)
        if (!isReload) {
            ViewAnimations.crossFade(primaryView, binding.root)
        }
        // In case there was a captcha attempt before
        binding.captchaText.editText?.setText("")
        ViewUtil.loadImage(binding.captchaImage, captchaResult!!.getCaptchaUrl(wiki), roundedCorners = false, largeRoundedSize = false, force = true, listener = null)
    }

    fun hideCaptcha() {
        (activity as AppCompatActivity).supportActionBar!!.title = prevTitle
        ViewAnimations.crossFade(binding.root, primaryView)
    }

    fun cancelCaptcha() {
        if (captchaResult == null) {
            return
        }
        captchaResult = null
        binding.captchaText.editText?.setText("")
        hideCaptcha()
    }
}
