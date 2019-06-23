package com.leo.accessibilityhelp

import android.app.Application
import com.leo.system.ContextHelp

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ContextHelp.setContext(this@App)
    }
}