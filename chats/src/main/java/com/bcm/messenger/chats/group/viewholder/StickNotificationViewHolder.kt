package com.bcm.messenger.chats.group.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bcm.messenger.chats.group.logic.GroupLogic
import com.bcm.messenger.common.AccountContext
import com.bcm.messenger.common.grouprepository.model.AmeGroupMessageDetail
import com.bcm.messenger.common.ui.BaseAccountHolder
import kotlinx.android.synthetic.main.chats_stick_notification_item.view.*

/**
 * ViewHolder for group message pin to top
 * Created by wjh on 2019/5/24
 */
class StickNotificationViewHolder(accountContext: AccountContext, containerView: View) : BaseAccountHolder(accountContext, containerView) {

    fun bindData(messageRecord: AmeGroupMessageDetail) {
        itemView.chats_stick_notification_layout?.setGroupInfo(GroupLogic.get(accountContext).getGroupInfo(messageRecord.gid)
                ?: return)
        itemView.chats_stick_notification_layout?.showLoading(messageRecord.isSending)
    }
}