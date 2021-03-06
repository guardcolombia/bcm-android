package com.bcm.messenger.common.ui.popup

import com.bcm.messenger.common.ui.popup.centerpopup.*
import com.bcm.messenger.common.ui.popup.bottompopup.*

/**
 * bcm.social.01 2018/5/31.
 */
object AmePopup {
    //
    val center: AmeCenterPopup = AmeCenterPopup.instance()
    //
    val bottom: AmeBottomPopup = AmeBottomPopup.instance()
    //
    val loading: AmeLoadingPopup = AmeLoadingPopup()
    //loading
    val tipLoading = AmeTipLoadingPopup()
    //
    val result: AmeResultPopup = AmeResultPopup()
    //
    val progress: AmeProgressPopup = AmeProgressPopup()
}