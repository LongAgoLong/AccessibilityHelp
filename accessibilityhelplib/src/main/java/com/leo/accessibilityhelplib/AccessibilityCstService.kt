package com.leo.accessibilityhelplib

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.leo.system.LogUtil

class AccessibilityCstService : AccessibilityService() {
    val TAG = AccessibilityCstService::class.simpleName

    override fun onInterrupt() {
        //To change body of created functions use File | Settings | File Templates.
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        LogUtil.i(TAG, "onServiceConnected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        //To change body of created functions use File | Settings | File Templates.
        LogUtil.i(TAG, "onAccessibilityEvent")
        AccessibilityHelp.instance.nodeInfo = rootInActiveWindow
        event?.let {
            AccessibilityHelp.instance.notifyEvent(it)
        }
    }
}