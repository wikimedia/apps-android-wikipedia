package org.wikipedia.util

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnLongClickListener
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.skydoves.balloon.*
import org.wikipedia.R
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.SuggestedEditsFunnel
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.main.MainActivity
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.random.RandomActivity
import org.wikipedia.readinglist.ReadingListActivity
import org.wikipedia.staticdata.SpecialAliasData
import org.wikipedia.staticdata.UserAliasData
import org.wikipedia.suggestededits.SuggestionsActivity
import org.wikipedia.util.DimenUtil.roundedDpToPx
import java.util.concurrent.TimeUnit

object FeedbackUtil {
    @JvmField
    val LENGTH_DEFAULT = TimeUnit.SECONDS.toMillis(5).toInt()

    @JvmField
    val LENGTH_MEDIUM = TimeUnit.SECONDS.toMillis(8).toInt()
    val LENGTH_LONG = TimeUnit.SECONDS.toMillis(15).toInt()
    private val TOOLBAR_LONG_CLICK_LISTENER = OnLongClickListener { v: View ->
        showToastOverView(v, v.contentDescription, LENGTH_DEFAULT)
        true
    }

    @JvmStatic
    fun showError(activity: Activity, e: Throwable?) {
        val error = ThrowableUtil.getAppError(activity, e!!)
        makeSnackbar(activity, error.error, LENGTH_DEFAULT).show()
    }

    @JvmStatic
    fun showMessageAsPlainText(activity: Activity, possibleHtml: CharSequence) {
        val richText: CharSequence = StringUtil.fromHtml(possibleHtml.toString())
        showMessage(activity, richText.toString())
    }

    @JvmStatic
    fun showMessage(fragment: Fragment, @StringRes text: Int) {
        makeSnackbar(fragment.requireActivity(), fragment.getString(text), Snackbar.LENGTH_LONG).show()
    }

    @JvmStatic
    fun showMessage(fragment: Fragment, text: String) {
        makeSnackbar(fragment.requireActivity(), text, Snackbar.LENGTH_LONG).show()
    }

    @JvmStatic
    fun showMessage(activity: Activity, @StringRes resId: Int) {
        showMessage(activity, activity.getString(resId), Snackbar.LENGTH_LONG)
    }

    fun showMessage(activity: Activity, @StringRes resId: Int, duration: Int) {
        showMessage(activity, activity.getString(resId), duration)
    }

    @JvmOverloads
    @JvmStatic
    fun showMessage(activity: Activity, text: CharSequence, duration: Int = Snackbar.LENGTH_LONG) {
        makeSnackbar(activity, text, duration).show()
    }

    @JvmStatic
    fun showPrivacyPolicy(context: Context) {
        UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(R.string.privacy_policy_url)))
    }

    @JvmStatic
    fun showOfflineReadingAndData(context: Context) {
        UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(R.string.offline_reading_and_data_url)))
    }

    @JvmStatic
    fun showAboutWikipedia(context: Context) {
        UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(R.string.about_wikipedia_url)))
    }

    @JvmStatic
    fun showAndroidAppFAQ(context: Context) {
        UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(R.string.android_app_faq_url)))
    }

    @JvmStatic
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

    @JvmOverloads
    @JvmStatic
    fun showAndroidAppEditingFAQ(context: Context,
                                 @StringRes urlStr: Int = R.string.android_app_edit_help_url) {
        SuggestedEditsFunnel.get().helpOpened()
        UriUtil.visitInExternalBrowser(context, Uri.parse(context.getString(urlStr)))
    }

    @JvmStatic
    fun showProtectionStatusMessage(activity: Activity, status: String?) {
        if (TextUtils.isEmpty(status)) {
            return
        }
        val message: String = when (status) {
            "sysop" -> activity.getString(R.string.page_protected_sysop)
            "autoconfirmed" -> activity.getString(R.string.page_protected_autoconfirmed)
            else -> activity.getString(R.string.page_protected_other, status)
        }
        showMessage(activity, message)
    }

    @JvmStatic
    fun setButtonLongPressToast(vararg views: View) {
        for (v in views) {
            v.setOnLongClickListener(TOOLBAR_LONG_CLICK_LISTENER)
        }
    }

    @JvmStatic
    fun makeSnackbar(activity: Activity, text: CharSequence, duration: Int): Snackbar {
        val view = findBestView(activity)
        val snackbar = Snackbar.make(view, StringUtil.fromHtml(text.toString()), duration)
        val textView = snackbar.view.findViewById<TextView>(R.id.snackbar_text)
        textView.movementMethod = LinkMovementMethod.getInstance()
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

    @JvmStatic
    fun showTooltip(activity: Activity, anchor: View, text: CharSequence, aboveOrBelow: Boolean, autoDismiss: Boolean): Balloon {
        return showTooltip(activity, getTooltip(anchor.context, text, autoDismiss), anchor, aboveOrBelow, autoDismiss)
    }

    @JvmStatic
    fun showTooltip(activity: Activity, anchor: View, @LayoutRes layoutRes: Int,
                    arrowAnchorPadding: Int, topOrBottomMargin: Int, aboveOrBelow: Boolean, autoDismiss: Boolean): Balloon {
        return showTooltip(activity, getTooltip(anchor.context, layoutRes, arrowAnchorPadding, topOrBottomMargin, aboveOrBelow, autoDismiss), anchor, aboveOrBelow, autoDismiss)
    }

    private fun showTooltip(activity: Activity, balloon: Balloon, anchor: View, aboveOrBelow: Boolean, autoDismiss: Boolean): Balloon {
        if (aboveOrBelow) {
            balloon.showAlignTop(anchor, 0, roundedDpToPx(8f))
        } else {
            balloon.showAlignBottom(anchor, 0, -roundedDpToPx(8f))
        }
        if (!autoDismiss) {
            (activity as BaseActivity).setCurrentTooltip(balloon)
        }
        return balloon
    }

    fun getTooltip(context: Context, text: CharSequence, autoDismiss: Boolean): Balloon {
        return createBalloon(context) {
            setArrowDrawableResource(R.drawable.ic_tooltip_arrow_up)
            setArrowPositionRules(ArrowPositionRules.ALIGN_ANCHOR)
            setArrowOrientationRules(ArrowOrientationRules.ALIGN_ANCHOR)
            setArrowSize(24)
            setMarginLeft(8)
            setMarginRight(8)
            setBackgroundColorResource(ResourceUtil.getThemedAttributeId(context, R.attr.colorAccent))
            setDismissWhenTouchOutside(autoDismiss)
            setText(text)
            setTextSize(14f)
            setTextTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL))
            setTextColor(Color.WHITE)
            setPadding(16)
        }
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
        return when (activity) {
            is MainActivity -> {
                activity.findViewById(R.id.fragment_main_coordinator)
            }
            is PageActivity -> {
                activity.findViewById(R.id.fragment_page_coordinator)
            }
            is RandomActivity -> {
                activity.findViewById(R.id.random_coordinator_layout)
            }
            is ReadingListActivity -> {
                activity.findViewById(R.id.fragment_reading_list_coordinator)
            }
            is SuggestionsActivity -> {
                activity.findViewById(R.id.suggestedEditsCardsCoordinator)
            }
            else -> {
                activity.findViewById(android.R.id.content)
            }
        }
    }
}
