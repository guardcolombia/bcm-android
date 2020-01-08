package com.bcm.messenger.me.ui.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import com.bcm.messenger.common.ARouterConstants
import com.bcm.messenger.login.logic.AmeLoginLogic
import com.bcm.messenger.me.R
import com.bcm.route.annotation.Route

/**
 * Created by ling
 */
@Route(routePath = ARouterConstants.Activity.USER_REGISTER_PATH)
class RegistrationActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "RegistrationActivity"
        const val RE_LOGIN_ID = "RE_LOGIN_ID"
        const val REQUEST_CODE_SCAN_QR_LOGIN = 10013
        const val CREATE_ACCOUNT_ID = "CREATE_ACCOUNT"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_SCAN_QR_LOGIN && resultCode == Activity.RESULT_OK) {
            val scanResult = data?.getStringExtra(ARouterConstants.PARAM.SCAN.SCAN_RESULT) ?: return
            AmeLoginLogic.saveBackupFromExportModelWithWarning(scanResult, true) { id ->
                if (!id.isNullOrEmpty()) {
                    val f = LoginVerifyPinFragment()
                    val arg = Bundle()
                    arg.putString(RE_LOGIN_ID, id)
                    f.arguments = arg
                    supportFragmentManager.beginTransaction()
                            .replace(R.id.register_container, f)
                            .addToBackStack("sms")
                            .commit()
                }
            }

        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.me_activity_registration)

        if (intent.getBooleanExtra(CREATE_ACCOUNT_ID, false)) {
            handleFirstGoToLogin(null)
        } else {
            val lastLoginUid = AmeLoginLogic.accountHistory.getLastLoginUid()
            handleFirstGoToLogin(lastLoginUid)
        }
    }

    private fun handleFirstGoToLogin(lastLoginUid: String?) {
        if (!lastLoginUid.isNullOrEmpty()) {
            val f = ReloginFragment()
            val arg = Bundle()
            arg.putString(RE_LOGIN_ID, lastLoginUid)
            f.arguments = arg

            supportFragmentManager.beginTransaction()
                    .add(R.id.register_container, f, "relogin")
                    .commitNowAllowingStateLoss()
        } else {
            val f = StartupFragment()
            val arg = Bundle()
            arg.putBoolean(CREATE_ACCOUNT_ID, intent.getBooleanExtra(CREATE_ACCOUNT_ID, false))
            f.arguments = arg
            supportFragmentManager.beginTransaction()
                    .add(R.id.register_container, f, "startup")
                    .commitNowAllowingStateLoss()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (supportFragmentManager.backStackEntryCount > 0) {
                supportFragmentManager.popBackStack()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

}