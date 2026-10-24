package com.ehealthinformatics.prognocare.feature.auth

import android.content.Intent
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult

/**
 * Process-wide bridge between [com.ehealthinformatics.prognocare.MainActivity.onActivityResult]
 * and the Compose login screen.
 *
 * The Facebook SDK drives login through the classic
 * `onActivityResult → CallbackManager` route, which cannot be wired from a
 * composable alone. MainActivity forwards every activity result here; the
 * login screen installs a one-shot [onFacebookResult] right before starting
 * the Facebook dialog.
 */
object FacebookAuthBridge {
    val callbackManager: CallbackManager = CallbackManager.Factory.create()

    /** One-shot handler installed by the login screen before launching FB. */
    @Volatile
    var onFacebookResult: ((accessToken: String?, cancelled: Boolean, error: String?) -> Unit)? =
        null

    init {
        LoginManager.getInstance().registerCallback(
            callbackManager,
            object : FacebookCallback<LoginResult> {
                override fun onSuccess(result: LoginResult) {
                    onFacebookResult?.invoke(result.accessToken.token, false, null)
                    onFacebookResult = null
                }

                override fun onCancel() {
                    onFacebookResult?.invoke(null, true, null)
                    onFacebookResult = null
                }

                override fun onError(error: FacebookException) {
                    onFacebookResult?.invoke(null, false, error.message ?: "Facebook sign-in failed")
                    onFacebookResult = null
                }
            },
        )
    }

    /** Returns true when the result was consumed by the Facebook callback. */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean =
        callbackManager.onActivityResult(requestCode, resultCode, data)
}
