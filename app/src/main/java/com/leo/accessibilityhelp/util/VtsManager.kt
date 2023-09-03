package com.leo.accessibilityhelp.util

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.text.TextUtils
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.leo.accessibilityhelp.extensions.getPhoneAllActivities
import com.leo.accessibilityhelp.extensions.readAssetsStringList
import com.leo.accessibilityhelp.ui.view.FloatingView
import com.leo.accessibilityhelplib.AccessibilityHelp
import com.leo.accessibilityhelplib.callback.IActivityInfoImpl
import com.leo.accessibilityhelplib.util.HandlerUtils
import com.leo.accessibilityhelplib.util.VtsTouchUtils
import com.leo.commonutil.app.AppInfoUtil
import com.leo.commonutil.notify.ToastUtil
import com.leo.commonutil.storage.IOUtil
import com.leo.commonutil.storage.SPHelp
import com.leo.system.context.ContextHelper
import com.leo.system.log.ZLog

class VtsManager : IActivityInfoImpl {

    private val skipChars = mutableListOf<String>()
    private val skipIds = mutableListOf<String>()
    private val allAppInfoMap = mutableMapOf<String, List<String>>()

    /**
     * 上一次命中的界面
     */
    private var lastHitPkgAndActivity: String = ""

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
            field = value
            HandlerUtils.getMainHandler().post {
                if (field) addInfoView() else removeInfoView()
            }
        }

    /**
     * 是否开启拦截广告功能
     */
    @Volatile
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
        HandlerUtils.getVtsHandler().post {
            loadConfigFromSdcard()
            // TODO 监听系统安装卸载应用
            AccessibilityHelp.getInstance().mIActivityInfoImpl = this
        }
    }

    /**
     * 加载配置
     */
    private fun loadConfigFromSdcard() {
        val idList = IOUtil.readAssetsStringList(ContextHelper.context, AD_IDS)
        if (idList.isNotEmpty()) {
            this.skipIds.addAll(idList)
        }
        val adTexts = IOUtil.readAssetsStringList(ContextHelper.context, AD_TEXTS)
        if (adTexts.isNotEmpty()) {
            this.skipChars.addAll(adTexts)
        }
        loadAllAppInfo()
    }

    private fun loadAllAppInfo() {
        val allAppInfoMap = AppInfoUtil.getPhoneAllActivities()
        this.allAppInfoMap.putAll(allAppInfoMap)
        val pkgBlackList = IOUtil.readAssetsStringList(ContextHelper.context, PACKAGES)
        pkgBlackList.forEach {
            this.allAppInfoMap.remove(it)
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
        HandlerUtils.getVtsHandler().post {
            if (event.packageName.isNullOrEmpty() || event.className.isNullOrEmpty()) {
                return@post
            }
            val pkgName = event.packageName.toString()
            val activityName = event.className.toString()
            val allActivities = allAppInfoMap[pkgName] ?: return@post
            if (!allActivities.contains(activityName)) {
                ZLog.e(TAG, "onAccessibilityEvent: not match activity!")
                return@post
            }
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                HandlerUtils.getMainHandler().post {
                    mFloatingView?.updateInfo(pkgName, activityName)
                }
            }
            val currentHitResult = "$pkgName-$activityName"
            if (TextUtils.equals(currentHitResult, lastHitPkgAndActivity)
                || !isInterceptAD
            ) {
                return@post
            }
            lastHitPkgAndActivity = currentHitResult
            ZLog.d(TAG, "onAccessibilityEvent: activity = $currentHitResult")
            val source = AccessibilityHelp.getInstance().mService ?: return@post
            val nodeList = findNodeList(source.rootInActiveWindow)
            if (nodeList.isNullOrEmpty()) {
                return@post
            }

            for (node in nodeList) {
                if (VtsTouchUtils.performClick(node)) {
                    break
                }
            }
        }
    }

    private fun findNodeList(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo>? {
        var nodeList: List<AccessibilityNodeInfo>? = null
        for (id in skipIds) {
            if (!root.findAccessibilityNodeInfosByViewId(id).also {
                    nodeList = it
                }.isNullOrEmpty()) {
                break
            }
        }
        if (nodeList.isNullOrEmpty()) {
            for (s in skipChars) {
                if (!root.findAccessibilityNodeInfosByText(s).also {
                        nodeList = it
                    }.isNullOrEmpty()) {
                    break
                }
            }
        }
        return nodeList
    }

    private fun findNodeList(event: AccessibilityEvent): List<AccessibilityNodeInfo>? {
        var nodeList: List<AccessibilityNodeInfo>? = null
        for (id in skipIds) {
            if (!event.source?.findAccessibilityNodeInfosByViewId(id).also {
                    nodeList = it
                }.isNullOrEmpty()) {
                break
            }
        }
        if (nodeList.isNullOrEmpty()) {
            for (s in skipChars) {
                if (!event.source?.findAccessibilityNodeInfosByText(s).also {
                        nodeList = it
                    }.isNullOrEmpty()) {
                    break
                }
            }
        }
        return nodeList
    }
}