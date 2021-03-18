package org.wikipedia.auth

import android.accounts.AbstractAccountAuthenticator
import android.app.Service
import android.content.Intent
import android.os.IBinder

class AuthenticatorService : Service() {
    private var authenticator: AbstractAccountAuthenticator? = null

    override fun onCreate() {
        super.onCreate()
        authenticator = WikimediaAuthenticator(this)
    }

    override fun onBind(intent: Intent): IBinder? {
        return authenticator?.iBinder
    }
}
