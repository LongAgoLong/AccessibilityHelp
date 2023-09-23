package com.leo.accessibilityhelp.vts.dispatch

import android.text.TextUtils
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.leo.accessibilityhelp.extensions.readAssetsStringList
import com.leo.accessibilityhelp.vts.VtsManager
import com.leo.accessibilityhelplib.AccessibilityHelp
import com.leo.accessibilityhelplib.util.VtsTouchUtils
import com.leo.commonutil.notify.ToastUtil
import com.leo.commonutil.storage.IOUtil
import com.leo.commonutil.storage.SPHelp
import com.leo.system.context.ContextHelper
import com.leo.system.log.ZLog

class AdsDispatch(private val allAppInfoMap: Map<String, List<String>>) : VtsBaseDispatch {
    private val skipIds = mutableListOf<String>()
    private val skipChars = mutableListOf<String>()

    /**
     * 上一次命中的界面
     */
    private var lastHitPkgAndActivity: String = ""

    /**
     * 是否开启拦截广告功能
     */
    @Volatile
    var isInterceptAD: Boolean = false
        set(value) {
            SPHelp.getInstance().put(key = VtsManager.KEY_INTERCEPT_AD, o = value)
            if (field == value) {
                return
            }
            ToastUtil.show(text = "${if (value) "已开启" else "已关闭"}开屏广告拦截功能")
            field = value
        }


    init {
        val idList = IOUtil.readAssetsStringList(ContextHelper.context, AD_IDS)
        if (idList.isNotEmpty()) {
            this.skipIds.addAll(idList)
        }
        val adTexts = IOUtil.readAssetsStringList(ContextHelper.context, AD_TEXTS)
        if (adTexts.isNotEmpty()) {
            this.skipChars.addAll(adTexts)
        }
        val settingsOn =
            AccessibilityHelp.getInstance().isAccessibilitySettingsOn(ContextHelper.context)
        isInterceptAD = if (settingsOn) {
            SPHelp.getInstance()
                .getBoolean(ContextHelper.context, KEY_ADS_INTERCEPT, false)
        } else {
            false
        }
    }

    override fun onVtsEventDispatch(event: AccessibilityEvent, source: AccessibilityNodeInfo?) {
        if (event.packageName.isNullOrEmpty() || event.className.isNullOrEmpty()) {
            return
        }
        val pkgName = event.packageName.toString()
        val activityName = event.className.toString()
        val allActivities = allAppInfoMap[pkgName] ?: return
        if (!allActivities.contains(activityName)) {
            ZLog.e(TAG, "onAccessibilityEvent: not match activity!")
            return
        }
        synchronized(lock) {
            val currentHitResult = "$pkgName-$activityName"
            if (TextUtils.equals(currentHitResult, lastHitPkgAndActivity)
                || !isInterceptAD
            ) {
                return
            }
            lastHitPkgAndActivity = currentHitResult
            ZLog.d(TAG, "onAccessibilityEvent: activity = $currentHitResult")
            val nodeList = findNodeList(source)
            if (nodeList.isNullOrEmpty()) {
                return
            }

            for (node in nodeList) {
                if (VtsTouchUtils.performClick(node)) {
                    break
                }
            }
        }
    }

    private fun findNodeList(source: AccessibilityNodeInfo?): List<AccessibilityNodeInfo>? {
        var nodeList: List<AccessibilityNodeInfo>? = null
        for (id in skipIds) {
            if (!source?.findAccessibilityNodeInfosByViewId(id).also {
                    nodeList = it
                }.isNullOrEmpty()) {
                break
            }
        }
        if (nodeList.isNullOrEmpty()) {
            for (s in skipChars) {
                if (!source?.findAccessibilityNodeInfosByText(s).also {
                        nodeList = it
                    }.isNullOrEmpty()) {
                    break
                }
            }
        }
        return nodeList
    }

    companion object {
        private const val TAG = "AdsDispatch"
        private const val AD_IDS = "skipIds.txt"
        private const val AD_TEXTS = "skipTexts.txt"
        private const val KEY_ADS_INTERCEPT = "key_ads_intercept"
        private val lock = Any()
    }
}
