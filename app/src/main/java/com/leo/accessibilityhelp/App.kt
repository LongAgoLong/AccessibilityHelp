package com.leo.accessibilityhelp

import android.app.Application
import com.leo.accessibilityhelp.util.ServiceHelp
import com.leo.system.LogUtil
import com.leo.system.enume.LogType

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        LogUtil.setType(LogType.VERBOSE)
        ServiceHelp.getInstance().bindService()
    }
}