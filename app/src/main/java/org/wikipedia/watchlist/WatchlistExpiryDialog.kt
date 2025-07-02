package org.wikipedia.watchlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices.PIXEL_7
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.activity.FragmentUtil
import org.wikipedia.compose.ComposeColors
import org.wikipedia.compose.theme.BaseTheme
import org.wikipedia.compose.theme.WikipediaTheme
import org.wikipedia.databinding.DialogWatchlistExpiryBinding
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.page.PageTitle
import org.wikipedia.theme.Theme
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.Resource

class WatchlistExpiryDialog : ExtendedBottomSheetDialogFragment() {

    interface Callback {
        fun onExpiryChanged(expiry: WatchlistExpiry)
    }

    private val viewModel: WatchlistExpiryDialogViewModel by viewModels()
    private var _binding: DialogWatchlistExpiryBinding? = null
    private val binding get() = _binding!!
    private lateinit var expiryOptions: Array<View>
    private val expiryList = listOf(WatchlistExpiry.NEVER, WatchlistExpiry.ONE_WEEK, WatchlistExpiry.ONE_MONTH,
            WatchlistExpiry.THREE_MONTH, WatchlistExpiry.SIX_MONTH)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogWatchlistExpiryBinding.inflate(inflater, container, false)
        return ComposeView(requireContext()).apply {
            setContent {
                BaseTheme(
                    currentTheme = Theme.LIGHT
                ) {
                    WatchlistExpiryDialogView(
                        modifier = Modifier
                            .background(WikipediaTheme.colors.paperColor),
                        expiryList = expiryList,
                        selectedWatchListTime = WatchlistExpiry.NEVER,
                        onExpiryItemSelected = {
                            viewModel.changeExpiry(it)
                        }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                viewModel.uiState.collect {
                    when (it) {
                        is Resource.Success -> {
                            FeedbackUtil.makeSnackbar(requireActivity(), it.data.message).show()
                            callback()?.onExpiryChanged(it.data.expiry)
                            dismiss()
                        }
                        is Resource.Error -> {
                            FeedbackUtil.showError(requireActivity(), it.throwable)
                            dismiss()
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        BottomSheetBehavior.from(requireView().parent as View).peekHeight = DimenUtil.displayHeightPx
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun callback(): Callback? {
        return FragmentUtil.getCallback(this, Callback::class.java)
    }

    companion object {
        const val ARG_PAGE_TITLE = "pageTitle"

        fun newInstance(pageTitle: PageTitle): WatchlistExpiryDialog {
            val dialog = WatchlistExpiryDialog()
            dialog.arguments = bundleOf(
                ARG_PAGE_TITLE to pageTitle
            )
            return dialog
        }
    }
}

@Composable
fun WatchlistExpiryDialogView(
    modifier: Modifier = Modifier,
    expiryList: List<WatchlistExpiry>,
    selectedWatchListTime: WatchlistExpiry,
    onExpiryItemSelected: (WatchlistExpiry) -> Unit
) {
    var selectedWatchListTime by remember { mutableStateOf(selectedWatchListTime) }

    Column(
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(WikipediaTheme.colors.borderColor)
                .height(48.dp)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                modifier = Modifier
                    .align(Alignment.CenterStart),
                text = stringResource(R.string.watchlist_expiry_dialog_title),
                style = MaterialTheme.typography.labelLarge,
                color = WikipediaTheme.colors.primaryColor
            )
        }

        LazyColumn {
            items(expiryList) { expiryItem ->
                val expiryTitle = stringResource(if (expiryItem == WatchlistExpiry.NEVER) R.string.watchlist_expiry_dialog_permanent else expiryItem.stringId)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = {
                            selectedWatchListTime = expiryItem
                            onExpiryItemSelected(expiryItem)
                        })
                        .height(48.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (expiryItem.icon != null) {
                        Icon(
                            modifier = Modifier
                                .size(24.dp),
                            painter = painterResource(expiryItem.icon),
                            contentDescription = null,
                            tint = ComposeColors.Yellow500
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                        )
                    }

                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 24.dp),
                        text = expiryTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = WikipediaTheme.colors.primaryColor,
                        fontWeight = if (expiryItem.expiry == selectedWatchListTime.expiry) FontWeight.Bold else null
                    )

                    if (expiryItem.expiry == selectedWatchListTime.expiry) {
                        Icon(
                            modifier = Modifier
                                .size(24.dp),
                            painter = painterResource(R.drawable.ic_check_black_24dp),
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }
}

@Preview(device = PIXEL_7, showBackground = true)
@Composable
private fun WatchlistExpiryDialogViewPreview() {
    BaseTheme(
        currentTheme = Theme.LIGHT
    ) {
        WatchlistExpiryDialogView(
            modifier = Modifier
                .background(WikipediaTheme.colors.paperColor),
            expiryList = listOf(WatchlistExpiry.NEVER, WatchlistExpiry.ONE_WEEK, WatchlistExpiry.ONE_MONTH,
                WatchlistExpiry.THREE_MONTH, WatchlistExpiry.SIX_MONTH),
            selectedWatchListTime = WatchlistExpiry.NEVER,
            onExpiryItemSelected = {}
        )
    }
}
