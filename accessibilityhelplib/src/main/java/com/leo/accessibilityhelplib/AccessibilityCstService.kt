package com.leo.accessibilityhelplib

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.leo.commonutil.asyn.threadPool.ThreadPoolHelp
import com.leo.system.LogUtil
import java.util.concurrent.TimeUnit

class AccessibilityCstService : AccessibilityService() {
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
        LogUtil.i(TAG, "onServiceConnected")
        super.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        //To change body of created functions use File | Settings | File Templates.
//        LogUtil.i(
//            TAG,
//            "onAccessibilityEvent : pkg is ${event?.packageName}; cls name is ${event?.className}"
//        )
        val future = ThreadPoolHelp.submit { rootInActiveWindow }
        try {
            val nodeInfo = future.get(3, TimeUnit.SECONDS)
            nodeInfo?.run {
                AccessibilityHelp.getInstance().nodeInfo = this
            }
            event?.run {
                AccessibilityHelp.getInstance().notifyEvent(this)
            }
        } catch (e: Exception) {
//            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        LogUtil.i(TAG, "onDestroy")
        super.onDestroy()
    }
}