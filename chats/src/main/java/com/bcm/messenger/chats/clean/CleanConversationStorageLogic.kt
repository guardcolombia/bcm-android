package com.bcm.messenger.chats.clean

import android.util.Log
import com.bcm.messenger.common.AccountContext
import com.bcm.messenger.common.core.Address
import com.bcm.messenger.common.database.repositories.Repository
import com.bcm.messenger.common.grouprepository.manager.MessageDataManager
import com.bcm.messenger.common.provider.bean.ConversationStorage
import com.bcm.messenger.common.utils.GroupUtil
import com.bcm.messenger.utility.logger.ALog
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.ConcurrentHashMap

object CleanConversationStorageLogic {

    interface ConversationStorageCallback {
        fun onCollect(finishedConversation: Address?, allFinished: Boolean)
        fun onClean(finishedConversation: Address?, allFinished: Boolean)
    }

    private const val TAG = "ConversationStorage"

    val ADDRESS_ALL: Address = Address.from(AccountContext("","",""), "clean_all")

    private val storageMap = ConcurrentHashMap<Address, ConversationStorage>()
    private var threadList: List<Address>? = null
        @Synchronized set
        @Synchronized get

    private var collecting = false
        @Synchronized set
        @Synchronized get
    private var cancelCollection = false
        @Synchronized set
        @Synchronized get
    private var collectionFinish = false // true collect finish, false collecting
        @Synchronized set
        @Synchronized get
    private var cleaning = false
        @Synchronized set
        @Synchronized get

    private var mCallbackMap = mutableMapOf<String, ConversationStorageCallback>()


    fun clearCache() {
        ALog.i(TAG, "clearCache")
        this.storageMap.clear()
        this.threadList = null
    }

    fun removeCallback(tag: String = TAG) {
        ALog.i(TAG, "removeCallback tag: $tag")
        mCallbackMap.remove(tag)
    }

    fun addCallback(tag: String = TAG, callback: ConversationStorageCallback) {
        ALog.i(TAG, "addCallback tag: $tag")
        mCallbackMap[tag] = callback
    }

    fun collectionAllConversationStorageSize(accountContext: AccountContext) {
        if (collecting) {
            return
        }
        collecting = true
        cancelCollection = false
        collectionFinish = false
        handleAllConversationStorageCollect(accountContext)
    }


    private fun handleAllConversationStorageCollect(accountContext: AccountContext) {
        Observable.create<Pair<Address?, Boolean>> {
            try {
                val list = Repository.getThreadRepo(accountContext)?.getAllThreads()?.map { Address.from(accountContext, it.uid) }?: listOf()
                this.threadList = list
                var timestamp = System.currentTimeMillis()

                for (address in list){
                    if (!cancelCollection){
                        val size = collectionConversationStorageSize(accountContext, address)
                        storageMap[address] = size
                        if (System.currentTimeMillis() - timestamp > 500){
                            timestamp = System.currentTimeMillis()
                            it.onNext(Pair(address, false))
                        }
                    } else {
                        throw Exception("cancel")
                    }
                }

                collectionFinish = true
                it.onNext(Pair(null, true))

            } catch (e: Exception) {
                it.onError(e)
            } finally {
                it.onComplete()
            }

        }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({resultPair ->
                    if (resultPair.first == null && resultPair.second) {
                        collecting = false
                    }
                    for ((tag, callback) in mCallbackMap) {
                        callback.onCollect(resultPair.first, resultPair.second)
                    }

                }, {
                    ALog.e(TAG, "handleAllConversationStorageCollect error", it)
                    collecting = false

                    for ((tag, callback) in mCallbackMap) {
                        callback.onCollect(null, true)
                    }
                })
    }

    fun isAllCollectionFinished(): Boolean {
        return collectionFinish
    }

    fun isCollectedFinished(address: Address): Boolean {
        return storageMap.containsKey(address)
    }

    fun cancelCollectionAllConversationStorageSize() {
        cancelCollection = true
        collectionFinish = false
    }

    fun getConversationList(): List<Address> {
        val array = mutableListOf<Address>()
        array.add(ADDRESS_ALL)
        array.addAll(threadList ?: listOf())
        return array
    }

    fun getConversationStorageSize(address: Address): Long {
        return storageMap[address]?.storageUsed()?:0
    }

    fun getAllConversationStorageSize(): ConversationStorage {
        val allConversationStorage = ConversationStorage(0, 0, 0)
        for ((_, size) in storageMap){
            allConversationStorage.append(size)
        }
        return allConversationStorage
    }

