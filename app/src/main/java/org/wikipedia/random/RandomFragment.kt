package org.wikipedia.random

import android.graphics.drawable.Animatable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.core.os.bundleOf
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.functions.Consumer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.wikipedia.Constants
import org.wikipedia.Constants.InvokeSource
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.FragmentRandomBinding
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.events.ArticleSavedOrDeletedEvent
import org.wikipedia.history.HistoryEntry
import org.wikipedia.page.ExclusiveBottomSheetPresenter
import org.wikipedia.page.PageActivity
import org.wikipedia.page.PageTitle
import org.wikipedia.readinglist.AddToReadingListDialog
import org.wikipedia.readinglist.LongPressMenu
import org.wikipedia.readinglist.MoveToReadingListDialog
import org.wikipedia.readinglist.ReadingListBehaviorsUtil
import org.wikipedia.readinglist.ReadingListBehaviorsUtil.AddToDefaultListCallback
import org.wikipedia.readinglist.ReadingListBehaviorsUtil.addToDefaultList
import org.wikipedia.readinglist.database.ReadingListPage
import org.wikipedia.util.AnimationUtil.PagerTransformer
import org.wikipedia.util.DimenUtil
import org.wikipedia.util.FeedbackUtil
import org.wikipedia.views.PositionAwareFragmentStateAdapter

class RandomFragment : Fragment() {

    companion object {
        fun newInstance(wikiSite: WikiSite, invokeSource: InvokeSource) = RandomFragment().apply {
            arguments = bundleOf(
                    RandomActivity.INTENT_EXTRA_WIKISITE to wikiSite,
                    Constants.INTENT_EXTRA_INVOKE_SOURCE to invokeSource
            )
        }
    }

    private var _binding: FragmentRandomBinding? = null
    private val binding get() = _binding!!

    private val disposables = CompositeDisposable()

    private val bottomSheetPresenter = ExclusiveBottomSheetPresenter()
    private val viewPagerListener: ViewPagerListener = ViewPagerListener()

    private val viewModel: RandomViewModel by viewModels { RandomViewModel.Factory(requireArguments()) }

    private lateinit var wikiSite: WikiSite
    private val topTitle get() = getTopChild()?.title
    private var saveButtonState = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        _binding = FragmentRandomBinding.inflate(inflater, container, false)
        val view = binding.root

        FeedbackUtil.setButtonLongPressToast(binding.randomNextButton, binding.randomSaveButton)

        wikiSite = requireArguments().getParcelable(RandomActivity.INTENT_EXTRA_WIKISITE)!!

        binding.randomItemPager.offscreenPageLimit = 2
        binding.randomItemPager.adapter = RandomItemAdapter(this)
        binding.randomItemPager.setPageTransformer(PagerTransformer(resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL))
        binding.randomItemPager.registerOnPageChangeCallback(viewPagerListener)

        binding.randomNextButton.setOnClickListener { onNextClick() }
        binding.randomBackButton.setOnClickListener { onBackClick() }
        binding.randomSaveButton.setOnClickListener { onSaveShareClick() }

        disposables.add(WikipediaApp.getInstance().bus.subscribe(EventBusConsumer()))

        updateSaveShareButton()
        updateBackButton(0)

        if (savedInstanceState != null && binding.randomItemPager.currentItem == 0 && topTitle != null) {
            updateSaveShareButton(topTitle)
        }
        lifecycleScope.launchWhenResumed {
            launch {
                viewModel.savaShareState.collect { alreadyStored ->
                    saveButtonState = alreadyStored
                    val img =
                        if (saveButtonState) R.drawable.ic_bookmark_white_24dp else R.drawable.ic_bookmark_border_white_24dp
                    binding.randomSaveButton.setImageResource(img)
                }
            }

            launch {
                viewModel.saveToDefaultList.collect { result ->
                    if (result is RandomViewModel.Result) {
                        onAddPageToDefaultList(result.value)
                    }
                }
            }
            launch {
                viewModel.saveToCustomList.collect { result ->
                    if (result is RandomViewModel.Result) {
                        onAddPageToCustomList(result.value)
                    }
                }
            }
            launch {
                viewModel.movePageToList.collect { result ->
                    if (result is RandomViewModel.Result) {
                        onMovePageToList(result.value.sourceReadingListId, result.value.title)
                    }
                }
            }
        }

