package org.wikipedia.talk

import org.wikipedia.talk.db.TalkPageSeen
import org.wikipedia.talk.db.TalkPageSeenDao

class TalkPageSeenRepository constructor(private val talkPageSeenDao: TalkPageSeenDao) {

    fun getAllTalkPageSeen() = talkPageSeenDao.getAll()

    fun getTalkPageSeen(sha: String) = talkPageSeenDao.getTalkPageSeen(sha)

    suspend fun insertTalkPageSeen(talkPageSeen: TalkPageSeen) {
        talkPageSeenDao.insertTalkPageSeen(talkPageSeen)
    }

    suspend fun deleteAll() {
        talkPageSeenDao.deleteAll()
    }
}
