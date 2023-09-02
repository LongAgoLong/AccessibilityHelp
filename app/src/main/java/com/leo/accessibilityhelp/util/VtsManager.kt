package com.leo.accessibilityhelp.util

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.os.SystemClock
import android.text.TextUtils
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.leo.accessibilityhelp.extensions.readAssetsStringList
import com.leo.accessibilityhelp.ui.view.FloatingView
import com.leo.accessibilityhelplib.AccessibilityHelp
import com.leo.accessibilityhelplib.callback.IActivityInfoImpl
import com.leo.accessibilityhelplib.util.VtsTouchUtils
import com.leo.commonutil.notify.ToastUtil
import com.leo.commonutil.storage.IOUtil
import com.leo.commonutil.storage.SPHelp
import com.leo.system.context.ContextHelper
import com.leo.system.log.ZLog
import java.util.Locale

class VtsManager : IActivityInfoImpl {
    private val skipChars = mutableListOf<String>()
    private val skipIds = mutableListOf<String>()
    private val activityBlackList = mutableListOf<String>()
    private val pkgBlackList = mutableListOf<String>()

    private var mParams: WindowManager.LayoutParams? = null
    private var mWindowManager: WindowManager? = null
    var mFloatingView: FloatingView? = null

    /**
     * 是否显示Info弹框
     */
    var isShowInfoView = false
        set(value) {
            if (value == field) {
                return
            }
            if (value) addInfoView() else removeInfoView()
            field = value
        }

    /**
     * 是否开启拦截广告功能
     */
    var isInterceptAD: Boolean = false
        set(value) {
            SPHelp.getInstance().put(key = KEY_INTERCEPT_AD, o = value)
            ToastUtil.show(text = "${if (value) "已开启" else "已关闭"}开屏广告拦截功能")
            field = value
        }

    companion object {
        const val TAG = "VtsManager"
        const val KEY_INTERCEPT_AD = "interceptAd"

        const val AD_IDS = "skipIds.txt"
        const val AD_TEXTS = "skipTexts.txt"
        const val AD_ACT_BLACK = "activityBlackList.txt"
        const val PACKAGES = "pkgBlackList.txt"
        private var mInstance: VtsManager? = null
        fun getInstance(): VtsManager {
            return mInstance ?: synchronized(this) {
                mInstance ?: VtsManager().also { mInstance = it }
            }
        }
    }

    /**
     * 初始化
     */
    fun init() {
        loadConfigFromSdcard()
        AccessibilityHelp.getInstance().mIActivityInfoImpl = this
    }

    private fun loadConfigFromSdcard() {
        val idList = IOUtil.readAssetsStringList(ContextHelper.context, AD_IDS)
        if (idList.isNotEmpty()) {
            this.skipIds.addAll(idList)
        }
        val adTexts = IOUtil.readAssetsStringList(ContextHelper.context, AD_TEXTS)
        if (adTexts.isNotEmpty()) {
            this.skipChars.addAll(adTexts)
        }
        val actBlackList =
            IOUtil.readAssetsStringList(ContextHelper.context, AD_ACT_BLACK)
        if (actBlackList.isNotEmpty()) {
            this.activityBlackList.addAll(actBlackList)
        }
        val pkgBlackList = IOUtil.readAssetsStringList(ContextHelper.context, PACKAGES)
        if (pkgBlackList.isNotEmpty()) {
            this.pkgBlackList.addAll(pkgBlackList)
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
                mFloatingView!!.setOnCloseCallback(callback = object :
                    FloatingView.OnCloseCallback {
                    override fun onClose() {
                        removeInfoView()
                    }
                })
                mFloatingView!!.layoutParams = mParams
                mWindowManager?.addView(mFloatingView, mParams)
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

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // TODO 加一个事件流
        if (event.packageName.isNullOrEmpty() || event.className.isNullOrEmpty()) {
            return
        }
        val pkgName = event.packageName.toString()
        val activityName = event.className.toString()
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            mFloatingView?.updateInfo(pkgName, activityName)
        }
        // 过滤应用不拦截
        if (pkgBlackList.contains(pkgName)) {
            return
        }
        // 黑名单的activity也不检测
        if (activityBlackList.contains(activityName)) {
            return
        }
        val nodeList = findNodeList(event)
        if (nodeList.isNullOrEmpty()) {
            return
        }

        for (node in nodeList) {
            if (!TextUtils.isEmpty(node.text)) {
                // 防止会员跳过的按钮
                val s = node.text.toString().toLowerCase(Locale.getDefault())
                if (s.contains("vip")
                    || s.contains("会员")
                    || s.contains("會員")
                ) {
                    continue
                }
            }
            if (VtsTouchUtils.performClick(node)) {
                break
            }
        }
    }

    private fun findNodeList(event: AccessibilityEvent): List<AccessibilityNodeInfo>? {
        var nodeList: List<AccessibilityNodeInfo>? = null
        for (s in skipChars) {
            if (!event.source?.findAccessibilityNodeInfosByText(s).also {
                    nodeList = it
                }.isNullOrEmpty()) {
                break
            }
        }
        if (nodeList.isNullOrEmpty()) {
            for (id in skipIds) {
                if (!event.source?.findAccessibilityNodeInfosByViewId(id).also {
                        nodeList = it
                    }.isNullOrEmpty()) {
                    break
                }
            }
        }
        return nodeList
    }
}