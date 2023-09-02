package com.leo.accessibilityhelplib

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.leo.system.log.ZLog

class AccessibilityVtsService : AccessibilityService() {
    companion object {
        const val TAG = "AccessibilityCstService"
    }

    override fun onCreate() {
        super.onCreate()
    }

    override fun onInterrupt() {
        //To change body of created functions use File | Settings | File Templates.
    }

    override fun onServiceConnected() {
        ZLog.i(TAG, "onServiceConnected")
        super.onServiceConnected()
        AccessibilityHelp.getInstance().mService = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        //To change body of created functions use File | Settings | File Templates.
//        ZLog.i(
//            TAG,
//            "onAccessibilityEvent : pkg is ${event?.packageName}; cls name is ${event?.className}"
//        )
        try {
            val nodeInfo = rootInActiveWindow
            nodeInfo?.run {
                AccessibilityHelp.getInstance().nodeInfo = this
            }
            event?.run {
                AccessibilityHelp.getInstance().notifyEvent(this)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        ZLog.i(TAG, "onDestroy")
        AccessibilityHelp.getInstance().mService = null
        super.onDestroy()
    }
}