package com.leo.accessibilityhelp

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.text.TextUtils
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.leo.accessibilityhelp.view.FloatingView
import com.leo.accessibilityhelplib.AccessibilityHelp
import com.leo.accessibilityhelplib.callback.IActivityInfoImpl
import com.leo.commonutil.app.AppInfoUtil
import com.leo.commonutil.notify.NotificationCompatUtil
import com.leo.commonutil.notify.ToastUtil
import com.leo.system.LogUtil


class CstService : Service(), IActivityInfoImpl {
    var mParams: WindowManager.LayoutParams? = null
    var mWindowManager: WindowManager? = null
    var mFloatingView: FloatingView? = null
    var mIsInterceptAD: Boolean = false
    var mAbEvent: AccessibilityEvent? = null
    var mMatchCount: Int = 0

    companion object {
        const val NOTIFY_ID = 10086

        const val TAG = "CstService"
        const val TYPE = "type"
        const val TYPE_COMMAND = 101
        const val TYPE_INTERCEPT_AD = 102

        const val KEY_COMMAND = "command"
        const val COMMAND_OPEN = "COMMAND_OPEN"
        const val COMMAND_CLOSE = "COMMAND_CLOSE"
        const val KEY_INTERCEPT_AD = "intercept_ad"
        const val MATCH_COUNT = 2
        val REGEX = Regex("[0-9]{0,1}(s|S|\\s|)(\\n|)(跳过|关闭广告)(\\n|)[0-9]{0,1}(s|S|\\s|)")
    }

    override fun onDestroy() {
        LogUtil.i(TAG, "onDestroy")
        removeView()
        AccessibilityHelp.instance.mIActivityInfoImpl = null
        stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        foreground()
        intent?.let {
            LogUtil.d(TAG, "onStartCommand")
            initTrackerWindowManager()
            AccessibilityHelp.instance.mIActivityInfoImpl = this@CstService
            when (it.getIntExtra(TYPE, TYPE_COMMAND)) {
                TYPE_COMMAND -> {
                    val command = it.getStringExtra(KEY_COMMAND)
                    command?.let { s ->
                        if (TextUtils.equals(s, COMMAND_OPEN)) {
                            addView()
                        } else if (TextUtils.equals(s, COMMAND_CLOSE)) {
                            removeView()
                        }
                    }
                }
                TYPE_INTERCEPT_AD -> {
                    mIsInterceptAD = it.getBooleanExtra(KEY_INTERCEPT_AD, false)
                    if (mIsInterceptAD) {
                        ToastUtil.show(this, "已开启开屏广告拦截功能")
                    } else {
                        ToastUtil.show(this, "已关闭开屏广告拦截功能")
                    }
                }
                else -> {
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun foreground() {
        val ordinaryNotification = NotificationCompatUtil.getInstance(this)
            .createOrdinaryNotification(
                this,
                null,
                AppInfoUtil.getAppName(),
                "${AppInfoUtil.getAppName()}的前台服务",
                R.drawable.ic_launcher_foreground,
                null,
                false
            )
        startForeground(NOTIFY_ID, ordinaryNotification)
    }

    private fun initTrackerWindowManager() {
        mWindowManager = this@CstService.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val params = WindowManager.LayoutParams()
        params.x = 0
        params.y = 0
        params.width = WindowManager.LayoutParams.WRAP_CONTENT
        params.height = WindowManager.LayoutParams.WRAP_CONTENT
        params.gravity = Gravity.START or Gravity.TOP
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            params.type = WindowManager.LayoutParams.TYPE_PHONE
        }
        params.format = PixelFormat.RGBA_8888
        params.flags =
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        mParams = params
    }

    private fun addView() {
        if (null == mFloatingView) {
            synchronized(lock = CstService::class, block = {
                if (null == mFloatingView) {
                    mFloatingView = FloatingView(this@CstService)
                    mFloatingView!!.layoutParams = mParams
                    mWindowManager?.addView(mFloatingView, mParams)
                }
            })
        }
    }

    private fun removeView() {
        mFloatingView?.let {
            mWindowManager?.removeView(mFloatingView)
            mFloatingView = null
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            mFloatingView?.updateInfo(event.packageName.toString(), event.className.toString())
        } else if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            val nodeInfo = AccessibilityHelp.instance.nodeInfo
            if (null != nodeInfo) {
                LogUtil.i(
                    TAG,
                    "nodeInfo is ${nodeInfo.className}"
                )
                if (mIsInterceptAD) {
                    mAbEvent = event
                    val targetNode = matchNodeInfo(nodeInfo)
                    mMatchCount = 0
                    canPerformClick(targetNode)
                }
            }
        }
    }

    private fun matchNodeInfo(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (null == node) {
            LogUtil.d(TAG, "matchNodeInfo() null == mRootNodeInfo")
            return null
        }
        if (!TextUtils.isEmpty(node.text) && node.text.contains(REGEX)) {
            LogUtil.d(TAG, "matchNodeInfo() match text is ${node.text}")
            return node
        } else {
            val childCount = node.childCount
            if (childCount > 0) {
                for (i in 0 until childCount) {
                    val tempNode = node.getChild(i)
                    if (tempNode != null) {
                        LogUtil.d(
                            TAG, "matchNodeInfo() tempNode : "
                                    + tempNode.className + ", text : " + tempNode.text
                        )
                    }
                    val targetNode = matchNodeInfo(tempNode)
                    if (null != targetNode) {
                        return targetNode
                    }
                }
            } else {
                return null
            }
        }
        return null
    }

    /**
     * 模拟点击策略
     */
    @Synchronized
    private fun canPerformClick(nodeInfo: AccessibilityNodeInfo?): Boolean {
        if (nodeInfo == null) {
            LogUtil.i(TAG, "canPerformClick() return false, mMatchCount= $mMatchCount")
            return false
        }
        if (nodeInfo.isClickable) {
            LogUtil.i(TAG, "canPerformClick() return true, mMatchCount= $mMatchCount")
            performClick(nodeInfo)
            return true
        } else if (mMatchCount < MATCH_COUNT) {
            mMatchCount++
            return canPerformClick(nodeInfo.parent)
        }
        LogUtil.i(TAG, "mMatchCount = $mMatchCount")
        return false
    }

    /**
     * 执行点击事件，默认点击目标控件的可点击父控件
     */
    private fun performClick(nodeInfo: AccessibilityNodeInfo?): Boolean {
        if (nodeInfo == null) {
            LogUtil.i(TAG, "performClick() return false")
            return false
        }
        if (nodeInfo.isClickable) {
            return nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            val result = performClick(nodeInfo.parent)
            if (result) {
                return result
            }
        }
        return false
    }
}
