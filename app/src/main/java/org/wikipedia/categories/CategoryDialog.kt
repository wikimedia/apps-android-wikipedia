package org.wikipedia.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.wikipedia.R
import org.wikipedia.databinding.DialogCategoriesBinding
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.Resource
import org.wikipedia.util.StringUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.DrawableItemDecoration
import org.wikipedia.views.PageItemView

class CategoryDialog : ExtendedBottomSheetDialogFragment() {
    private var _binding: DialogCategoriesBinding? = null
    private val binding get() = _binding!!

    private val itemCallback = ItemCallback()
    private val viewModel: CategoryDialogViewModel by viewModels { CategoryDialogViewModel.Factory(requireArguments()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogCategoriesBinding.inflate(inflater, container, false)
        binding.categoriesRecycler.layoutManager = LinearLayoutManager(requireActivity())
        binding.categoriesRecycler.addItemDecoration(DrawableItemDecoration(requireContext(), R.attr.list_separator_drawable, drawStart = false, drawEnd = false))
        binding.categoriesDialogPageTitle.text = StringUtil.fromHtml(viewModel.pageTitle.displayText)
        L10nUtil.setConditionalLayoutDirection(binding.root, viewModel.pageTitle.wikiSite.languageCode)

        binding.categoriesError.isVisible = false
        binding.categoriesNoneFound.isVisible = false
        binding.categoriesRecycler.isVisible = false
        binding.dialogCategoriesProgress.isVisible = true

        viewModel.categoriesData.observe(this) {
            binding.dialogCategoriesProgress.isVisible = false
            if (it is Resource.Success) {
                layOutCategories(it.data)
            } else if (it is Resource.Error) {
                binding.categoriesRecycler.isVisible = false
                binding.categoriesError.setError(it.throwable)
                binding.categoriesError.isVisible = true
                L.e(it.throwable)
            }
        }

        return binding.root
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    private fun layOutCategories(categoryList: List<PageTitle>) {
        if (categoryList.isEmpty()) {
            binding.categoriesNoneFound.visibility = View.VISIBLE
            binding.categoriesRecycler.visibility = View.GONE
        }
        binding.categoriesRecycler.visibility = View.VISIBLE
        binding.categoriesNoneFound.visibility = View.GONE
        binding.categoriesError.visibility = View.GONE
        binding.categoriesRecycler.adapter = CategoryAdapter(categoryList)
    }

    private inner class CategoryItemHolder constructor(val view: PageItemView<PageTitle>) : RecyclerView.ViewHolder(view) {
        fun bindItem(title: PageTitle) {
            view.item = title
            view.setTitle(StringUtil.removeNamespace(title.displayText))
        }
    }

    private inner class CategoryAdapter(val categoryList: List<PageTitle>) : RecyclerView.Adapter<CategoryItemHolder>() {
        override fun getItemCount(): Int {
            return categoryList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, pos: Int): CategoryItemHolder {
            val view = PageItemView<PageTitle>(requireContext())
            view.setImageVisible(false)
            return CategoryItemHolder(view)
        }

        override fun onBindViewHolder(holder: CategoryItemHolder, pos: Int) {
            holder.bindItem(categoryList[pos])
        }

        override fun onViewAttachedToWindow(holder: CategoryItemHolder) {
            super.onViewAttachedToWindow(holder)
            holder.view.callback = itemCallback
        }

        override fun onViewDetachedFromWindow(holder: CategoryItemHolder) {
            holder.view.callback = null
            super.onViewDetachedFromWindow(holder)
        }
    }

    private inner class ItemCallback : PageItemView.Callback<PageTitle?> {
        override fun onClick(item: PageTitle?) {
            if (item != null) {
                startActivity(CategoryActivity.newIntent(requireActivity(), item))
            }
        }

        override fun onLongClick(item: PageTitle?): Boolean {
            return false
        }

        override fun onActionClick(item: PageTitle?, view: View) {}

        override fun onListChipClick(readingList: ReadingList) {}
    }

    companion object {
        const val ARG_TITLE = "title"

        fun newInstance(title: PageTitle): CategoryDialog {
            return CategoryDialog().apply { arguments = bundleOf(ARG_TITLE to title) }
        }
    }
}
