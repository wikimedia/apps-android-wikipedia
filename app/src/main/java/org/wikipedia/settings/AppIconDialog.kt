package org.wikipedia.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.AlignItems
import com.google.android.flexbox.FlexDirection
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.android.flexbox.JustifyContent
import org.wikipedia.R
import org.wikipedia.appshortcuts.AppShortcuts
import org.wikipedia.databinding.DialogAppIconBinding
import org.wikipedia.databinding.ItemAppIconBinding
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.ResourceUtil

class AppIconDialog : ExtendedBottomSheetDialogFragment() {
    private var _binding: DialogAppIconBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogAppIconBinding.inflate(inflater, container, false)

        binding.appIconRecyclerView.layoutManager = FlexboxLayoutManager(requireContext()).apply {
            flexDirection = FlexDirection.ROW
            justifyContent = JustifyContent.CENTER
            alignItems = AlignItems.CENTER
        }
        binding.appIconRecyclerView.adapter = AppIconAdapter(LauncherIcon.entries).apply {
            onItemClickListener = { icon ->
                Prefs.selectedAppIcon = icon.key
                LauncherController.setIcon(icon)
                AppShortcuts.setShortcuts(requireContext())
                dismiss()
                Toast.makeText(requireActivity(), R.string.settings_app_icon_updated, Toast.LENGTH_SHORT).show()
            }
        }
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class AppIconAdapter(val list: List<LauncherIcon>) : RecyclerView.Adapter<AppIconAdapter.AppIconViewHolder>() {
        var onItemClickListener: ((LauncherIcon) -> Unit)? = null

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
                    val isSelected = Prefs.selectedAppIcon == item.key
                    val strokeColor = if (isSelected) R.attr.progressive_color else R.attr.border_color
                    val newStrokeWidth = if (isSelected) 2f else 1f
                    this.strokeColor = ResourceUtil.getThemedColorStateList(context, strokeColor)
                    this.strokeWidth = DimenUtil.dpToPx(newStrokeWidth)
                }
            }
        }
    }
}
