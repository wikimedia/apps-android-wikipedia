package org.wikipedia.util

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.skydoves.balloon.*
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.SuggestedEditsFunnel
import org.wikipedia.databinding.ViewPlainTextTooltipBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.main.MainActivity
import org.wikipedia.page.LinkMovementMethodExt
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.page.edithistory.EditHistoryListActivity
import org.wikipedia.random.RandomActivity
import org.wikipedia.readinglist.ReadingListActivity
import org.wikipedia.richtext.RichTextUtil
import org.wikipedia.staticdata.SpecialAliasData
import org.wikipedia.staticdata.UserAliasData
import org.wikipedia.suggestededits.SuggestionsActivity
import java.util.concurrent.TimeUnit

object FeedbackUtil {
    private val LENGTH_SHORT = TimeUnit.SECONDS.toMillis(3).toInt()
    val LENGTH_DEFAULT = TimeUnit.SECONDS.toMillis(5).toInt()
    val LENGTH_MEDIUM = TimeUnit.SECONDS.toMillis(8).toInt()
    val LENGTH_LONG = TimeUnit.SECONDS.toMillis(15).toInt()
    private val TOOLBAR_LONG_CLICK_LISTENER = View.OnLongClickListener { v ->
        showToastOverView(v, v.contentDescription, LENGTH_DEFAULT)
        true
    }
    private val TOOLBAR_ON_CLICK_LISTENER = View.OnClickListener { v ->
        showToastOverView(v, v.contentDescription, LENGTH_SHORT)
    }

    fun showError(activity: Activity, e: Throwable) {
        val error = ThrowableUtil.getAppError(activity, e)
        makeSnackbar(activity, error.error, LENGTH_DEFAULT).also {
            if (error.error.length > 200) {
                it.duration = Snackbar.LENGTH_INDEFINITE
                it.setAction(android.R.string.ok) { _ ->
                    it.dismiss()
                }
            }
            it.show()
        }
    }

    fun showMessageAsPlainText(activity: Activity, possibleHtml: CharSequence) {
        val richText: CharSequence = StringUtil.fromHtml(possibleHtml.toString())
        showMessage(activity, richText.toString())
    }

    fun showMessage(fragment: Fragment, @StringRes text: Int) {
        makeSnackbar(fragment.requireActivity(), fragment.getString(text), Snackbar.LENGTH_LONG).show()
    }

    fun showMessage(fragment: Fragment, text: String) {
        makeSnackbar(fragment.requireActivity(), text, Snackbar.LENGTH_LONG).show()
    }

    fun showMessage(activity: Activity, @StringRes resId: Int) {
        showMessage(activity, activity.getString(resId), Snackbar.LENGTH_LONG)
    }

    fun showMessage(activity: Activity, @StringRes resId: Int, duration: Int) {
        showMessage(activity, activity.getString(resId), duration)
    }

    fun showMessage(activity: Activity, text: CharSequence, duration: Int = Snackbar.LENGTH_LONG) {
        makeSnackbar(activity, text, duration).show()
    }

