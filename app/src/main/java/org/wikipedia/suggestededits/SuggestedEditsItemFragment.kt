package org.wikipedia.suggestededits

import android.os.Bundle
import androidx.fragment.app.Fragment
import io.reactivex.rxjava3.disposables.CompositeDisposable
import org.wikipedia.dataclient.mwapi.MwQueryPage

abstract class SuggestedEditsItemFragment : Fragment() {
    interface Callback {
        fun getLangCode(): String
        fun getSinglePage(): MwQueryPage?
        fun updateActionButton()
        fun nextPage(sourceFragment: Fragment?)
        fun logSuccess()
    }

    val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //retainInstance = true
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    fun parent(): SuggestedEditsCardsFragment {
        return requireActivity().supportFragmentManager.fragments[0] as SuggestedEditsCardsFragment
    }

    open fun publishEnabled(): Boolean {
        return true
    }

    open fun publishOutlined(): Boolean {
        return false
    }

    open fun publish() {}
}
