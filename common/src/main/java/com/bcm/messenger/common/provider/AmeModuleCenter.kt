package com.bcm.messenger.common.provider

import com.bcm.messenger.common.ARouterConstants
import com.bcm.messenger.common.AccountContext
import com.bcm.messenger.common.AccountJobManager
import com.bcm.messenger.common.core.AmeFileUploader
import com.bcm.messenger.common.deprecated.DatabaseFactory
import com.bcm.messenger.common.database.repositories.Repository
import com.bcm.messenger.common.preferences.TextSecurePreferences
import com.bcm.messenger.common.provider.accountmodule.*
import com.bcm.messenger.common.server.*
import com.bcm.messenger.common.service.RotateSignedPreKeyListener
import com.bcm.messenger.common.utils.AccountContextMap
import com.bcm.messenger.utility.AppContextHolder
import com.bcm.messenger.utility.logger.ALog
import com.sdk.crashreport.ReportUtils
import org.whispersystems.jobqueue.JobManager

object AmeModuleCenter {
    private val serverDataDispatcher: ServerDataDispatcher = ServerDataDispatcher()
    private val daemonScheduler = DaemonScheduler()
    private val serverConnDaemons = AccountContextMap {
        ServerConnectionDaemon(it, daemonScheduler, serverDataDispatcher)
    }

    fun instance() {
        login().restoreLastLoginState()
        login().initModule()
    }

    fun init(accountContext: AccountContext) {
        group(accountContext)?.initModule()
        chat(accountContext)?.initModule()
        contact(accountContext)?.initModule()
        user(accountContext)?.initModule()
        metric(accountContext)?.initModule()
        wallet(accountContext)?.initModule()

    }

    fun unInit(accountContext: AccountContext) {
        group(accountContext)?.uninitModule()
        chat(accountContext)?.uninitModule()
        contact(accountContext)?.uninitModule()
        user(accountContext)?.uninitModule()
        metric(accountContext)?.uninitModule()
        wallet(accountContext)?.uninitModule()
        AmeProvider.removeModule(accountContext)
    }

    fun removeModule(accountContext: AccountContext, providerName: String) {
        AmeProvider.removeModule(accountContext, providerName)
    }

    fun login(): ILoginModule {
        return AmeProvider.get(ARouterConstants.Provider.PROVIDER_LOGIN_BASE)!!
    }

    fun group(accountContext: AccountContext): IGroupModule? {
        return AmeProvider.getAccountModule(ARouterConstants.Provider.PROVIDER_GROUP_BASE, accountContext)
    }

    fun chat(accountContext: AccountContext): IChatModule? {
        return AmeProvider.getAccountModule(ARouterConstants.Provider.PROVIDER_CONVERSATION_BASE, accountContext)
    }

    fun contact(accountContext: AccountContext): IContactModule? {
        return AmeProvider.getAccountModule(ARouterConstants.Provider.PROVIDER_CONTACTS_BASE, accountContext)
    }

    fun user(accountContext: AccountContext): IUserModule? {
        return AmeProvider.getAccountModule(ARouterConstants.Provider.PROVIDER_USER_BASE, accountContext)
    }

    fun wallet(accountContext: AccountContext): IWalletModule? {
        return AmeProvider.getAccountModule(ARouterConstants.Provider.PROVIDER_WALLET_BASE, accountContext)
    }

    fun metric(accountContext: AccountContext): IMetricsModule? {
        return AmeProvider.getAccountModule(ARouterConstants.Provider.REPORT_BASE, accountContext)
    }

    fun adhoc(): IAdHocModule {
        return AmeProvider.get(ARouterConstants.Provider.PROVIDER_AD_HOC)!!
    }

    fun app(): IAmeAppModule {
        return AmeProvider.get(ARouterConstants.Provider.PROVIDER_APPLICATION_BASE)!!
    }

    fun serverDispatcher(): IServerDataDispatcher {
        return serverDataDispatcher
    }

    fun serverDaemon(accountContext: AccountContext): IServerConnectionDaemon {
        return serverConnDaemons.get(accountContext)
    }

    fun accountJobMgr(context: AccountContext): JobManager? {
        return AccountJobManager.get(context)
    }

    fun onLoginSucceed(accountContext: AccountContext) {
        ALog.i("AmeModuleCenter", "AccountLogin ${accountContext.tag}")

        accountJobMgr(accountContext)
        ALog.logForSecret("AmeModuleCenter", "updateAccount ${accountContext.tag} ${accountContext.uid}")
        AmeFileUploader.get(accountContext)
        ReportUtils.setGUid(accountContext.uid)

        if (DatabaseFactory.isDatabaseExist(accountContext, AppContextHolder.APP_CONTEXT) && !TextSecurePreferences.isDatabaseMigrated(accountContext)) {
            return
        } else {
            Repository.getInstance(accountContext)
        }

        this.init(accountContext)

        contact(accountContext)?.doForLogin()

        if (!adhoc().isAdHocMode()) {
            serverDaemon(accountContext).startDaemon()
            serverDaemon(accountContext).startConnection()
        }

        RotateSignedPreKeyListener.schedule(AppContextHolder.APP_CONTEXT, accountContext)

        System.gc()

    }

    fun onLogOutSucceed(accountContext: AccountContext) {
        if (serverConnDaemons.containsKey(accountContext)) {
            val serverdaemon = serverConnDaemons.get(accountContext)
            serverdaemon.stopConnection()
            serverdaemon.stopDaemon()
        }

        unInit(accountContext)
        Repository.accountLogOut(accountContext)
        serverConnDaemons.remove(accountContext)
        AccountJobManager.remove(accountContext)
        AmeFileUploader.remove(accountContext)
    }

}