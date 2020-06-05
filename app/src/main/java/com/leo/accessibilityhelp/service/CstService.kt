package com.leo.accessibilityhelp.service

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.text.TextUtils
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import com.leo.accessibilityhelp.R
import com.leo.accessibilityhelp.ui.view.FloatingView
import com.leo.accessibilityhelp.ui.view.FloatingView.OnCloseCallback
import com.leo.accessibilityhelplib.AccessibilityHelp
import com.leo.accessibilityhelplib.callback.IActivityInfoImpl
import com.leo.commonutil.app.AppInfoUtil
import com.leo.commonutil.asyn.threadPool.ThreadPoolHelp
import com.leo.commonutil.notify.NotificationHelp
import com.leo.commonutil.storage.IOUtil
import com.leo.commonutil.storage.SDcardUtil
import com.leo.system.LogUtil
import com.leo.system.ResHelp
import java.util.*
import kotlin.math.abs


class CstService : Service(), IActivityInfoImpl {
    private var mCurrentActivity: ActivityInfo? = null
    private val skipChars = mutableSetOf<String>()
    private val skipIds = mutableSetOf<String>()
    private val activityBlackList = mutableSetOf<String>()
    private val pkgBlackList = mutableSetOf<String>()
    private var mCheckCount = 0

    private var mParams: WindowManager.LayoutParams? = null
    private var mWindowManager: WindowManager? = null
    var mFloatingView: FloatingView? = null
    var mIsInterceptAD: Boolean = false
    private val binder: LocalBinder = LocalBinder()

    companion object {
        const val NOTIFY_ID = 10086
        const val NOTIFY_CHANNEL_ID = "ID10086"
        const val NOTIFY_CHANNEL_NAME = "前台服务"

        const val TAG = "CstService"
        private const val AD_IDS = "skipIds.txt"
        private const val AD_TEXTS = "skipTexts.txt"
        private const val AD_ACT_WHITE = "activityBlackList.txt"
        private const val PACKAGES = "pkgBlackList.txt"
        private const val VERSION_FILE = "version.txt"

        private const val MAX_CHECK_COUNT = 3
    }

    override fun onCreate() {
        super.onCreate()
        foreground()
        initTrackerWindowManager()
        AccessibilityHelp.getInstance().mIActivityInfoImpl = this@CstService
        /**
         * 初始化
         */
        ThreadPoolHelp.execute {
            if (checkVersion()) {
                IOUtil.delAllFile(SDcardUtil.fileFolder!!.absolutePath)
                val adTexts = ResHelp.getFileFromAssets(AD_TEXTS)
                adTexts?.let {
                    IOUtil.writeDiskText(
                        fileName = AD_TEXTS,
                        content = it,
                        base64Encode = false
                    )
                }
                val ids = ResHelp.getFileFromAssets(AD_IDS)
                ids?.let {
                    IOUtil.writeDiskText(
                        fileName = AD_IDS,
                        content = it,
                        base64Encode = false
                    )
                }

                val actBlackStr = ResHelp.getFileFromAssets(AD_ACT_WHITE)
                actBlackStr?.let {
                    IOUtil.writeDiskText(
                        fileName = AD_ACT_WHITE,
                        content = it,
                        base64Encode = false
                    )
                }

                val pkgBlackSts = ResHelp.getFileFromAssets(PACKAGES)
                pkgBlackSts?.let {
                    IOUtil.writeDiskText(
                        fileName = PACKAGES,
                        content = it,
                        base64Encode = false
                    )
                }
                val fileFromAssets = ResHelp.getFileFromAssets(VERSION_FILE)
                fileFromAssets?.let {
                    IOUtil.writeDiskText(
                        fileName = VERSION_FILE,
                        content = it,
                        base64Encode = false
                    )
                }
            }
            loadFromSdcard()
        }
    }

