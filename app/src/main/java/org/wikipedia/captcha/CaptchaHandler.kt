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
import org.wikipedia.util.DeviceUtil.hideSoftKeyboard
import org.wikipedia.util.FeedbackUtil.showAndroidAppRequestAnAccount
import org.wikipedia.util.FeedbackUtil.showError
import org.wikipedia.util.StringUtil.fromHtml
import org.wikipedia.views.ViewAnimations.crossFade
import org.wikipedia.views.ViewUtil.loadImage

class CaptchaHandler(private val activity: Activity, private val wiki: WikiSite,
                     captchaView: View, private val primaryView: View,
                     private val prevTitle: String, submitButtonText: String?) {
    private val binding: GroupCaptchaBinding = GroupCaptchaBinding.bind(captchaView)
    private val disposables = CompositeDisposable()
    private var captchaResult: CaptchaResult? = null
    var token: String? = null
    val isActive: Boolean
        get() = captchaResult != null

    init {
        if (submitButtonText != null) {
            binding.captchaSubmitButton.text = submitButtonText
            binding.captchaSubmitButton.visibility = View.VISIBLE
        }
        binding.requestAccountText.text = fromHtml(activity.getString(R.string.edit_section_captcha_request_an_account_message))
        binding.requestAccountText.movementMethod = LinkMovementMethodExt { _: String? -> showAndroidAppRequestAnAccount(activity) }
        binding.captchaImage.setOnClickListener {
            binding.captchaImageProgress.visibility = View.VISIBLE
            disposables.add(ServiceFactory.get(wiki).newCaptcha
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doAfterTerminate { binding.captchaImageProgress.visibility = View.GONE }
                    .subscribe({ response: Captcha ->
                        captchaResult = CaptchaResult(response.captchaId())
                        handleCaptcha(true)
                    }) { caught ->
                        cancelCaptcha()
                        showError(activity, caught)
                    })
        }
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

    private fun handleCaptcha(isReload: Boolean) {
        if (captchaResult == null) {
            return
        }
        hideSoftKeyboard(activity)
        if (!isReload) {
            crossFade(primaryView, binding.root)
        }
        // In case there was a captcha attempt before
        binding.captchaText.editText?.setText("")
        loadImage(binding.captchaImage, captchaResult!!.getCaptchaUrl(wiki), roundedCorners = false, largeRoundedSize = false, force = true, listener = null)
    }

    fun hideCaptcha() {
        (activity as AppCompatActivity).supportActionBar!!.title = prevTitle
        crossFade(binding.root, primaryView)
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