    fun clearAllConversationMediaMessage(accountContext: AccountContext, type: Int) {
        if (getStorageSizeByType(type) > 0) {
            if (cleaning) {
                return
            }
            handleAllConversationStorageClean(accountContext, type)
        } else {
            for ((tag, callback) in mCallbackMap) {
                callback.onClean(null, true)
            }
        }
    }

    private fun handleAllConversationStorageClean(accountContext: AccountContext, type: Int) {
        Observable.create<Pair<Address?, Boolean>> {

            try {
                val timestamp = System.currentTimeMillis()
                val list = Repository.getThreadRepo(accountContext)?.getAllThreads()?: listOf()
                for (record in list) {
                    val address = Address.from(accountContext, record.uid)
                    if (address.isIndividual){
                        val threadId = record.id
                        if (threadId > 0) {
                            Repository.getChatRepo(accountContext)?.deleteConversationMediaMessages(threadId, type)
                        }
                    } else if (address.isNewGroup) {
                        MessageDataManager.deleteAllMediaMessage(accountContext, GroupUtil.gidFromAddress(address), type)
                    }

                    clearStorageSizeByTypeWithConversation(address, type)
                    if (System.currentTimeMillis() - timestamp > 500) {
                        it.onNext(Pair(address, false))
                    }
                }
                it.onNext(Pair(null, true))

            } catch (ex: Exception) {
                it.onError(ex)
            } finally {
                it.onComplete()
            }

        }.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({resultPair ->
                    if (resultPair.first == null && resultPair.second) {
                        cleaning = false
                    }
                    for ((tag, callback) in mCallbackMap) {
                        callback.onClean(resultPair.first, resultPair.second)
                    }

                }, {
                    ALog.e(TAG, "handleAllConversationStorageClean error", it)
                    cleaning = false
                    for ((tag, callback) in mCallbackMap) {
                        callback.onClean(null, true)
                    }
                })
    }

    fun messageDeletedForConversation(address: Address?, storageType: Int, fileSize: Long) {
        if (address == null){
            return
        }

        if (storageMap.isNotEmpty()){
            val conversationStorage = storageMap[address]
            if (null != conversationStorage){
                if (ConversationStorage.testFlag(storageType, ConversationStorage.TYPE_FILE)){
                    conversationStorage.fileSize -= fileSize
                } else if (ConversationStorage.testFlag(storageType, ConversationStorage.TYPE_VIDEO)){
                    conversationStorage.videoSize -= fileSize
                } else if (ConversationStorage.testFlag(storageType, ConversationStorage.TYPE_IMAGE)){
                    conversationStorage.imageSize -= fileSize
                }
            }
        }
    }

    private fun clearStorageSizeByTypeWithConversation(address: Address, type:Int) {
        val size = storageMap[address]
        if (null != size){
            if (ConversationStorage.testFlag(type, ConversationStorage.TYPE_FILE)){
                size.fileSize = 0L
            }

            if (ConversationStorage.testFlag(type, ConversationStorage.TYPE_VIDEO)){
                size.videoSize = 0L
            }

            if (ConversationStorage.testFlag(type, ConversationStorage.TYPE_IMAGE)){
                size.imageSize = 0L
            }
        }
    }

    fun getStorageSizeByType(type:Int): Long {
        val storage = getAllConversationStorageSize()
        var size = 0L
        if (ConversationStorage.testFlag(type, ConversationStorage.TYPE_FILE)){
            size += storage.fileSize
        }

        if (ConversationStorage.testFlag(type, ConversationStorage.TYPE_VIDEO)){
            size += storage.videoSize
        }

        if (ConversationStorage.testFlag(type, ConversationStorage.TYPE_IMAGE)){
            size += storage.imageSize
        }
        return size
    }

    private fun collectionConversationStorageSize(accountContext: AccountContext, address: Address): ConversationStorage {
        if (address.isIndividual){
            val threadId = Repository.getThreadRepo(accountContext)?.getThreadIdIfExist(address.serialize())?:0
            if (threadId > 0){
                return Repository.getChatRepo(accountContext)?.getConversationStorageSize(threadId)?: ConversationStorage(0,0,0)
            }
        } else if(address.isNewGroup) {
            return MessageDataManager.fetchMediaMessageStorageSize(accountContext, GroupUtil.gidFromAddress(address))
        }

        Log.e(TAG, "unknown session")
        return ConversationStorage(0, 0, 0)
    }

}