package org.wikipedia.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.wikipedia.R
import org.wikipedia.WikipediaApp
import org.wikipedia.databinding.ViewUserMentionInputBinding
import org.wikipedia.dataclient.ServiceFactory
import org.wikipedia.edit.richtext.SyntaxHighlighter
import org.wikipedia.extensions.coroutineScope
import org.wikipedia.page.PageTitle
import org.wikipedia.util.StringUtil

class UserMentionInputView(context: Context, attrs: AttributeSet? = null) :
    LinearLayout(context, attrs), UserMentionEditText.Listener {

    interface Listener {
        fun onUserMentionListUpdate()
        fun onUserMentionComplete()
    }

    val editText get() = binding.inputEditText
    val textInputLayout get() = binding.inputTextLayout
    var wikiSite = WikipediaApp.instance.wikiSite
    var listener: Listener? = null
    var userNameHints: Set<String> = emptySet()

    private val binding = ViewUserMentionInputBinding.inflate(LayoutInflater.from(context), this)
    private val userNameList = mutableListOf<String>()
    private val syntaxHighlighter: SyntaxHighlighter
    private var clientJob: Job? = null

    init {
        orientation = VERTICAL
        binding.inputEditText.listener = this
        binding.userListRecycler.layoutManager = LinearLayoutManager(context)
        binding.userListRecycler.adapter = UserNameAdapter()
        syntaxHighlighter = SyntaxHighlighter(context as ComponentActivity, binding.inputEditText,
            null, 200)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        clientJob?.cancel()
    }

    override fun onStartUserNameEntry() {
        userNameList.clear()
        binding.userListRecycler.adapter?.notifyDataSetChanged()
    }

    override fun onCancelUserNameEntry() {
        clientJob?.cancel()
        binding.userListRecycler.isVisible = false
        listener?.onUserMentionComplete()
    }

    override fun onUserNameChanged(userName: String) {
        var userNamePrefix = userName
        if (userNamePrefix.startsWith("@")) {
            if (userNamePrefix.length > 1) {
                userNamePrefix = userNamePrefix.substring(1)
                searchForUserName(userNamePrefix)
            } else {
                userNameList.clear()
                userNameList.addAll(userNameHints)
                onSearchResults()
            }
        }
    }

    fun maybePrepopulateUserName(currentUserName: String, currentPageTitle: PageTitle) {
        if (binding.inputEditText.text.isEmpty() && userNameHints.isNotEmpty()) {
            val candidateName = userNameHints.first()
            if (candidateName != currentUserName &&
                    !StringUtil.addUnderscores(candidateName).equals(StringUtil.addUnderscores(currentPageTitle.text), true)) {
                binding.inputEditText.prepopulateUserName(candidateName, wikiSite)
            }
        }
    }

    private fun searchForUserName(prefix: String) {
        clientJob?.cancel()
        clientJob = coroutineScope().launch(CoroutineExceptionHandler { _, exception ->
            onSearchError(exception)
        }) {
            delay(200)
            val response = ServiceFactory.get(wikiSite).prefixSearchUsers(prefix, 10)
            userNameList.clear()
            userNameList.addAll(userNameHints.filter { it.startsWith(prefix, ignoreCase = true) })
            response.query?.allUsers?.forEach {
                if (!userNameList.contains(it.name)) {
                    userNameList.add(it.name)
                }
            }
            onSearchResults()
        }
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

    private inner class UserNameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), OnClickListener {
        private lateinit var userName: String

        fun bindItem(position: Int) {
            userName = userNameList[position]
            itemView.setOnClickListener(this)
            (itemView as TextView).text = userName
        }

        override fun onClick(v: View) {
            binding.inputEditText.onCommitUserName(userName, wikiSite)
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