    private fun loadFromSdcard() {
        try {
            skipIds.clear()
            skipChars.clear()
            activityBlackList.clear()
            pkgBlackList.clear()
            val idsStr = IOUtil.getDiskText(fileName = AD_IDS)
            if (!TextUtils.isEmpty(idsStr)) {
                val list = idsStr!!.split("#")
                if (!list.isNullOrEmpty()) {
                    skipIds.addAll(list)
                }
            }
            val adTexts = IOUtil.getDiskText(fileName = AD_TEXTS)
            if (!TextUtils.isEmpty(adTexts)) {
                val list = adTexts!!.split("#")
                if (!list.isNullOrEmpty()) {
                    skipChars.addAll(list)
                }
            }
            val actBlackStr =
                IOUtil.getDiskText(fileName = AD_ACT_WHITE)
            if (!TextUtils.isEmpty(actBlackStr)) {
                val whiteList = actBlackStr!!.split("#")
                if (!whiteList.isNullOrEmpty()) {
                    activityBlackList.addAll(whiteList)
                }
            }
            val pkgBlackSts = IOUtil.getDiskText(fileName = PACKAGES)
            if (!TextUtils.isEmpty(pkgBlackSts)) {
                val pkgBlacks = pkgBlackSts!!.split("#")
                if (!pkgBlacks.isNullOrEmpty()) {
                    pkgBlackList.addAll(pkgBlacks)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkVersion(): Boolean {
        val diskVersionText =
            IOUtil.getDiskText(fileName = VERSION_FILE)
        val assetsVersion = ResHelp.getFileFromAssets(VERSION_FILE)!!.toInt()
        if (TextUtils.isEmpty(diskVersionText)) {
            return true
        } else {
            try {
                val toInt = diskVersionText!!.toInt()
                if (abs(toInt - assetsVersion) != 0) {
                    return true
                }
            } catch (e: NumberFormatException) {
                return true
            }
        }
        return false
    }

    override fun onDestroy() {
        removeView()
        AccessibilityHelp.getInstance().mIActivityInfoImpl = null
        stopForeground(true)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    private fun foreground() {
        val ordinaryNotification = NotificationHelp.getInstance(this)
            .ordinaryNotification(
                this,
                null,
                AppInfoUtil.appName,
                "${AppInfoUtil.appName}的前台服务",
                R.drawable.ic_launcher_foreground,
                bigContent = "",
                channelId = NOTIFY_CHANNEL_ID,
                channelName = NOTIFY_CHANNEL_NAME
            )
        startForeground(NOTIFY_ID, ordinaryNotification)
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        tryGetActivity(event)
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            mFloatingView?.updateInfo(
                event.packageName.toString(),
                mCurrentActivity?.name ?: event.className.toString()
            )
        }

        mCurrentActivity ?: return
        // 过滤应用不拦截
        if (pkgBlackList.contains(event.packageName)) {
            return
        }
        // 黑名单的activity也不检测
        if (!TextUtils.isEmpty(mCurrentActivity!!.name)
            && activityBlackList.contains(mCurrentActivity!!.name)
        ) {
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
                if (s.contains("vip") || s.contains("会员") || s.contains("會員")) {
                    continue
                }
            }
            mCheckCount = 0
            if (performClick(node)) {
                break
            }
        }
    }

    private fun initTrackerWindowManager() {
        mWindowManager = this@CstService.getSystemService(Context.WINDOW_SERVICE) as WindowManager
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

    private fun addView() {
        if (null == mFloatingView) {
            synchronized(lock = CstService::class, block = {
                if (null == mFloatingView) {
                    mFloatingView = FloatingView(this@CstService)
                    mFloatingView!!.setOnCloseCallback(callback = object : OnCloseCallback {
                        override fun onClose() {
                            removeView()
                        }
                    })
                    mFloatingView!!.layoutParams = mParams
                    mWindowManager?.addView(mFloatingView, mParams)
                }
            })
        }
    }

    private fun removeView() {
        mFloatingView?.let {
            mWindowManager?.removeView(it)
        }
        mFloatingView = null
    }

    /**
     * 执行点击事件，默认点击目标控件的可点击父控件
     */
    private fun performClick(nodeInfo: AccessibilityNodeInfo?): Boolean {
        if (nodeInfo == null) {
            LogUtil.i(TAG, "performClick() return false")
            return false
        }
        mCheckCount++
        if (mCheckCount > MAX_CHECK_COUNT) {
            return false
        }
        if (nodeInfo.isClickable) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                LogUtil.d(
                    TAG,
                    "performClick() currentActivity is ${mCurrentActivity?.name
                        ?: "null"} nodeInfo text is ${nodeInfo.text} ; id is ${nodeInfo.viewIdResourceName}"
                )
            } else {
                LogUtil.d(
                    TAG,
                    "performClick() nodeInfo text is ${nodeInfo.text}"
                )
            }
            return nodeInfo.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            val result = performClick(nodeInfo.parent)
            if (result) {
                return result
            }
        }
        return false
    }

    private fun tryGetActivity(event: AccessibilityEvent): ActivityInfo? {
        val componentName = ComponentName(event.packageName.toString(), event.className.toString())
        try {
            mCurrentActivity = packageManager.getActivityInfo(componentName, 0)
            LogUtil.e(TAG, "mCurrentActivity name is ${mCurrentActivity?.name ?: "null"}")
        } catch (e: PackageManager.NameNotFoundException) {
        }
        return mCurrentActivity
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
        if (nodeList.isNullOrEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
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

    inner class LocalBinder : Binder() {
        fun switchFloatingViewState(b: Boolean): Boolean {
            if (b && mFloatingView == null) {
                addView()
            } else if (!b && mFloatingView != null) {
                removeView()
            }
            return true
        }

        fun switchInterceptAd(b: Boolean): Boolean {
            mIsInterceptAD = b
            return true
        }

        fun reloadFromSd() {
            ThreadPoolHelp.execute { loadFromSdcard() }
        }
    }
}
