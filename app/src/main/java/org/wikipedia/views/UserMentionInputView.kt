package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ViewUserMentionInputBinding
import org.wikipedia.dataclient.ServiceFactory
import java.util.concurrent.TimeUnit

class UserMentionInputView : LinearLayout, UserMentionEditText.Listener {
    interface Listener {
        fun onUserMentionListUpdate()
        fun onUserMentionComplete()
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    val editText get() = binding.inputEditText
    val textInputLayout get() = binding.inputTextLayout
    var wikiSite = WikipediaApp.getInstance().wikiSite
    var listener: Listener? = null
    var userNameHints: Set<String> = emptySet()

    private val binding = ViewUserMentionInputBinding.inflate(LayoutInflater.from(context), this)
    private val disposables = CompositeDisposable()
    private val userNameList = mutableListOf<String>()

    init {
        orientation = VERTICAL
        binding.inputEditText.listener = this
        binding.userListRecycler.layoutManager = LinearLayoutManager(context)
        binding.userListRecycler.adapter = UserNameAdapter()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        disposables.clear()
    }

    override fun onStartUserNameEntry() {
        userNameList.clear()
        binding.userListRecycler.adapter?.notifyDataSetChanged()
    }

    override fun onCancelUserNameEntry() {
        disposables.clear()
        binding.userListRecycler.isVisible = false
        listener?.onUserMentionComplete()
    }

    override fun onUserNameChanged(userName: String) {
        var userNamePrefix = userName
        if (userNamePrefix.startsWith("@") && userNamePrefix.length > 1) {
            userNamePrefix = userNamePrefix.substring(1)
            if (userNamePrefix.isNotEmpty()) {
                searchForUserName(userNamePrefix)
            }
        }
    }

    fun maybePrepopulateUserName() {
        if (binding.inputEditText.text.isNullOrEmpty() && userNameHints.isNotEmpty()) {
            binding.inputEditText.prepopulateUserName(userNameHints.first())
        }
    }

    private fun searchForUserName(prefix: String) {
        disposables.clear()
        disposables.add(Observable.timer(200, TimeUnit.MILLISECONDS)
                .flatMap { ServiceFactory.get(wikiSite).prefixSearchUsers(prefix, 10) }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ response ->
                    userNameList.clear()
                    userNameList.addAll(userNameHints.filter { it.startsWith(prefix, ignoreCase = true) })

                    response.query?.allUsers?.forEach {
                        if (!userNameList.contains(it.name)) {
                            userNameList.add(it.name)
                        }
                    }
                    onSearchResults()
                }, {
                    onSearchError(it)
                })
        )
    }

    private fun onSearchResults() {
        binding.userListRecycler.isVisible = true
        binding.userListRecycler.adapter?.notifyDataSetChanged()
        listener?.onUserMentionListUpdate()
    }

    private fun onSearchError(t: Throwable) {
        t.printStackTrace()
        binding.userListRecycler.isVisible = false
    }

    private inner class UserNameViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView), OnClickListener {
        private lateinit var userName: String

        fun bindItem(position: Int) {
            userName = userNameList[position]
            itemView.setOnClickListener(this)
            (itemView as TextView).text = userName
        }

        override fun onClick(v: View) {
            binding.inputEditText.onCommitUserName(userName)
            listener?.onUserMentionComplete()
        }
    }

    private inner class UserNameAdapter : RecyclerView.Adapter<UserNameViewHolder>() {
        override fun getItemCount(): Int {
            return userNameList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserNameViewHolder {
            return UserNameViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_search_recent, parent, false))
        }

        override fun onBindViewHolder(holder: UserNameViewHolder, pos: Int) {
            holder.bindItem(pos)
        }
    }
}
