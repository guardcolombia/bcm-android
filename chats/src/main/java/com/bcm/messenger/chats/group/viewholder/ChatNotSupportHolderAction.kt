package com.bcm.messenger.chats.group.viewholder

import com.bcm.messenger.chats.R
import com.bcm.messenger.common.AccountContext
import com.bcm.messenger.common.core.AmeGroupMessage
import com.bcm.messenger.common.grouprepository.model.AmeGroupMessageDetail
import com.bcm.messenger.common.mms.GlideRequests
import com.bcm.messenger.common.ui.emoji.EmojiTextView
import com.bcm.messenger.common.utils.getAttrColor
import com.bcm.messenger.common.utils.getColorCompat

/**
 * 
 * Created by wjh on 2018/10/23
 */
open class ChatNotSupportHolderAction(accountContext: AccountContext) : BaseChatHolderAction<EmojiTextView>(accountContext) {

    override fun bindData(message: AmeGroupMessageDetail, body: EmojiTextView, glideRequests: GlideRequests, batchSelected: Set<AmeGroupMessageDetail>?) {

        val text = textFromMessage(message)
        if (text.isEmpty()) {
            return
        }

        if (!message.isSendByMe) {
            if (message.message.type == AmeGroupMessage.NONSUPPORT) {
                body.setTextColor(body.context.getAttrColor(R.attr.common_text_third_color))
            } else {
                body.setTextColor(body.context.getAttrColor(R.attr.common_text_main_color))
            }
        } else {
            body.setTextColor(body.context.getAttrColor(R.attr.common_white_color))
        }

        ChatViewHolder.interceptMessageText(body, message, text)

    }

    private fun textFromMessage(record: AmeGroupMessageDetail): String {
        val data = record.message?.content as? AmeGroupMessage.TextContent
        return if (null != data) {
            data.text
        } else {
            (record.message?.content as? AmeGroupMessage.LinkContent)?.url ?: ""
        }
    }

    override fun resend(messageRecord: AmeGroupMessageDetail) {

    }

}