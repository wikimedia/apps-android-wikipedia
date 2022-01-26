package org.wikipedia.theme

import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.button.MaterialButton
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.Consumer
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.analytics.AppearanceChangeFunnel
import org.wikipedia.databinding.DialogThemeChooserBinding
import org.wikipedia.events.WebViewInvalidateEvent
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.page.customize.CustomizeToolbarActivity
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DeviceUtil
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil

class ThemeChooserDialog : ExtendedBottomSheetDialogFragment() {
    private var _binding: DialogThemeChooserBinding? = null
    private val binding get() = _binding!!

    interface Callback {
        fun onToggleDimImages()
        fun onToggleReadingFocusMode()
        fun onCancelThemeChooser()
    }

    private enum class FontSizeAction {
        INCREASE, DECREASE, RESET
    }

    private var app = WikipediaApp.getInstance()
    private lateinit var funnel: AppearanceChangeFunnel
    private lateinit var invokeSource: InvokeSource
    private var isMobileWeb: Boolean = false
    private val disposables = CompositeDisposable()
    private var updatingFont = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogThemeChooserBinding.inflate(inflater, container, false)
        binding.buttonDecreaseTextSize.setOnClickListener(FontSizeButtonListener(FontSizeAction.DECREASE))
        binding.buttonIncreaseTextSize.setOnClickListener(FontSizeButtonListener(FontSizeAction.INCREASE))
        FeedbackUtil.setButtonLongPressToast(binding.buttonDecreaseTextSize, binding.buttonIncreaseTextSize)
        binding.buttonThemeLight.setOnClickListener(ThemeButtonListener(Theme.LIGHT))
        binding.buttonThemeDark.setOnClickListener(ThemeButtonListener(Theme.DARK))
        binding.buttonThemeBlack.setOnClickListener(ThemeButtonListener(Theme.BLACK))
        binding.buttonThemeSepia.setOnClickListener(ThemeButtonListener(Theme.SEPIA))
        binding.buttonFontFamilySansSerif.setOnClickListener(FontFamilyListener())
        binding.buttonFontFamilySerif.setOnClickListener(FontFamilyListener())
        binding.themeChooserDarkModeDimImagesSwitch.setOnCheckedChangeListener { _, b -> onToggleDimImages(b) }
        binding.themeChooserMatchSystemThemeSwitch.setOnCheckedChangeListener { _, b -> onToggleMatchSystemTheme(b) }
        binding.themeChooserReadingFocusModeSwitch.setOnCheckedChangeListener { _, b -> onToggleReadingFocusMode(b) }
        binding.textSizeSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, value: Int, fromUser: Boolean) {
                if (!fromUser) {
                    return
                }
                val currentMultiplier = Prefs.textSizeMultiplier
                val changed = app.setFontSizeMultiplier(binding.textSizeSeekBar.value)
                if (changed) {
                    updatingFont = true
                    updateFontSize()
                    funnel.logFontSizeChange(currentMultiplier.toFloat(), Prefs.textSizeMultiplier.toFloat())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        updateComponents()
        disableBackgroundDim()
        requireDialog().window?.let {
            DeviceUtil.setNavigationBarColor(it, ResourceUtil.getThemedColor(requireContext(), R.attr.paper_color))
        }

        // TODO: test only
        binding.customizeFavorites.setOnClickListener {
            startActivity(CustomizeToolbarActivity.newIntent(requireContext()))
        }

        disposables.add(WikipediaApp.getInstance().bus.subscribe(EventBusConsumer()))
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        BottomSheetBehavior.from(requireView().parent as View).peekHeight = DimenUtil.roundedDpToPx(DimenUtil.getDimension(R.dimen.themeChooserSheetPeekHeight))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        invokeSource = requireArguments().getSerializable(Constants.INTENT_EXTRA_INVOKE_SOURCE) as InvokeSource
        isMobileWeb = requireArguments().getBoolean(EXTRA_IS_MOBILE_WEB)
        funnel = AppearanceChangeFunnel(app, app.wikiSite, invokeSource)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        disposables.clear()
        _binding = null
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        callback()?.onCancelThemeChooser()
    }

    private fun disableButtonsOnMobileWeb() {
        binding.textSizeSeekBar.isEnabled = !isMobileWeb
        binding.buttonDecreaseTextSize.isEnabled = !isMobileWeb
        binding.buttonIncreaseTextSize.isEnabled = !isMobileWeb
        binding.buttonFontFamilySerif.isEnabled = !isMobileWeb
        binding.buttonFontFamilySansSerif.isEnabled = !isMobileWeb
        binding.themeChooserMatchSystemThemeSwitch.isEnabled = !isMobileWeb
        binding.themeChooserDarkModeDimImagesSwitch.isEnabled = !isMobileWeb && binding.themeChooserDarkModeDimImagesSwitch.isEnabled
        binding.themeChooserReadingFocusModeSwitch.isEnabled = !isMobileWeb
        binding.buttonThemeBlack.isEnabled = binding.buttonThemeBlack.isEnabled && (app.currentTheme == Theme.BLACK || !isMobileWeb)
        binding.buttonThemeDark.isEnabled = binding.buttonThemeDark.isEnabled && (app.currentTheme == Theme.DARK || !isMobileWeb)
        binding.buttonThemeLight.isEnabled = app.currentTheme == Theme.LIGHT || !isMobileWeb
        binding.buttonThemeSepia.isEnabled = app.currentTheme == Theme.SEPIA || !isMobileWeb

        if (isMobileWeb) {
            val textColor = ResourceUtil.getThemedColor(requireContext(), R.attr.color_group_61)
            binding.buttonDecreaseTextSize.setTextColor(textColor)
            binding.buttonIncreaseTextSize.setTextColor(textColor)
            binding.buttonFontFamilySerif.setTextColor(textColor)
            binding.buttonFontFamilySerif.setTextColor(textColor)
            binding.themeChooserMatchSystemThemeSwitch.setTextColor(textColor)
            binding.themeChooserDarkModeDimImagesSwitch.setTextColor(textColor)
            binding.themeChooserReadingFocusModeSwitch.setTextColor(textColor)
            binding.themeChooserReadingFocusModeDescription.setTextColor(textColor)
            updateThemeButtonAlpha(binding.buttonThemeBlack, !binding.buttonThemeBlack.isEnabled)
            updateThemeButtonAlpha(binding.buttonThemeDark, !binding.buttonThemeDark.isEnabled)
            updateThemeButtonAlpha(binding.buttonThemeLight, !binding.buttonThemeLight.isEnabled)
            updateThemeButtonAlpha(binding.buttonThemeSepia, !binding.buttonThemeSepia.isEnabled)
        }
    }

    private fun onToggleDimImages(enabled: Boolean) {
        if (enabled == Prefs.dimDarkModeImages) {
            return
        }
        Prefs.dimDarkModeImages = enabled
        callback()?.onToggleDimImages()
    }

    private fun onToggleMatchSystemTheme(enabled: Boolean) {
        if (enabled == Prefs.shouldMatchSystemTheme) {
            return
        }
        Prefs.shouldMatchSystemTheme = enabled
        val currentTheme = app.currentTheme
        if (isMatchingSystemThemeEnabled) {
            when (app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> if (!app.currentTheme.isDark) {
                    app.currentTheme = if (!app.unmarshalTheme(Prefs.previousThemeId).isDark) Theme.BLACK else app.unmarshalTheme(Prefs.previousThemeId)
                    Prefs.previousThemeId = currentTheme.marshallingId
                }
                Configuration.UI_MODE_NIGHT_NO -> if (app.currentTheme.isDark) {
                    app.currentTheme = if (app.unmarshalTheme(Prefs.previousThemeId).isDark) Theme.LIGHT else app.unmarshalTheme(Prefs.previousThemeId)
                    Prefs.previousThemeId = currentTheme.marshallingId
                }
            }
        }
        conditionallyDisableThemeButtons()
    }

    private fun onToggleReadingFocusMode(enabled: Boolean) {
        Prefs.readingFocusModeEnabled = enabled
        funnel.logReadingFocusMode(enabled)
        callback()?.onToggleReadingFocusMode()
    }

    private fun conditionallyDisableThemeButtons() {
        updateThemeButtonAlpha(binding.buttonThemeLight, isMatchingSystemThemeEnabled && app.currentTheme.isDark)
        updateThemeButtonAlpha(binding.buttonThemeSepia, isMatchingSystemThemeEnabled && app.currentTheme.isDark)
        updateThemeButtonAlpha(binding.buttonThemeDark, isMatchingSystemThemeEnabled && !app.currentTheme.isDark)
        updateThemeButtonAlpha(binding.buttonThemeBlack, isMatchingSystemThemeEnabled && !app.currentTheme.isDark)
        binding.buttonThemeLight.isEnabled = !isMatchingSystemThemeEnabled || !app.currentTheme.isDark
        binding.buttonThemeSepia.isEnabled = !isMatchingSystemThemeEnabled || !app.currentTheme.isDark
        binding.buttonThemeDark.isEnabled = !isMatchingSystemThemeEnabled || app.currentTheme.isDark
        binding.buttonThemeBlack.isEnabled = !isMatchingSystemThemeEnabled || app.currentTheme.isDark
    }

    private fun updateThemeButtonAlpha(button: View, translucent: Boolean) {
        button.alpha = if (translucent) 0.2f else 1.0f
    }

    private val isMatchingSystemThemeEnabled: Boolean
        get() = Prefs.shouldMatchSystemTheme && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private fun updateComponents() {
        updateFontSize()
        updateFontFamily()
        updateThemeButtons()
        updateDimImagesSwitch()
        updateMatchSystemThemeSwitch()
        disableButtonsOnMobileWeb()

        binding.themeChooserReadingFocusModeSwitch.isChecked = Prefs.readingFocusModeEnabled
    }

    private fun updateMatchSystemThemeSwitch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            binding.themeChooserMatchSystemThemeSwitch.visibility = View.VISIBLE
            binding.themeChooserMatchSystemThemeSwitch.isChecked = Prefs.shouldMatchSystemTheme
            conditionallyDisableThemeButtons()
        } else {
            binding.themeChooserMatchSystemThemeSwitch.visibility = View.GONE
        }
    }

    private fun updateFontSize() {
        val multiplier = Prefs.textSizeMultiplier
        binding.textSizeSeekBar.value = multiplier
        val percentStr = getString(R.string.text_size_percent,
                (100 * (1 + multiplier * DimenUtil.getFloat(R.dimen.textSizeMultiplierFactor))).toInt())
        binding.textSizePercent.text = if (multiplier == 0) getString(R.string.text_size_percent_default, percentStr) else percentStr
        if (updatingFont) {
            binding.fontChangeProgressBar.visibility = View.VISIBLE
        } else {
            binding.fontChangeProgressBar.visibility = View.GONE
        }
    }

    private fun updateFontFamily() {
        binding.buttonFontFamilySansSerif.strokeWidth = if (Prefs.fontFamily == binding.buttonFontFamilySansSerif.tag) BUTTON_STROKE_WIDTH else 0
        binding.buttonFontFamilySerif.strokeWidth = if (Prefs.fontFamily == binding.buttonFontFamilySerif.tag) BUTTON_STROKE_WIDTH else 0
    }

    private fun updateThemeButtons() {
        updateThemeButtonStroke(binding.buttonThemeLight, app.currentTheme === Theme.LIGHT)
        updateThemeButtonStroke(binding.buttonThemeSepia, app.currentTheme === Theme.SEPIA)
        updateThemeButtonStroke(binding.buttonThemeDark, app.currentTheme === Theme.DARK)
        updateThemeButtonStroke(binding.buttonThemeBlack, app.currentTheme === Theme.BLACK)
    }

    private fun updateThemeButtonStroke(button: MaterialButton, selected: Boolean) {
        button.strokeWidth = if (selected) BUTTON_STROKE_WIDTH else 0
        button.isClickable = !selected
    }

    private fun updateDimImagesSwitch() {
        binding.themeChooserDarkModeDimImagesSwitch.isChecked = Prefs.dimDarkModeImages
        binding.themeChooserDarkModeDimImagesSwitch.isEnabled = app.currentTheme.isDark
        binding.themeChooserDarkModeDimImagesSwitch.setTextColor(if (binding.themeChooserDarkModeDimImagesSwitch.isEnabled)
            ResourceUtil.getThemedColor(requireContext(), R.attr.section_title_color) else ContextCompat.getColor(requireContext(), R.color.black26))
    }

    private inner class ThemeButtonListener(private val theme: Theme) : View.OnClickListener {
        override fun onClick(v: View) {
            if (app.currentTheme !== theme) {
                funnel.logThemeChange(app.currentTheme, theme)
                app.currentTheme = theme
            }
        }
    }

    private inner class FontFamilyListener : View.OnClickListener {
        override fun onClick(v: View) {
            if (v.tag != null) {
                val newFontFamily = v.tag as String
                funnel.logFontThemeChange(Prefs.fontFamily, newFontFamily)
                app.setFontFamily(newFontFamily)
            }
        }
    }

    private inner class FontSizeButtonListener(private val action: FontSizeAction) : View.OnClickListener {
        override fun onClick(view: View) {
            val currentMultiplier = Prefs.textSizeMultiplier
            val changed = when (action) {
                FontSizeAction.INCREASE -> {
                    app.setFontSizeMultiplier(Prefs.textSizeMultiplier + 1)
                }
                FontSizeAction.DECREASE -> {
                    app.setFontSizeMultiplier(Prefs.textSizeMultiplier - 1)
                }
                FontSizeAction.RESET -> {
                    app.setFontSizeMultiplier(0)
                }
            }
            if (changed) {
                updatingFont = true
                updateFontSize()
                funnel.logFontSizeChange(currentMultiplier.toFloat(), Prefs.textSizeMultiplier.toFloat())
            }
        }
    }

    private inner class EventBusConsumer : Consumer<Any> {
        override fun accept(event: Any?) {
            if (event is WebViewInvalidateEvent) {
                updatingFont = false
                updateComponents()
            }
        }
    }

    fun callback(): Callback? {
        return FragmentUtil.getCallback(this, Callback::class.java)
    }

    companion object {
        private const val EXTRA_IS_MOBILE_WEB = "isMobileWeb"
        private val BUTTON_STROKE_WIDTH = DimenUtil.roundedDpToPx(2f)

        fun newInstance(source: InvokeSource, isMobileWeb: Boolean = false): ThemeChooserDialog {
            return ThemeChooserDialog().apply {
                arguments = bundleOf(Constants.INTENT_EXTRA_INVOKE_SOURCE to source,
                    EXTRA_IS_MOBILE_WEB to isMobileWeb)
            }
        }
    }
}
