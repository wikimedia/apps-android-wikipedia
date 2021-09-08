package org.wikipedia.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.databinding.DialogCategoriesBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.mwapi.MwQueryPage
import org.wikipedia.page.ExtendedBottomSheetDialogFragment
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.database.ReadingList
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.log.L
import org.wikipedia.views.PageItemView

class CategoryDialog : ExtendedBottomSheetDialogFragment() {
    private var _binding: DialogCategoriesBinding? = null
    private val binding get() = _binding!!

    private lateinit var pageTitle: PageTitle
    private val categoryList = mutableListOf<MwQueryPage.Category>()
    private val itemCallback = ItemCallback()
    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pageTitle = requireArguments().getParcelable(TITLE)!!
    }

    override fun onDestroy() {
        disposables.clear()
        _binding = null
        super.onDestroy()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogCategoriesBinding.inflate(inflater, container, false)
        binding.categoriesRecycler.layoutManager = LinearLayoutManager(requireActivity())
        binding.categoriesRecycler.adapter = CategoryAdapter()

        // titleText.setText(StringUtil.fromHtml(pageTitle.getDisplayText()));
        L10nUtil.setConditionalLayoutDirection(binding.root, pageTitle.wikiSite.languageCode)
        loadCategories()
        return binding.root
    }

    private fun loadCategories() {
        binding.categoriesError.visibility = View.GONE
        binding.categoriesNoneFound.visibility = View.GONE
        binding.categoriesRecycler.visibility = View.GONE
        binding.dialogCategoriesProgress.visibility = View.VISIBLE
        disposables.add(ServiceFactory.get(pageTitle.wikiSite).getCategories(pageTitle.prefixedText)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally { binding.dialogCategoriesProgress.visibility = View.GONE }
                .subscribe({ response ->
                    categoryList.clear()
                    for (cat in response.query!!.firstPage()!!.categories!!) {
                        if (!cat.hidden) {
                            categoryList.add(cat)
                        }
                    }
                    layOutCategories()
                }) { t ->
                    binding.categoriesError.setError(t)
                    binding.categoriesError.visibility = View.VISIBLE
                    L.e(t)
                })
    }

    private fun layOutCategories() {
        if (categoryList.isEmpty()) {
            binding.categoriesNoneFound.visibility = View.VISIBLE
            binding.categoriesRecycler.visibility = View.GONE
        }
        binding.categoriesRecycler.visibility = View.VISIBLE
        binding.categoriesNoneFound.visibility = View.GONE
        binding.categoriesError.visibility = View.GONE
    }

    private inner class CategoryItemHolder constructor(itemView: PageItemView<PageTitle>) : RecyclerView.ViewHolder(itemView) {
        fun bindItem(category: MwQueryPage.Category) {
            val title = PageTitle(category.title, pageTitle.wikiSite)
            view.item = title
            view.setTitle(title.text.replace("_", " "))
        }

        val view: PageItemView<PageTitle>
            get() = itemView as PageItemView<PageTitle>
    }

    private inner class CategoryAdapter : RecyclerView.Adapter<CategoryItemHolder>() {
        override fun getItemCount(): Int {
            return categoryList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, pos: Int): CategoryItemHolder {
            val view = PageItemView<PageTitle>(requireContext())
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
        private const val TITLE = "title"

        @JvmStatic
        fun newInstance(title: PageTitle): CategoryDialog {
            return CategoryDialog().apply { arguments = bundleOf(TITLE to title) }
        }
    }
}
