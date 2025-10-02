package org.wikipedia.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.appshortcuts.AppShortcuts
import org.wikipedia.databinding.DialogAppIconBinding
import org.wikipedia.databinding.ItemAppIconBinding
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.util.ResourceUtil

class AppIconDialog : ExtendedBottomSheetDialogFragment() {
    private var _binding: DialogAppIconBinding? = null
    private val binding get() = _binding!!

    private val appIconAdapter = AppIconAdapter().apply {
        onItemClickListener { icon ->
            Prefs.selectedAppIcon = icon.key
            LauncherController.setIcon(icon)
            AppShortcuts.setShortcuts(requireContext())
            updateIcons(icon)
            dismiss()
            FeedbackUtil.makeSnackbar(requireActivity(), WikipediaApp.instance.getString(R.string.settings_app_icon_updated)).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAppIconBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.appIconRecyclerView.apply {
            this.layoutManager = FlexboxLayoutManager(requireContext()).apply {
                flexDirection = FlexDirection.ROW
                justifyContent = JustifyContent.CENTER
                alignItems = AlignItems.CENTER
            }
            adapter = appIconAdapter
        }
        appIconAdapter.updateItems(LauncherIcon.initialValues())
    }

    private fun updateIcons(selectedIcon: LauncherIcon) {
        val currentSelectedIcon = if (Prefs.selectedAppIcon != null) Prefs.selectedAppIcon
        else selectedIcon.key

        LauncherIcon.entries.forEach {
            it.isSelected = it.key == currentSelectedIcon
        }
        appIconAdapter.updateItems(LauncherIcon.entries)
    }

    private class AppIconAdapter : RecyclerView.Adapter<AppIconAdapter.AppIconViewHolder>() {
        private var list = mutableListOf<LauncherIcon>()
        private var onItemClickListener: ((LauncherIcon) -> Unit)? = null

        fun onItemClickListener(onItemClickListener: (LauncherIcon) -> Unit) {
            this.onItemClickListener = onItemClickListener
        }

        fun updateItems(newList: List<LauncherIcon>) {
            list.clear()
            list.addAll(newList)
            notifyItemRangeChanged(0, list.size)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppIconViewHolder {
            val view = ItemAppIconBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return AppIconViewHolder(view)
        }

        override fun getItemCount() = list.size

        override fun onBindViewHolder(holder: AppIconViewHolder, position: Int) {
            val item = list[position]
            holder.bind(item)
        }

        private inner class AppIconViewHolder(val binding: ItemAppIconBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: LauncherIcon) {
                binding.appIcon.apply {
                    foreground = ContextCompat.getDrawable(binding.root.context, item.foreground)
                    background = ContextCompat.getDrawable(binding.root.context, item.background)
                    setOnClickListener {
                        onItemClickListener?.invoke(item)
                    }
                    val strokeColor = if (item.isSelected) R.attr.progressive_color else R.attr.border_color
                    val newStrokeWidth = if (item.isSelected) 2f else 1f
                    this.strokeColor = ResourceUtil.getThemedColorStateList(context, strokeColor)
                    this.strokeWidth = DimenUtil.dpToPx(newStrokeWidth)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
