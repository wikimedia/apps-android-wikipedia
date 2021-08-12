package org.wikipedia.random

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.Constants
import org.wikipedia.databinding.FragmentRandomItemBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.dataclient.WikiSite
import org.wikipedia.dataclient.page.PageSummary
import org.wikipedia.page.PageTitle
import org.wikipedia.util.ImageUrlUtil.getUrlForPreferredSize
import org.wikipedia.util.L10nUtil
import org.wikipedia.util.log.L

class RandomItemFragment : Fragment() {

    companion object {
        private const val EXTRACT_MAX_LINES = 4

        fun newInstance(wikiSite: WikiSite) = RandomItemFragment().apply {
            arguments = bundleOf(RandomActivity.INTENT_EXTRA_WIKISITE to wikiSite)
        }
    }

    private var _binding: FragmentRandomItemBinding? = null
    private val binding get() = _binding!!

    private val disposables = CompositeDisposable()

    private lateinit var wikiSite: WikiSite
    private var summary: PageSummary? = null

    val isLoadComplete: Boolean get() = summary != null
    val title: PageTitle? get() = summary?.getPageTitle(wikiSite)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        wikiSite = requireArguments().getParcelable(RandomActivity.INTENT_EXTRA_WIKISITE)!!

        retainInstance = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        super.onCreateView(inflater, container, savedInstanceState)

        _binding = FragmentRandomItemBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.randomItemWikiArticleCardView.setOnClickListener {
            title?.let { title ->
                parent().onSelectPage(title, binding.randomItemWikiArticleCardView.getSharedElements())
            }
        }

        binding.randomItemErrorView.backClickListener = View.OnClickListener {
            requireActivity().finish()
        }

        binding.randomItemErrorView.retryClickListener = View.OnClickListener {
            binding.randomItemProgress.visibility = View.VISIBLE
            getRandomPage()
        }

        updateContents()

        if (summary == null) {
            getRandomPage()
        }

        L10nUtil.setConditionalLayoutDirection(view, wikiSite.languageCode)
        return view
    }

    override fun onDestroyView() {
        disposables.clear()
        _binding = null

        super.onDestroyView()
    }

    private fun getRandomPage() {
        val d = ServiceFactory.getRest(wikiSite).randomSummary
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ pageSummary ->
                    summary = pageSummary
                    updateContents()
                    parent().onChildLoaded()
                }, { t ->
                    setErrorState(t)
                })

        disposables.add(d)
    }

    private fun setErrorState(t: Throwable) {
        L.e(t)

        binding.randomItemErrorView.setError(t)
        binding.randomItemErrorView.visibility = View.VISIBLE
        binding.randomItemProgress.visibility = View.GONE
        binding.randomItemWikiArticleCardView.visibility = View.GONE
    }

    private fun updateContents() {
        binding.randomItemErrorView.visibility = View.GONE

        binding.randomItemWikiArticleCardView.visibility =
                if (summary == null) View.GONE else View.VISIBLE

        binding.randomItemProgress.visibility =
                if (summary == null) View.VISIBLE else View.GONE

        val summary = summary ?: return

        binding.randomItemWikiArticleCardView.setTitle(summary.displayTitle)
        binding.randomItemWikiArticleCardView.setDescription(summary.description)
        binding.randomItemWikiArticleCardView.setExtract(summary.extract, EXTRACT_MAX_LINES)

        var imageUri: Uri? = null

        summary.thumbnailUrl.takeUnless { it.isNullOrBlank() }?.let { thumbnailUrl ->
            imageUri = Uri.parse(getUrlForPreferredSize(thumbnailUrl, Constants.PREFERRED_CARD_THUMBNAIL_SIZE))
        }
        binding.randomItemWikiArticleCardView.setImageUri(imageUri, false)
    }

    private fun parent(): RandomFragment {
        return requireActivity().supportFragmentManager.fragments[0] as RandomFragment
    }
}
