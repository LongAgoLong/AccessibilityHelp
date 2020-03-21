package com.leo.accessibilityhelp

import android.app.Application
import com.leo.system.LogUtil
import com.leo.system.enume.LogType

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        LogUtil.setType(if (BuildConfig.DEBUG) LogType.VERBOSE else LogType.ASSERT)
    }
}