package org.wikipedia.donate

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
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
import org.wikipedia.databinding.ActivityDonateBinding
import org.wikipedia.dataclient.donate.DonationConfig
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.ResourceUtil
import org.wikipedia.util.UriUtil

class GooglePayActivity : BaseActivity() {
    private lateinit var binding: ActivityDonateBinding
    private lateinit var paymentsClient: PaymentsClient

    private val viewModel: GooglePayViewModel by viewModels()

    private var shouldWatchText = true

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
                                setErrorState(resource.throwable)
                            }
                            is GooglePayViewModel.NoPaymentMethod -> {
                                DonateDialog.launchDonateLink(this@GooglePayActivity, intent.getStringExtra(DonateDialog.ARG_DONATE_URL))
                                finish()
                            }
                            is Resource.Success -> {
                                onContentsReceived(resource.data)
                            }
                            is GooglePayViewModel.DonateSuccess -> {
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

            var amount = amountText.toFloatOrNull() ?: 0f
            if (binding.checkBoxTransactionFee.isChecked) {
                amount += viewModel.transactionFee
            }
            viewModel.finalAmount = amount

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
                    val amount = text.toString().toFloatOrNull() ?: 0f
                    child.tag == amount
                } else {
                    false
                }
            }
            setButtonHighlighted(buttonToHighlight)
        }

        binding.linkProblemsDonating.setOnClickListener {
            UriUtil.visitInExternalBrowser(this, Uri.parse(getString(R.string.donate_problems_url)))
        }
        binding.linkOtherWays.setOnClickListener {
            finish()
            UriUtil.visitInExternalBrowser(this, Uri.parse(getString(R.string.donate_other_ways_url)))
        }
        binding.linkFAQ.setOnClickListener {
            UriUtil.visitInExternalBrowser(this, Uri.parse(getString(R.string.donate_faq_url)))
        }
        binding.linkTaxDeduct.setOnClickListener {
            UriUtil.visitInExternalBrowser(this, Uri.parse(getString(R.string.donate_tax_url)))
        }
    }

    private fun validateInput(text: String): Boolean {
        val amount = text.toFloatOrNull() ?: 0f
        val min = viewModel.donationConfig?.currencyMinimumDonation?.get(viewModel.currencyCode) ?: 0f
        val max = viewModel.donationConfig?.currencyMaximumDonation?.get(viewModel.currencyCode) ?: 0f

        if (amount < min) {
            binding.donateAmountInput.error = getString(R.string.donate_gpay_minimum_amount, viewModel.currencyFormat.format(min))
            return false
        } else if (amount > max) {
            binding.donateAmountInput.error = getString(R.string.donate_gpay_maximum_amount, viewModel.currencyFormat.format(max))
            return false
        } else {
            binding.donateAmountInput.error = null
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

        val transactionFee = donationConfig.currencyTransactionFees[viewModel.currencyCode] ?: donationConfig.currencyTransactionFees["default"] ?: 0f
        binding.checkBoxTransactionFee.text = getString(R.string.donate_gpay_check_transaction_fee, viewModel.currencyFormat.format(transactionFee))

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
                shouldWatchText = false
                binding.donateAmountText.setText(viewModel.decimalFormat.format(it.tag as Float))
                shouldWatchText = true
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
                                binding.checkBoxAllowEmail.isChecked,
                                intent.getStringExtra(DonateDialog.ARG_CAMPAIGN_ID).orEmpty())
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

        fun newIntent(context: Context, campaignId: String? = null, donateUrl: String? = null): Intent {
            return Intent(context, GooglePayActivity::class.java)
                .putExtra(DonateDialog.ARG_CAMPAIGN_ID, campaignId)
                .putExtra(DonateDialog.ARG_DONATE_URL, donateUrl)
        }
    }
}
