package com.bcm.messenger.adhoc.search

import com.bcm.messenger.adhoc.logic.AdHocSession
import com.bcm.messenger.common.AccountContext
import com.bcm.messenger.common.finder.BcmFindData
import com.bcm.messenger.common.finder.BcmFinderType
import com.bcm.messenger.common.finder.IBcmFindResult
import com.bcm.messenger.common.finder.IBcmFinder
import com.bcm.messenger.common.recipients.Recipient
import com.bcm.messenger.utility.StringAppearanceUtil
import com.bcm.messenger.utility.logger.ALog
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicReference

/**
 *
 * Created by wjh on 2019-09-08
 */
class BcmAdHocFinder(private val accountContext: AccountContext) : IBcmFinder {

    private val TAG = "BcmAdHocFinder"
    private val mSearchLock = AtomicReference(CountDownLatch(1))
    @Volatile
    private var mSearchList: List<AdHocSession>? = null
    @Volatile
    private var mFindResult: SessionFindResult? = null

    fun updateSource(sessionList: List<AdHocSession>) {
        ALog.d(TAG, "updateSource: ${sessionList.size}")
        if (mSearchLock.get().count <= 0) {
            mSearchLock.set(CountDownLatch(1))
        }
        mSearchList = sessionList.sortedWith(object : Comparator<AdHocSession> {
            private val map = mutableMapOf<AdHocSession, Int>()
            override fun compare(o1: AdHocSession, o2: AdHocSession): Int {
                return sort(map, o1, o2)
            }
        })
        mSearchLock.get().countDown()
    }

    private fun sort(map: MutableMap<AdHocSession,Int>, entry1: AdHocSession, entry2: AdHocSession): Int {

        fun getCharacterLetterIndex(name: String): Int {
            if (name.isNotEmpty()) {
                val n = StringAppearanceUtil.getFirstCharacterLetter(name)
                for (i in Recipient.LETTERS.indices) {
                    if (n == Recipient.LETTERS[i]) {
                        return i
                    }
                }
            }
            return Recipient.LETTERS.size - 1
        }

        var one = map[entry1]
        if (one == null) {
            one = getCharacterLetterIndex(entry1.displayName(accountContext))
            map[entry1] = one
        }
        var two = map[entry2]
        if (two == null) {
            two = getCharacterLetterIndex(entry2.displayName(accountContext))
            map[entry2] = two
        }

        return one.compareTo(two)
    }


    override fun type(): BcmFinderType {
        return BcmFinderType.AIR_CHAT
    }

    override fun find(key: String): IBcmFindResult {
        ALog.d(TAG, "find key: $key")
        if (mFindResult == null) {
            mFindResult = SessionFindResult(key)
        }else {
            mFindResult?.mKeyword = key
        }
        return mFindResult!!
    }

    override fun cancel() {
        mSearchLock.get().countDown()
    }

    fun getSourceList(): List<AdHocSession> {
        try {
            mSearchLock.get().await()
        }catch (ex: Exception) {}
        return mSearchList ?: listOf()
    }

    inner class SessionFindResult(key: String) : IBcmFindResult {

        private var mChanged = false
            @Synchronized get
            @Synchronized set

        var mKeyword: String = ""
            set(value) {
                if (field != value) {
                    mChanged = true
                    field = value
                }
            }

        init {
            mKeyword = key
        }

        override fun get(position: Int): BcmFindData<AdHocSession>? {
            val r = getSourceList()[position]
            return BcmFindData(r)
        }

        override fun count(): Int {
            return getSourceList().size
        }

        override fun topN(n: Int): List<BcmFindData<AdHocSession>> {
            mChanged = false
            val list = getSourceList()
            ALog.d(TAG, "topN begin: ${list.size}")
            val resultList = mutableListOf<BcmFindData<AdHocSession>>()
            if (mKeyword.isNotEmpty()) {
                var i = 0
                for (s in list) {
                    if (mChanged) {
                        return listOf()
                    }
                    if (StringAppearanceUtil.containIgnore(s.displayName(accountContext), mKeyword)) {
                        resultList.add(BcmFindData(s))
                        i++
                    }
                    if (i >= n) {
                        break
                    }
                }
            }
            ALog.d(TAG, "topN end: ${resultList.size}")
            return resultList
        }

        override fun toList(): List<BcmFindData<AdHocSession>> {
            mChanged = false
            val list = getSourceList()
            ALog.d(TAG, "toList begin: ${list.size}")
            val resultList = mutableListOf<BcmFindData<AdHocSession>>()
            if (mKeyword.isNotEmpty()) {
                for (s in list) {
                    if (mChanged) {
                        return listOf()
                    }
                    if (StringAppearanceUtil.containIgnore(s.displayName(accountContext), mKeyword)) {
                        resultList.add(BcmFindData(s))
                    }
                }
            }
            ALog.d(TAG, "toList end: ${resultList.size}")
            return resultList
        }

    }

}