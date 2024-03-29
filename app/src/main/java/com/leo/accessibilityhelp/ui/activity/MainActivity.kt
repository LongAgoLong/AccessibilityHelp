package com.leo.accessibilityhelp.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.databinding.DataBindingUtil
import com.leo.accessibilityhelp.R
import com.leo.accessibilityhelp.databinding.ActivityMainBinding
import com.leo.accessibilityhelp.extensions.setCheckedNoEvent
import com.leo.accessibilityhelp.vts.VtsManager
import com.leo.accessibilityhelplib.AccessibilityHelp
import com.leo.commonutil.app.AppInfoUtil
import com.leo.commonutil.notify.ToastUtil


class MainActivity : BaseActivity() {
    private lateinit var mBinding: ActivityMainBinding

    companion object {
        private const val REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(
            this,
            R.layout.activity_main
        )
        initView()
    }

    private fun initView() {
        /**
         * 开启UI类完整包名和类名悬浮窗
         */
        mBinding.activityTrackerSwitch.isChecked = VtsManager.getInstance().isAppInfoShow()
        mBinding.activityTrackerSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked && !checkOverlayPermission()) {
                buttonView?.run { setCheckedNoEvent(false) }
            } else if (isChecked && !AccessibilityHelp.getInstance()
                    .checkAccessibility(this@MainActivity)
            ) {
                buttonView?.run { setCheckedNoEvent(false) }
                ToastUtil.show(text = "请授予${AppInfoUtil.appName}无障碍服务权限")
            } else {
                VtsManager.getInstance().toggleAppInfoView(isChecked, onClose = {
                    mBinding.activityTrackerSwitch.isChecked = false
                })
            }
        }
        /**
         * 开启/关闭开屏广告拦截功能
         */
        mBinding.interceptAdSwitch.isChecked = VtsManager.getInstance().isAdsIntercept()
        mBinding.interceptAdSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!AccessibilityHelp.getInstance().checkAccessibility(this@MainActivity)) {
                buttonView?.run { setCheckedNoEvent(false) }
            } else {
                VtsManager.getInstance().toggleAdsIntercept(isChecked)
            }
        }

        /**
         * 微信红包功能
         */
        mBinding.wechatRedPacketSwitch.isChecked =
            VtsManager.getInstance().isAutoOpenWechatRedPacket()
        mBinding.wechatRedPacketSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!AccessibilityHelp.getInstance().checkAccessibility(this@MainActivity)) {
                buttonView?.run { setCheckedNoEvent(false) }
            } else {
                VtsManager.getInstance().toggleAutoOpenWechatRedPacket(isChecked)
            }
        }
    }

    private fun checkOverlayPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                ToastUtil.show(text = "请授予${AppInfoUtil.appName}悬浮窗权限")
                val uri = Uri.parse("package:$packageName")
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri)
                startActivityForResult(
                    intent,
                    REQUEST_CODE
                )
                return false
            }
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && !Settings.canDrawOverlays(this)) {
            ToastUtil.show(text = "无法获取悬浮窗权限！！！")
        }
    }
}
