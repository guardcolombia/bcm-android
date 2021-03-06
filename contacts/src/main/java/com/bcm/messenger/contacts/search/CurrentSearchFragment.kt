package com.bcm.messenger.contacts.search

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.bcm.messenger.common.ARouterConstants
import com.bcm.messenger.common.BaseFragment
import com.bcm.messenger.common.api.ISearchAction
import com.bcm.messenger.common.api.ISearchCallback
import com.bcm.messenger.common.core.corebean.AmeGroupInfo
import com.bcm.messenger.common.event.HomeTopEvent
import com.bcm.messenger.common.finder.BcmFinderManager
import com.bcm.messenger.common.finder.BcmFinderType
import com.bcm.messenger.common.finder.SearchItemData
import com.bcm.messenger.common.provider.AmeModuleCenter
import com.bcm.messenger.common.recipients.Recipient
import com.bcm.messenger.common.ui.activity.SearchActivity
import com.bcm.messenger.common.utils.startBcmActivity
import com.bcm.messenger.contacts.R
import com.bcm.messenger.utility.logger.ALog
import com.bcm.route.api.BcmRouter
import kotlinx.android.synthetic.main.contacts_fragment_current_search.*

/**
 * Created by wjh on 2019/4/3
 */
@SuppressLint("ValidFragment")
class CurrentSearchFragment : BaseFragment(), ISearchAction {

    private val TAG = "CurrentSearchFragment"

    private var mKeyword = ""
    private var mSearchLimit = false
    private var mTypes: Array<BcmFinderType> = arrayOf(BcmFinderType.ADDRESS_BOOK, BcmFinderType.GROUP, BcmFinderType.USER_ID)
    private var mAdapter: SearchResultListAdapter? = null

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            mAdapter?.setSearchResult(listOf(), "")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.contacts_fragment_current_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        search_shade.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        search_list.layoutManager = LinearLayoutManager(context)

        mAdapter = SearchResultListAdapter(context
                ?: return, object : SearchResultListAdapter.SearchActionListener {

            override fun onSelect(data: SearchItemData) {
                val a = activity
                when (data.type) {
                    BcmFinderType.ADDRESS_BOOK, BcmFinderType.USER_ID -> {
                        val r = data.tag as Recipient
                        if (a is ISearchCallback) {
                            a.onSelect(data.type, r.address.serialize())
                        }
                        AmeModuleCenter.app().gotoHome(accountContext, HomeTopEvent(true,
                                HomeTopEvent.ConversationEvent.fromPrivateConversation(r.address.serialize(), true)))
                    }
                    BcmFinderType.GROUP -> {
                        val g = data.tag as AmeGroupInfo
                        if (a is ISearchCallback) {
                            a.onSelect(data.type, g.gid.toString())
                        }
                        AmeModuleCenter.app().gotoHome(accountContext, HomeTopEvent(true,
                                HomeTopEvent.ConversationEvent.fromGroupConversation(g.gid)))
                    }
                    else -> {
                    }
                }
            }

            override fun onMore(data: SearchItemData) {
                val a = activity
                if (a is ISearchCallback) {
                    a.onMore(data.type, mKeyword)
                }
                SearchActivity.callSearchActivity(a
                        ?: return, accountContext, mKeyword, true, true, CurrentSearchFragment::class.java.name, null, SearchActivity.REQUEST_SEARCH_MORE)
            }

            override fun onAction(data: SearchItemData) {
                val recipient = data.tag as? Recipient ?: return
                BcmRouter.getInstance().get(ARouterConstants.Activity.REQUEST_FRIEND)
                        .putParcelable(ARouterConstants.PARAM.PARAM_ADDRESS, recipient.address)
                        .startBcmActivity(accountContext, context)
            }

        })
        search_list.adapter = mAdapter

        ALog.d(TAG, "onViewCreate keyword: $mKeyword searchLimit: $mSearchLimit")
        if (mKeyword.isNotEmpty() && mTypes.isNotEmpty()) {
            doSearchAction(mKeyword, mSearchLimit, mTypes)
        }
    }

    override fun setKeyword(keyword: String, searchLimit: Boolean) {
        ALog.d(TAG, "setCurrentKeyword keyword: $keyword searchLimit: $searchLimit")
        mKeyword = keyword
        mSearchLimit = searchLimit
        if (isAdded) {
            doSearchAction(mKeyword, mSearchLimit, mTypes)
        } else {
            ALog.d(TAG, "setCurrentKeyword fail, is not added")
        }
    }

    private fun doSearchAction(keyword: String, searchLimit: Boolean, types: Array<BcmFinderType>) {
        ALog.d(TAG, "doSearchAction keyword: $keyword")
        val callback: (result: List<SearchItemData>) -> Unit = { resultList ->
            if (keyword == mKeyword) {
                mAdapter?.setSearchResult(resultList, keyword)
                search_list?.post {
                    hideLoading()
                    if (mAdapter?.itemCount == 0) {
                        showEmpty()
                    } else {
                        hideEmpty()
                    }
                }
            }
        }
        showLoading()
        if (searchLimit) {
            BcmFinderManager.get(accountContext).querySearchResultLimit(keyword, types, callback)
        } else {
            BcmFinderManager.get(accountContext).querySearchResult(keyword, types, callback)
        }
    }

    private fun showLoading() {
        ALog.d(TAG, "showLoading")
        try {
            search_shade?.showLoading()
        } catch (ex: Exception) {
            ALog.e(TAG, "showLoading error", ex)
        }
    }

    private fun hideLoading() {
        ALog.d(TAG, "hideLoading")
        try {
            search_shade?.hide()
        } catch (ex: Exception) {
            ALog.e(TAG, "hideLoading ex", ex)
        }
    }

    private fun showEmpty() {
        search_shade.showContent(getString(R.string.contacts_result_from_remote_empty), getString(R.string.contacts_please_check_and_try_again))
    }

    private fun hideEmpty() {
        search_shade.hide()
    }

}