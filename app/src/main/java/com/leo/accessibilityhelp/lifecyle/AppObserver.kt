package com.leo.accessibilityhelp.lifecyle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.leo.accessibilityhelp.BuildConfig
import com.leo.accessibilityhelp.util.ServiceHelp
import com.leo.system.LogUtil
import com.leo.system.enume.LogType

class AppObserver : LifecycleObserver {
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    private fun init() {
        LogUtil.setType(if (BuildConfig.DEBUG) LogType.VERBOSE else LogType.ASSERT)
        ServiceHelp.getInstance().bindService()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private fun appToTheBackground() {

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun appToTheFrontDesk() {

    }
}