    fun showPrivacyPolicy(context: Context) {
        UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(R.string.privacy_policy_url)))
    }

    fun showOfflineReadingAndData(context: Context) {
        UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(R.string.offline_reading_and_data_url)))
    }

    fun showAboutWikipedia(context: Context) {
        UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(R.string.about_wikipedia_url)))
    }

    fun showAndroidAppFAQ(context: Context) {
        UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(R.string.android_app_faq_url)))
    }

    fun showAndroidAppRequestAnAccount(context: Context) {
        UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(R.string.android_app_request_an_account_url)))
    }

    fun showUserContributionsPage(context: Context, username: String, languageCode: String) {
        val title = PageTitle(SpecialAliasData.valueFor(languageCode) + ":" +
                "Contributions/" + username, WikiSite.forLanguageCode(languageCode))
        UriUtil.visitInExternalBrowser(context, Uri.parse(title.uri))
    }

    fun showUserProfilePage(context: Context, username: String, languageCode: String) {
        val title = PageTitle(UserAliasData.valueFor(languageCode) + ":" +
                username, WikiSite.forLanguageCode(languageCode))
        UriUtil.visitInExternalBrowser(context, Uri.parse(title.uri))
    }

    fun showAndroidAppEditingFAQ(context: Context,
                                 @StringRes urlStr: Int = R.string.android_app_edit_help_url) {
        SuggestedEditsFunnel.get().helpOpened()
        UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(urlStr)))
    }

    fun setButtonLongPressToast(vararg views: View) {
        views.forEach { it.setOnLongClickListener(TOOLBAR_LONG_CLICK_LISTENER) }
    }

    fun setButtonOnClickToast(vararg views: View) {
        views.forEach { it.setOnClickListener(TOOLBAR_ON_CLICK_LISTENER) }
    }

    fun makeSnackbar(activity: Activity, text: CharSequence, duration: Int): Snackbar {
        val view = findBestView(activity)
        val snackbar = Snackbar.make(view, StringUtil.fromHtml(text.toString()), duration)
        val textView = snackbar.view.findViewById<TextView>(R.id.snackbar_text)
        textView.setLinkTextColor(ResourceUtil.getThemedColor(view.context, R.attr.color_group_52))
        textView.movementMethod = LinkMovementMethodExt.getExternalLinkMovementMethod()
        RichTextUtil.removeUnderlinesFromLinks(textView)
        val actionView = snackbar.view.findViewById<TextView>(R.id.snackbar_action)
        actionView.setTextColor(ResourceUtil.getThemedColor(view.context, R.attr.color_group_52))
        return snackbar
    }

    fun showToastOverView(view: View, text: CharSequence?, duration: Int): Toast {
        val toast = Toast.makeText(view.context, text, duration)
        val v = LayoutInflater.from(view.context).inflate(R.layout.abc_tooltip, null)
        val message = v.findViewById<TextView>(R.id.message)
        message.text = text
        message.maxLines = Int.MAX_VALUE
        toast.view = v
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        toast.setGravity(Gravity.TOP or Gravity.START, location[0], location[1])
        toast.show()
        return toast
    }

    fun showTooltip(activity: Activity, anchor: View, text: CharSequence, aboveOrBelow: Boolean,
                    autoDismiss: Boolean, arrowAnchorPadding: Int = 0, topOrBottomMargin: Int = 0): Balloon {
        return showTooltip(activity, getTooltip(anchor.context, text, autoDismiss, arrowAnchorPadding, topOrBottomMargin, aboveOrBelow), anchor, aboveOrBelow, autoDismiss)
    }

    fun showTooltip(activity: Activity, anchor: View, @LayoutRes layoutRes: Int,
                    arrowAnchorPadding: Int, topOrBottomMargin: Int, aboveOrBelow: Boolean, autoDismiss: Boolean): Balloon {
        return showTooltip(activity, getTooltip(anchor.context, layoutRes, arrowAnchorPadding, topOrBottomMargin, aboveOrBelow, autoDismiss), anchor, aboveOrBelow, autoDismiss)
    }

    private fun showTooltip(activity: Activity, balloon: Balloon, anchor: View, aboveOrBelow: Boolean, autoDismiss: Boolean): Balloon {
        if (aboveOrBelow) {
            balloon.showAlignTop(anchor, 0, DimenUtil.roundedDpToPx(8f))
        } else {
            balloon.showAlignBottom(anchor, 0, -DimenUtil.roundedDpToPx(8f))
        }
        if (!autoDismiss) {
            (activity as BaseActivity).setCurrentTooltip(balloon)
        }
        return balloon
    }

    fun getTooltip(context: Context, text: CharSequence, autoDismiss: Boolean, arrowAnchorPadding: Int = 0,
                   topOrBottomMargin: Int = 0, aboveOrBelow: Boolean = false, showDismissButton: Boolean = false): Balloon {
        val binding = ViewPlainTextTooltipBinding.inflate(LayoutInflater.from(context))
        binding.textView.text = text
        if (showDismissButton) {
            binding.buttonView.visibility = View.VISIBLE
        }

        val balloon = createBalloon(context) {
            setArrowDrawableResource(R.drawable.ic_tooltip_arrow_up)
            setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
            setArrowOrientationRules(ArrowOrientationRules.ALIGN_ANCHOR)
            setArrowSize(24)
            setMarginLeft(8)
            setMarginRight(8)
            setMarginTop(if (aboveOrBelow) 0 else topOrBottomMargin)
            setMarginBottom(if (aboveOrBelow) topOrBottomMargin else 0)
            setBackgroundColorResource(ResourceUtil.getThemedAttributeId(context, R.attr.colorAccent))
            setDismissWhenTouchOutside(autoDismiss)
            setLayout(binding.root)
            setWidth(BalloonSizeSpec.WRAP)
            setHeight(BalloonSizeSpec.WRAP)
            setArrowAlignAnchorPadding(arrowAnchorPadding)
        }

        binding.buttonView.setOnClickListener {
            balloon.dismiss()
        }

        return balloon
    }

    private fun getTooltip(context: Context, @LayoutRes layoutRes: Int, arrowAnchorPadding: Int,
                           topOrBottomMargin: Int, aboveOrBelow: Boolean, autoDismiss: Boolean): Balloon {
        return createBalloon(context) {
            setArrowDrawableResource(R.drawable.ic_tooltip_arrow_up)
            setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
            setArrowOrientationRules(ArrowOrientationRules.ALIGN_ANCHOR)
            setArrowSize(24)
            setMarginLeft(8)
            setMarginRight(8)
            setMarginTop(if (aboveOrBelow) 0 else topOrBottomMargin)
            setMarginBottom(if (aboveOrBelow) topOrBottomMargin else 0)
            setBackgroundColorResource(ResourceUtil.getThemedAttributeId(context, R.attr.colorAccent))
            setDismissWhenTouchOutside(autoDismiss)
            setLayout(layoutRes)
            setWidth(BalloonSizeSpec.WRAP)
            setHeight(BalloonSizeSpec.WRAP)
            setArrowAlignAnchorPadding(arrowAnchorPadding)
        }
    }

    private fun findBestView(activity: Activity): View {
        val viewId = when (activity) {
            is MainActivity -> R.id.fragment_main_coordinator
            is PageActivity -> R.id.fragment_page_coordinator
            is RandomActivity -> R.id.random_coordinator_layout
            is ReadingListActivity -> R.id.fragment_reading_list_coordinator
            is SuggestionsActivity -> R.id.suggestedEditsCardsCoordinator
            is EditHistoryListActivity -> R.id.edit_history_coordinator
            else -> android.R.id.content
        }
        return ActivityCompat.requireViewById(activity, viewId)
    }
}
