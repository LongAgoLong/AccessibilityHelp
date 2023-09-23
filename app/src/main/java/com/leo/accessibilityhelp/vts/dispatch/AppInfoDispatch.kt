package com.leo.accessibilityhelp.vts.dispatch

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.leo.accessibilityhelp.ui.view.FloatingView
import com.leo.accessibilityhelp.vts.VtsManager
import com.leo.accessibilityhelplib.AccessibilityHelp
import com.leo.accessibilityhelplib.util.HandlerUtils
import com.leo.commonutil.storage.SPHelp
import com.leo.system.context.ContextHelper
import com.leo.system.log.ZLog

class AppInfoDispatch(private val allAppInfoMap: Map<String, List<String>>) : VtsBaseDispatch {

    private var mParams: WindowManager.LayoutParams? = null
    private var mWindowManager: WindowManager? = null
    var mFloatingView: FloatingView? = null

    /**
     * 是否显示Info弹框
     */
    var isShowInfoView = false
        set(value) {
            SPHelp.getInstance().put(key = KEY_APP_INFO, o = value)
            if (value == field) {
                return
            }
            field = value
            HandlerUtils.getMainHandler().post {
                if (field) addInfoView() else removeInfoView()
            }
        }

    init {
        val settingsOn =
            AccessibilityHelp.getInstance().isAccessibilitySettingsOn(ContextHelper.context)
                    && Settings.canDrawOverlays(ContextHelper.context)
        isShowInfoView = if (settingsOn) {
            SPHelp.getInstance().getBoolean(
                ContextHelper.context,
                KEY_APP_INFO, false
            )
        } else {
            false
        }
    }

    companion object {
        private const val KEY_APP_INFO = "key_app_info"
    }

    override fun onVtsEventDispatch(event: AccessibilityEvent, source: AccessibilityNodeInfo?) {
        if (event.packageName.isNullOrEmpty() || event.className.isNullOrEmpty()) {
            return
        }
        val pkgName = event.packageName.toString()
        val activityName = event.className.toString()
        val allActivities = allAppInfoMap[pkgName] ?: return
        if (!allActivities.contains(activityName)) {
            ZLog.e(VtsManager.TAG, "onAccessibilityEvent: not match activity!")
            return
        }
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            HandlerUtils.getMainHandler().post {
                mFloatingView?.updateInfo(pkgName, activityName)
            }
        }
    }

    private fun initTrackerWindowManager() {
        mParams ?: run {
            mWindowManager =
                ContextHelper.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val params = WindowManager.LayoutParams()
            params.run {
                x = 0
                y = 0
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT
                gravity = Gravity.START or Gravity.TOP
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_PHONE
                }
                format = PixelFormat.RGBA_8888
                flags =
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            }
            mParams = params
        }
    }

    /**
     * 显示信息悬浮窗
     */
    private fun addInfoView() {
        mFloatingView ?: synchronized(this) {
            mFloatingView ?: run {
                initTrackerWindowManager()
                mFloatingView = FloatingView(ContextHelper.context)
                mFloatingView!!.layoutParams = mParams
                mWindowManager?.addView(mFloatingView, mParams)
                AccessibilityHelp.getInstance().mService?.run {
                    mFloatingView!!.updateInfo(
                        this.rootInActiveWindow.packageName.toString(),
                        this.rootInActiveWindow.className.toString()
                    )
                }
            }
        }
    }

    /**
     * 关闭信息悬浮窗
     */
    private fun removeInfoView() {
        mFloatingView?.let {
            mWindowManager?.removeView(it)
        }
        mFloatingView = null
    }
}
