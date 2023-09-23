package com.leo.accessibilityhelp

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.leo.accessibilityhelp.lifecyle.AppObserver
import com.leo.accessibilityhelp.vts.VtsManager

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppObserver())
        VtsManager.getInstance().init()
    }
}