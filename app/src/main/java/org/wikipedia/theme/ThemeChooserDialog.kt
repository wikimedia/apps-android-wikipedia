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
import org.wikipedia.settings.Prefs
import org.wikipedia.util.DimenUtil.getDimension
import org.wikipedia.util.DimenUtil.getFloat
import org.wikipedia.util.DimenUtil.roundedDpToPx
import org.wikipedia.util.FeedbackUtil.setButtonLongPressToast
import org.wikipedia.util.ResourceUtil.getThemedColor

class ThemeChooserDialog : ExtendedBottomSheetDialogFragment() {
    private var _binding: DialogThemeChooserBinding? = null
    private val binding get() = _binding!!

    interface Callback {
        fun onToggleDimImages()
        fun onCancelThemeChooser()
    }

    private enum class FontSizeAction {
        INCREASE, DECREASE, RESET
    }

    private var app = WikipediaApp.getInstance()
    private lateinit var funnel: AppearanceChangeFunnel
    private lateinit var invokeSource: InvokeSource
    private val disposables = CompositeDisposable()
    private var updatingFont = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogThemeChooserBinding.inflate(inflater, container, false)
        binding.buttonDecreaseTextSize.setOnClickListener(FontSizeButtonListener(FontSizeAction.DECREASE))
        binding.buttonIncreaseTextSize.setOnClickListener(FontSizeButtonListener(FontSizeAction.INCREASE))
        setButtonLongPressToast(binding.buttonDecreaseTextSize, binding.buttonIncreaseTextSize)
        binding.buttonThemeLight.setOnClickListener(ThemeButtonListener(Theme.LIGHT))
        binding.buttonThemeDark.setOnClickListener(ThemeButtonListener(Theme.DARK))
        binding.buttonThemeBlack.setOnClickListener(ThemeButtonListener(Theme.BLACK))
        binding.buttonThemeSepia.setOnClickListener(ThemeButtonListener(Theme.SEPIA))
        binding.buttonFontFamilySansSerif.setOnClickListener(FontFamilyListener())
        binding.buttonFontFamilySerif.setOnClickListener(FontFamilyListener())
        binding.themeChooserDarkModeDimImagesSwitch.setOnCheckedChangeListener { _, b -> onToggleDimImages(b) }
        binding.themeChooserMatchSystemThemeSwitch.setOnCheckedChangeListener { _, b -> onToggleMatchSystemTheme(b) }
        binding.textSizeSeekBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, value: Int, fromUser: Boolean) {
                if (!fromUser) {
                    return
                }
                val currentMultiplier = Prefs.getTextSizeMultiplier()
                val changed = app.setFontSizeMultiplier(binding.textSizeSeekBar.value)
                if (changed) {
                    updatingFont = true
                    updateFontSize()
                    funnel.logFontSizeChange(currentMultiplier.toFloat(), Prefs.getTextSizeMultiplier().toFloat())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        updateComponents()
        disableBackgroundDim()
        setNavigationBarColor(getThemedColor(requireContext(), R.attr.paper_color))

        disposables.add(WikipediaApp.getInstance().bus.subscribe(EventBusConsumer()))
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        BottomSheetBehavior.from(requireView().parent as View).peekHeight = roundedDpToPx(getDimension(R.dimen.themeChooserSheetPeekHeight))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        invokeSource = requireArguments().getSerializable(Constants.INTENT_EXTRA_INVOKE_SOURCE) as InvokeSource
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

    private fun onToggleDimImages(enabled: Boolean) {
        if (enabled == Prefs.shouldDimDarkModeImages()) {
            return
        }
        Prefs.setDimDarkModeImages(enabled)
        callback()?.onToggleDimImages()
    }

    private fun onToggleMatchSystemTheme(enabled: Boolean) {
        if (enabled == Prefs.shouldMatchSystemTheme()) {
            return
        }
        Prefs.setMatchSystemTheme(enabled)
        val currentTheme = app.currentTheme
        if (isMatchingSystemThemeEnabled) {
            when (app.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
                Configuration.UI_MODE_NIGHT_YES -> if (!app.currentTheme.isDark) {
                    app.currentTheme = if (!app.unmarshalTheme(Prefs.getPreviousThemeId()).isDark) Theme.BLACK else app.unmarshalTheme(Prefs.getPreviousThemeId())
                    Prefs.setPreviousThemeId(currentTheme.marshallingId)
                }
                Configuration.UI_MODE_NIGHT_NO -> if (app.currentTheme.isDark) {
                    app.currentTheme = if (app.unmarshalTheme(Prefs.getPreviousThemeId()).isDark) Theme.LIGHT else app.unmarshalTheme(Prefs.getPreviousThemeId())
                    Prefs.setPreviousThemeId(currentTheme.marshallingId)
                }
            }
        }
        conditionallyDisableThemeButtons()
    }

    private fun conditionallyDisableThemeButtons() {
        binding.buttonThemeLight.alpha = if (isMatchingSystemThemeEnabled && app.currentTheme.isDark) 0.2f else 1.0f
        binding.buttonThemeSepia.alpha = if (isMatchingSystemThemeEnabled && app.currentTheme.isDark) 0.2f else 1.0f
        binding.buttonThemeDark.alpha = if (isMatchingSystemThemeEnabled && !app.currentTheme.isDark) 0.2f else 1.0f
        binding.buttonThemeBlack.alpha = if (isMatchingSystemThemeEnabled && !app.currentTheme.isDark) 0.2f else 1.0f
        binding.buttonThemeLight.isEnabled = !isMatchingSystemThemeEnabled || !app.currentTheme.isDark
        binding.buttonThemeSepia.isEnabled = !isMatchingSystemThemeEnabled || !app.currentTheme.isDark
        binding.buttonThemeDark.isEnabled = !isMatchingSystemThemeEnabled || app.currentTheme.isDark
        binding.buttonThemeBlack.isEnabled = !isMatchingSystemThemeEnabled || app.currentTheme.isDark
    }

    private val isMatchingSystemThemeEnabled: Boolean
        get() = Prefs.shouldMatchSystemTheme() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private fun updateComponents() {
        updateFontSize()
        updateFontFamily()
        updateThemeButtons()
        updateDimImagesSwitch()
        updateMatchSystemThemeSwitch()
    }

    private fun updateMatchSystemThemeSwitch() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            binding.themeChooserMatchSystemThemeSwitch.visibility = View.VISIBLE
            binding.themeChooserMatchSystemThemeSwitch.isChecked = Prefs.shouldMatchSystemTheme()
            conditionallyDisableThemeButtons()
        } else {
            binding.themeChooserMatchSystemThemeSwitch.visibility = View.GONE
        }
    }

    private fun updateFontSize() {
        val multiplier = Prefs.getTextSizeMultiplier()
        binding.textSizeSeekBar.value = multiplier
        val percentStr = getString(R.string.text_size_percent,
                (100 * (1 + multiplier * getFloat(R.dimen.textSizeMultiplierFactor))).toInt())
        binding.textSizePercent.text = if (multiplier == 0) getString(R.string.text_size_percent_default, percentStr) else percentStr
        if (updatingFont) {
            binding.fontChangeProgressBar.visibility = View.VISIBLE
        } else {
            binding.fontChangeProgressBar.visibility = View.GONE
        }
    }

    private fun updateFontFamily() {
        binding.buttonFontFamilySansSerif.strokeWidth = if (Prefs.getFontFamily() == binding.buttonFontFamilySansSerif.tag) BUTTON_STROKE_WIDTH else 0
        binding.buttonFontFamilySerif.strokeWidth = if (Prefs.getFontFamily() == binding.buttonFontFamilySerif.tag) BUTTON_STROKE_WIDTH else 0
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
        binding.themeChooserDarkModeDimImagesSwitch.isChecked = Prefs.shouldDimDarkModeImages()
        binding.themeChooserDarkModeDimImagesSwitch.isEnabled = app.currentTheme.isDark
        binding.themeChooserDarkModeDimImagesSwitch.setTextColor(if (binding.themeChooserDarkModeDimImagesSwitch.isEnabled)
            getThemedColor(requireContext(), R.attr.section_title_color) else ContextCompat.getColor(requireContext(), R.color.black26))
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
                funnel.logFontThemeChange(Prefs.getFontFamily(), newFontFamily)
                app.setFontFamily(newFontFamily)
            }
        }
    }

    private inner class FontSizeButtonListener(private val action: FontSizeAction) : View.OnClickListener {
        override fun onClick(view: View) {
            val currentMultiplier = Prefs.getTextSizeMultiplier()
            val changed = when (action) {
                FontSizeAction.INCREASE -> {
                    app.setFontSizeMultiplier(Prefs.getTextSizeMultiplier() + 1)
                }
                FontSizeAction.DECREASE -> {
                    app.setFontSizeMultiplier(Prefs.getTextSizeMultiplier() - 1)
                }
                FontSizeAction.RESET -> {
                    app.setFontSizeMultiplier(0)
                }
            }
            if (changed) {
                updatingFont = true
                updateFontSize()
                funnel.logFontSizeChange(currentMultiplier.toFloat(), Prefs.getTextSizeMultiplier().toFloat())
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
        private val BUTTON_STROKE_WIDTH = roundedDpToPx(2f)

        @JvmStatic
        fun newInstance(source: InvokeSource): ThemeChooserDialog {
            return ThemeChooserDialog().apply {
                arguments = bundleOf(Constants.INTENT_EXTRA_INVOKE_SOURCE to source)
            }
        }
    }
}
