package com.leo.accessibilityhelp.vts

import android.view.accessibility.AccessibilityEvent
import com.leo.accessibilityhelp.extensions.getPhoneAllActivities
import com.leo.accessibilityhelp.extensions.readAssetsStringList
import com.leo.accessibilityhelp.ui.view.FloatingView
import com.leo.accessibilityhelp.vts.dispatch.AdsDispatch
import com.leo.accessibilityhelp.vts.dispatch.AppInfoDispatch
import com.leo.accessibilityhelp.vts.dispatch.VtsBaseDispatch
import com.leo.accessibilityhelp.vts.dispatch.WechatRedPacketDispatch
import com.leo.accessibilityhelplib.AccessibilityHelp
import com.leo.accessibilityhelplib.callback.IActivityInfoImpl
import com.leo.accessibilityhelplib.util.HandlerUtils
import com.leo.commonutil.app.AppInfoUtil
import com.leo.commonutil.storage.IOUtil
import com.leo.system.context.ContextHelper

class VtsManager : IActivityInfoImpl {

    private val allAppInfoMap = mutableMapOf<String, List<String>>()

    /**
     * 分发器
     */
    private val allDispatch: MutableList<VtsBaseDispatch>
    private val mWechatRedPacketDispatch: WechatRedPacketDispatch
    private val mAdsDispatch: AdsDispatch
    private val mAppInfoDispatch: AppInfoDispatch

    companion object {
        const val TAG = "VtsManager"
        const val KEY_INTERCEPT_AD = "interceptAd"


        const val PACKAGES = "pkgBlackList.txt"
        private var mInstance: VtsManager? = null
        fun getInstance(): VtsManager {
            return mInstance ?: synchronized(this) {
                mInstance ?: VtsManager().also { mInstance = it }
            }
        }
    }

    init {
        loadAllAppInfo()
        mWechatRedPacketDispatch = WechatRedPacketDispatch(allAppInfoMap)
        mAdsDispatch = AdsDispatch(allAppInfoMap)
        mAppInfoDispatch = AppInfoDispatch(allAppInfoMap)
        allDispatch = mutableListOf(mWechatRedPacketDispatch, mAdsDispatch, mAppInfoDispatch)
    }

    /**
     * 初始化
     */
    fun init() {
        HandlerUtils.getVtsHandler().post {
            // TODO 监听系统安装卸载应用
            AccessibilityHelp.getInstance().mIActivityInfoImpl = this
        }
    }

    /**
     * 是否显示app信息浮窗
     * @param isShow Boolean
     * @param onClose Function0<Unit>
     */
    fun toggleAppInfoView(isShow: Boolean, onClose: () -> Unit = {}) {
        mAppInfoDispatch.isShowInfoView = isShow
        mAppInfoDispatch.mFloatingView?.setOnCloseCallback(object :
            FloatingView.OnCloseCallback {
            override fun onClose() {
                onClose()
            }
        })
    }

    fun isAppInfoShow(): Boolean {
        return mAppInfoDispatch.isShowInfoView
    }

    /**
     * 是否拦截广告
     * @param isIntercept Boolean
     */
    fun toggleAdsIntercept(isIntercept: Boolean) {
        mAdsDispatch.isInterceptAD = isIntercept
    }

    fun isAdsIntercept(): Boolean {
        return mAdsDispatch.isInterceptAD
    }

    fun toggleAutoOpenWechatRedPacket(isAutoOpenWechatRedPacket: Boolean) {
        mWechatRedPacketDispatch.isAutoOpenWechatRedPacket = isAutoOpenWechatRedPacket
    }

    fun isAutoOpenWechatRedPacket(): Boolean {
        return mWechatRedPacketDispatch.isAutoOpenWechatRedPacket
    }

    private fun loadAllAppInfo() {
        val allAppInfoMap = AppInfoUtil.getPhoneAllActivities()
        this.allAppInfoMap.putAll(allAppInfoMap)
        val pkgBlackList = IOUtil.readAssetsStringList(ContextHelper.context, PACKAGES)
        pkgBlackList.forEach {
            this.allAppInfoMap.remove(it)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        HandlerUtils.getVtsHandler().post {
            val source = event.source
            allDispatch.forEach {
                it.onVtsEventDispatch(event, source)
            }
        }
    }
}