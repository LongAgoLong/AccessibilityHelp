package com.leo.accessibilityhelp.lifecyle

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.leo.accessibilityhelp.BuildConfig
import com.leo.system.log.LogType
import com.leo.system.log.ZLog

class AppObserver : LifecycleObserver {
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    private fun init() {
        ZLog.setType(if (BuildConfig.DEBUG) LogType.VERBOSE else LogType.ASSERT)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    private fun appToTheBackground() {
        // 应用回到后台

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    private fun appToTheFrontDesk() {
        // 应用回到前台

    }
}