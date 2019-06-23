package com.leo.accessibilityhelp

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import com.leo.accessibilityhelp.view.FloatingView
import com.leo.accessibilityhelplib.AccessibilityHelp
import com.leo.accessibilityhelplib.callback.IActivityInfoImpl
import com.leo.system.ContextHelp
import com.leo.system.LogUtil

class CstService : Service(), IActivityInfoImpl {
    var mParams: WindowManager.LayoutParams? = null
    var mWindowManager: WindowManager? = null
    var mFloatingView: FloatingView? = null

    companion object {
        const val TAG = "CstService"
        const val COMMAND = "COMMAND"
        const val COMMAND_OPEN = "COMMAND_OPEN"
        const val COMMAND_CLOSE = "COMMAND_CLOSE"
    }

    override fun onDestroy() {
        super.onDestroy()
        LogUtil.i(TAG, "onDestroy")
        removeView()
        AccessibilityHelp.instance.mIActivityInfoImpl = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            LogUtil.d(TAG, "onStartCommand")
            initTrackerWindowManager()
            AccessibilityHelp.instance.mIActivityInfoImpl = this@CstService
            val command = it.getStringExtra(COMMAND)
            if (command != null) {
                if (command == COMMAND_OPEN) {
                    addView()
                } else if (command == COMMAND_CLOSE) {
                    removeView()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
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
        params.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
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

    override fun onActivityInfo(packageName: String, className: String) {
        mFloatingView?.updateInfo(packageName, className)
        val nodeInfo = AccessibilityHelp.instance.nodeInfo
        nodeInfo?.let {
            LogUtil.e(TAG, it.toString())
        }
    }
}
