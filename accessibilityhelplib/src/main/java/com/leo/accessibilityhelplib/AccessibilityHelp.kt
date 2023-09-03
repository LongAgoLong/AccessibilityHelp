package com.leo.accessibilityhelplib

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.provider.Settings.Secure
import android.provider.Settings.Secure.ACCESSIBILITY_ENABLED
import android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
import android.provider.Settings.SettingNotFoundException
import android.view.accessibility.AccessibilityEvent
import com.leo.accessibilityhelplib.callback.IActivityInfoImpl
import java.util.Locale


class AccessibilityHelp {
    var mIActivityInfoImpl: IActivityInfoImpl? = null
    var mService: AccessibilityVtsService? = null

    companion object {
        /**
         * 懒加载单例
         */
//        val instance by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
//            AccessibilityHelp()
//        }

        @Volatile
        private var instance: AccessibilityHelp? = null

        fun getInstance() =
            instance ?: synchronized(this) {
                instance ?: AccessibilityHelp().also { instance = it }
            }
    }

    /**
     * 判断辅助功能是否开启
     */
    fun checkAccessibility(context: Context): Boolean {
        if (!isAccessibilitySettingsOn(context)) {
            // 引导至辅助功能设置页面
            context.startActivity(
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            )
            return false
        }
        return true
    }

    fun isAccessibilitySettingsOn(context: Context): Boolean {
        var accessibilityEnabled = 0
        try {
            accessibilityEnabled = Secure.getInt(
                context.contentResolver,
                ACCESSIBILITY_ENABLED
            )
        } catch (e: SettingNotFoundException) {
            e.printStackTrace()
        }

        if (accessibilityEnabled == 1) {
            val services = Secure.getString(
                context.contentResolver,
                ENABLED_ACCESSIBILITY_SERVICES
            )
            services?.run {
                return lowercase(Locale.getDefault())
                    .contains(
                        context.packageName.lowercase(Locale.getDefault())
                    )
            }
        }
        return false
    }

    fun notifyEvent(event: AccessibilityEvent) {
        mIActivityInfoImpl?.onAccessibilityEvent(event)
    }
}