        return view
    }

    override fun onResume() {
        super.onResume()
        updateSaveShareButton(topTitle)
    }

    override fun onDestroyView() {
        disposables.clear()
        binding.randomItemPager.unregisterOnPageChangeCallback(viewPagerListener)
        _binding = null
        super.onDestroyView()
    }

    private fun onNextClick() {
        if (binding.randomNextButton.drawable is Animatable) {
            (binding.randomNextButton.drawable as Animatable).start()
        }

        viewPagerListener.setNextPageSelectedAutomatic()
        binding.randomItemPager.setCurrentItem(binding.randomItemPager.currentItem + 1, true)

        viewModel.clickedForward()
    }

    private fun onBackClick() {
        viewPagerListener.setNextPageSelectedAutomatic()

        if (binding.randomItemPager.currentItem > 0) {
            binding.randomItemPager.setCurrentItem(binding.randomItemPager.currentItem - 1, true)
            viewModel.clickedBack()
        }
    }

    private fun onSaveShareClick() {
        val title = topTitle ?: return

        if (saveButtonState) {
            LongPressMenu(binding.randomSaveButton, object : LongPressMenu.Callback {
                override fun onOpenLink(entry: HistoryEntry) {
                    // ignore
                }

                override fun onOpenInNewTab(entry: HistoryEntry) {
                    // ignore
                }

                override fun onAddRequest(entry: HistoryEntry, addToDefault: Boolean) {
                    if (addToDefault) {
                        viewModel.saveToDefaultList(title)
                    } else {
                        viewModel.saveToCustomList(title)
                    }
                }

                override fun onMoveRequest(page: ReadingListPage?, entry: HistoryEntry) {
                    onMovePageToList(page!!.listId, title)
                }
            }).show(HistoryEntry(title, HistoryEntry.SOURCE_RANDOM))
        } else {
            viewModel.saveToDefaultList(title)
        }
    }

    fun onSelectPage(title: PageTitle, sharedElements: Array<Pair<View, String>>) {
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(requireActivity(), *sharedElements)
        val intent = PageActivity.newIntentForNewTab(requireContext(),
                HistoryEntry(title, HistoryEntry.SOURCE_RANDOM), title)

        if (sharedElements.isNotEmpty()) {
            intent.putExtra(Constants.INTENT_EXTRA_HAS_TRANSITION_ANIM, true)
        }

        startActivity(intent, if (DimenUtil.isLandscape(requireContext()) || sharedElements.isEmpty()) null else options.toBundle())
    }

    private fun onAddPageToDefaultList(title: PageTitle) {
        addToDefaultList(requireActivity(), title, InvokeSource.RANDOM_ACTIVITY,
            AddToDefaultListCallback { readingListId -> viewModel.movePageToList(readingListId, title) },
            ReadingListBehaviorsUtil.Callback { updateSaveShareButton(title) }
        )
    }

    private fun onAddPageToCustomList(title: PageTitle) {
            bottomSheetPresenter.show(childFragmentManager,
                    AddToReadingListDialog.newInstance(title, InvokeSource.RANDOM_ACTIVITY) {
                        updateSaveShareButton(title)
            })
    }

    fun onMovePageToList(sourceReadingListId: Long, title: PageTitle) {
        bottomSheetPresenter.show(childFragmentManager,
                MoveToReadingListDialog.newInstance(sourceReadingListId, listOf(title), InvokeSource.RANDOM_ACTIVITY, true) {
                    updateSaveShareButton(title)
                })
    }

    private fun updateBackButton(pagerPosition: Int) {
        binding.randomBackButton.isClickable = pagerPosition != 0
        binding.randomBackButton.alpha = if (pagerPosition == 0) 0.5f else 1f
    }

    private fun updateSaveShareButton(title: PageTitle?) {
        if (title == null) {
            return
        }

        viewModel.actualizeSaveShare(title)
    }

    private fun updateSaveShareButton() {
        val enable = getTopChild()?.isLoadComplete ?: false

        binding.randomSaveButton.isClickable = enable
        binding.randomSaveButton.alpha = if (enable) 1f else 0.5f
    }

    fun onChildLoaded() {
        updateSaveShareButton()
    }

    private fun getTopChild(): RandomItemFragment? {
        val adapter = binding.randomItemPager.adapter as? RandomItemAdapter
        return adapter?.getFragmentAt(binding.randomItemPager.currentItem) as? RandomItemFragment
    }

    private inner class RandomItemAdapter(fragment: Fragment) : PositionAwareFragmentStateAdapter(fragment) {
        override fun getItemCount(): Int {
            return Int.MAX_VALUE
        }

        override fun createFragment(position: Int): Fragment {
            return RandomItemFragment.newInstance(wikiSite)
        }
    }

    private inner class ViewPagerListener : OnPageChangeCallback() {
        private var prevPosition = 0
        private var nextPageSelectedAutomatic = false

        fun setNextPageSelectedAutomatic() {
            nextPageSelectedAutomatic = true
        }

        override fun onPageSelected(position: Int) {
            updateBackButton(position)
            updateSaveShareButton(topTitle)

            if (!nextPageSelectedAutomatic) {
                if (position > prevPosition) {
                    viewModel.swipedForward()
                } else if (position < prevPosition) {
                    viewModel.swipedBack()
                }
            }

            nextPageSelectedAutomatic = false
            prevPosition = position

            updateSaveShareButton()
        }
    }

    private inner class EventBusConsumer : Consumer<Any> {
        override fun accept(event: Any) {
            if (event is ArticleSavedOrDeletedEvent) {
                if (!isAdded || topTitle == null) {
                    return
                }
                for (page in event.pages) {
                    if (page.apiTitle == topTitle?.prefixedText && page.wiki.languageCode == topTitle?.wikiSite?.languageCode) {
                        updateSaveShareButton(topTitle)
                    }
                }
            }
        }
    }
}
