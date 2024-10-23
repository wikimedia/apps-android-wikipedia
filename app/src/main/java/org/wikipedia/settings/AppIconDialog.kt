package org.wikipedia.settings

import android.content.res.ColorStateList
import android.graphics.Color
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
import org.wikipedia.LauncherController
import org.wikipedia.LauncherIcon
import org.wikipedia.R
import org.wikipedia.appshortcuts.AppShortcuts
import org.wikipedia.databinding.DialogAppIconBinding
import org.wikipedia.databinding.ItemAppIconBinding
import org.wikipedia.page.ExtendedBottomSheetDialogFragment

class AppIconDialog : ExtendedBottomSheetDialogFragment() {

    private lateinit var binding: DialogAppIconBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DialogAppIconBinding.inflate(inflater, container, false)
        val layoutManager = FlexboxLayoutManager(requireContext()).apply {
            flexDirection = FlexDirection.ROW
            justifyContent = JustifyContent.CENTER
            alignItems = AlignItems.CENTER
        }
        val appIconAdapter = AppIconAdapter()
        appIconAdapter.updateItems(LauncherIcon.entries)
        appIconAdapter.onItemClickListener { selectedIcon ->
            LauncherController.setIcon(selectedIcon)
            AppShortcuts.setShortcuts(requireContext())
            LauncherIcon.entries.forEach {
                it.isSelected = it.key == selectedIcon.key
            }
            appIconAdapter.updateItems(LauncherIcon.entries)
        }
        binding.appIconRecyclerView.apply {
            this.layoutManager = layoutManager
            adapter = appIconAdapter
        }
        return binding.root
    }

    private class AppIconAdapter : RecyclerView.Adapter<AppIconAdapter.AppIconViewHolder>() {
        private var list: List<LauncherIcon> = listOf()
        private var onItemClickListener: ((LauncherIcon) -> Unit)? = null

        fun onItemClickListener(onItemClickListener: (LauncherIcon) -> Unit) {
            this.onItemClickListener = onItemClickListener
        }

        fun updateItems(list: List<LauncherIcon>) {
            this.list = list
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppIconViewHolder {
            val view = ItemAppIconBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return AppIconViewHolder(view)
        }

        override fun getItemCount(): Int = list.size

        override fun onBindViewHolder(holder: AppIconViewHolder, position: Int) {
            val item = list[position]
            holder.bind(item)
        }

        private inner class AppIconViewHolder(val binding: ItemAppIconBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(item: LauncherIcon) {
                binding.appIcon.foreground = ContextCompat.getDrawable(binding.root.context, item.foreground)
                binding.appIcon.background = ContextCompat.getDrawable(binding.root.context, item.background)
                binding.appIcon.setOnClickListener {
                   onItemClickListener?.invoke(item)
                }
                val strokeColor = if (item.isSelected) {
                    ContextCompat.getColor(binding.root.context, R.color.blue600)
                } else Color.TRANSPARENT
                binding.appIcon.strokeColor = ColorStateList.valueOf(strokeColor)
            }
        }
    }

    companion object {
        fun newInstance(): AppIconDialog {
            return AppIconDialog()
        }
    }
}
