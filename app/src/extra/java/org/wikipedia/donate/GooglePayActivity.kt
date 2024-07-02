package org.wikipedia.donate

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.activity.viewModels
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.button.ButtonConstants
import com.google.android.gms.wallet.button.ButtonOptions
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.activity.BaseActivity
import org.wikipedia.analytics.eventplatform.DonorExperienceEvent
import org.wikipedia.databinding.ActivityDonateBinding
import org.wikipedia.dataclient.donate.DonationConfig
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.StringUtil
import org.wikipedia.util.UriUtil
import kotlin.math.max

class GooglePayActivity : BaseActivity() {
    private lateinit var binding: ActivityDonateBinding
    private lateinit var paymentsClient: PaymentsClient

    private val viewModel: GooglePayViewModel by viewModels()

    private var shouldWatchText = true
    private var typedManually = false

    private val transactionFee get() = max(getAmountFloat(binding.donateAmountText.text.toString()) * GooglePayComponent.TRANSACTION_FEE_PERCENTAGE, viewModel.transactionFee)

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDonateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = ""

        binding.donateAmountInput.prefixText = viewModel.currencySymbol

        paymentsClient = GooglePayComponent.createPaymentsClient(this)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                launch {
                    viewModel.uiState.collect { resource ->
                        when (resource) {
                            is Resource.Loading -> {
                                setLoadingState()
                            }
                            is Resource.Error -> {
                                DonorExperienceEvent.logAction("error_other", "gpay")
                                setErrorState(resource.throwable)
                            }
                            is GooglePayViewModel.NoPaymentMethod -> {
                                DonorExperienceEvent.logAction("no_payment_method", "gpay")
                                DonateDialog.launchDonateLink(this@GooglePayActivity, intent.getStringExtra(DonateDialog.ARG_DONATE_URL))
                                finish()
                            }
                            is Resource.Success -> {
                                DonorExperienceEvent.logAction("impression", "googlepay_initiated")
                                onContentsReceived(resource.data)
                            }
                            is GooglePayViewModel.DonateSuccess -> {
                                DonorExperienceEvent.logAction("impression", "gpay_processed", campaignId = intent.getStringExtra(DonateDialog.ARG_CAMPAIGN_ID).orEmpty())
                                setResult(RESULT_OK)
                                finish()
                            }
                        }
                    }
                }
            }
        }

        binding.errorView.backClickListener = View.OnClickListener {
            onBackPressed()
        }

        binding.payButton.setOnClickListener {
            val amountText = binding.donateAmountText.text.toString()
            if (!validateInput(amountText)) {
                return@setOnClickListener
            }

            var totalAmount = getAmountFloat(amountText)
            if (binding.checkBoxTransactionFee.isChecked) {
                totalAmount += transactionFee
            }

            viewModel.finalAmount = totalAmount

            if (typedManually) {
                DonorExperienceEvent.logAction("amount_entered", "gpay")
            }
            DonorExperienceEvent.submit("donate_confirm_click", "gpay",
                "add_transaction: ${binding.checkBoxTransactionFee.isChecked}, recurring: ${binding.checkBoxRecurring.isChecked}, email_subscribe: ${binding.checkBoxAllowEmail.isChecked}")

            AutoResolveHelper.resolveTask(
                paymentsClient.loadPaymentData(viewModel.getPaymentDataRequest()),
                this, LOAD_PAYMENT_DATA_REQUEST_CODE
            )
        }

        binding.donateAmountText.addTextChangedListener { text ->
            validateInput(text.toString())
            if (!shouldWatchText) {
                return@addTextChangedListener
            }
            val buttonToHighlight = binding.amountPresetsContainer.children.firstOrNull { child ->
                if (child is MaterialButton) {
                    val amount = getAmountFloat(text.toString())
                    child.tag == amount
                } else {
                    false
                }
            }
            typedManually = true
            setButtonHighlighted(buttonToHighlight)
        }

        binding.linkProblemsDonating.setOnClickListener {
            DonorExperienceEvent.logAction("report_problem_click", "gpay")
            UriUtil.visitInExternalBrowser(this, Uri.parse(getString(R.string.donate_problems_url)))
        }
        binding.linkOtherWays.setOnClickListener {
            DonorExperienceEvent.logAction("other_give_click", "gpay")
            UriUtil.visitInExternalBrowser(this, Uri.parse(getString(R.string.donate_other_ways_url)))
        }
        binding.linkFAQ.setOnClickListener {
            DonorExperienceEvent.logAction("faq_click", "gpay")
            UriUtil.visitInExternalBrowser(this, Uri.parse(getString(R.string.donate_faq_url)))
        }
        binding.linkTaxDeduct.setOnClickListener {
            DonorExperienceEvent.logAction("taxinfo_click", "gpay")
            UriUtil.visitInExternalBrowser(this, Uri.parse(getString(R.string.donate_tax_url)))
        }
        binding.disclaimerText1.movementMethod = LinkMovementMethod.getInstance()
        binding.disclaimerText2.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun validateInput(text: String): Boolean {
        val amount = getAmountFloat(text)
        val min = viewModel.minimumAmount
        val max = viewModel.maximumAmount

        updateTransactionFee()

        if (amount <= 0f || amount < min) {
            binding.donateAmountInput.error = getString(R.string.donate_gpay_minimum_amount, viewModel.currencyFormat.format(min))
            DonorExperienceEvent.submit("submission_error", "gpay", "error_reason: min_amount")
            return false
        } else if (max > 0f && amount > max) {
            binding.donateAmountInput.error = getString(R.string.donate_gpay_maximum_amount, viewModel.currencyFormat.format(max))
            DonorExperienceEvent.submit("submission_error", "gpay", "error_reason: max_amount")
            return false
        } else {
            binding.donateAmountInput.isErrorEnabled = false
        }
        return true
    }

    private fun setLoadingState() {
        binding.contentsContainer.isVisible = false
        binding.errorView.isVisible = false
        binding.progressBar.isVisible = true
    }

    private fun setErrorState(throwable: Throwable) {
        binding.contentsContainer.isVisible = false
        binding.progressBar.isVisible = false
        binding.errorView.isVisible = true
        binding.errorView.setError(throwable)
    }

    private fun onContentsReceived(donationConfig: DonationConfig) {
        binding.contentsContainer.isVisible = true
        binding.progressBar.isVisible = false
        binding.errorView.isVisible = false

        binding.checkBoxAllowEmail.isVisible = viewModel.emailOptInRequired

        updateTransactionFee()

        binding.disclaimerText1.text = StringUtil.fromHtml(viewModel.disclaimerInformationSharing)
        binding.disclaimerText2.text = StringUtil.fromHtml(viewModel.disclaimerMonthlyCancel)

        val methods = JSONArray().put(GooglePayComponent.baseCardPaymentMethod)
        binding.payButton.initialize(ButtonOptions.newBuilder()
            .setButtonTheme(if (WikipediaApp.instance.currentTheme.isDark) ButtonConstants.ButtonTheme.DARK else ButtonConstants.ButtonTheme.LIGHT)
            .setButtonType(ButtonConstants.ButtonType.DONATE)
            .setAllowedPaymentMethods(methods.toString())
            .build())

        val viewIds = mutableListOf<Int>()
        val presets = donationConfig.currencyAmountPresets[viewModel.currencyCode]
        presets?.forEach { amount ->
            val viewId = View.generateViewId()
            viewIds.add(viewId)
            val button = MaterialButton(this)
            button.text = viewModel.currencyFormat.format(amount)
            button.id = viewId
            button.tag = amount
            binding.amountPresetsContainer.addView(button)
            button.setOnClickListener {
                setButtonHighlighted(it)
                setAmountText(it.tag as Float)
                DonorExperienceEvent.logAction("amount_selected", "gpay")
            }
        }
        binding.amountPresetsFlow.referencedIds = viewIds.toIntArray()
        setButtonHighlighted()
    }

    private fun setButtonHighlighted(button: View? = null) {
        binding.amountPresetsContainer.children.forEach { child ->
            if (child is MaterialButton) {
                if (child == button) {
                    child.backgroundTintList = ResourceUtil.getThemedColorStateList(this, R.attr.progressive_color)
                    child.setTextColor(Color.WHITE)
                } else {
                    child.backgroundTintList = ResourceUtil.getThemedColorStateList(this, R.attr.background_color)
                    child.setTextColor(ResourceUtil.getThemedColor(this, R.attr.primary_color))
                }
            }
        }
    }

    private fun updateTransactionFee() {
        binding.checkBoxTransactionFee.text = getString(R.string.donate_gpay_check_transaction_fee,
            viewModel.currencyFormat.format(transactionFee))
    }

    private fun getAmountFloat(text: String): Float {
        var result: Float?
        result = text.toFloatOrNull()
        if (result == null) {
            val text2 = if (text.contains(".")) text.replace(".", ",") else text.replace(",", ".")
            result = text2.toFloatOrNull()
        }
        return result ?: 0f
    }

    private fun setAmountText(amount: Float) {
        shouldWatchText = false
        binding.donateAmountText.setText(viewModel.decimalFormat.format(amount))
        shouldWatchText = true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == LOAD_PAYMENT_DATA_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    data?.let { intent ->
                        PaymentData.getFromIntent(intent)?.let { paymentData ->
                            viewModel.submit(paymentData,
                                binding.checkBoxTransactionFee.isChecked,
                                binding.checkBoxRecurring.isChecked,
                                if (viewModel.emailOptInRequired) binding.checkBoxAllowEmail.isChecked else true,
                                intent.getStringExtra(DonateDialog.ARG_CAMPAIGN_ID).orEmpty().ifEmpty { CAMPAIGN_ID_APP_MENU })
                        }
                    }
                }
                Activity.RESULT_CANCELED -> {
                    // The user cancelled the payment attempt
                }
                AutoResolveHelper.RESULT_ERROR -> {
                    AutoResolveHelper.getStatusFromIntent(data)?.let {
                        it.statusMessage?.let { message ->
                            FeedbackUtil.showMessage(this, message)
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val LOAD_PAYMENT_DATA_REQUEST_CODE = 42
        private const val CAMPAIGN_ID_APP_MENU = "appmenu"

        fun newIntent(context: Context, campaignId: String? = null, donateUrl: String? = null): Intent {
            return Intent(context, GooglePayActivity::class.java)
                .putExtra(DonateDialog.ARG_CAMPAIGN_ID, campaignId)
                .putExtra(DonateDialog.ARG_DONATE_URL, donateUrl)
        }
    }
}
