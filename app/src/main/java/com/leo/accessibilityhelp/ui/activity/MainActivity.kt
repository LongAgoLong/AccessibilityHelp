package com.leo.accessibilityhelp.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.leo.accessibilityhelp.R
import com.leo.accessibilityhelp.databinding.ActivityMainBinding
import com.leo.accessibilityhelp.util.ServiceHelp
import com.leo.accessibilityhelplib.AccessibilityHelp
import com.leo.commonutil.app.AppInfoUtil
import com.leo.commonutil.notify.ToastUtil
import com.leo.system.IntentUtil


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
        mBinding.openActivityTrackerBtn.setOnClickListener {
            if (!checkOverlayPermission()) {
                return@setOnClickListener
            }
            if (!AccessibilityHelp.getInstance().checkAccessibility(this@MainActivity)) {
                ToastUtil.show(text = "请授予${AppInfoUtil.appName}无障碍服务权限")
            }
            ServiceHelp.getInstance().switchFloatingViewState(true)
        }
        mBinding.closeActivityTrackerBtn.setOnClickListener {
            if (!checkOverlayPermission()) {
                return@setOnClickListener
            }
            ServiceHelp.getInstance().switchFloatingViewState(false)
        }
        /**
         * 开启/关闭开屏广告拦截功能
         */
        mBinding.openInterceptAdBtn.setOnClickListener {
            if (!AccessibilityHelp.getInstance().checkAccessibility(this@MainActivity)) {
                ToastUtil.show(text = "请授予${AppInfoUtil.appName}无障碍服务权限")
            }
            ServiceHelp.getInstance().switchInterceptAd(true)
        }
        mBinding.closeInterceptAdBtn.setOnClickListener {
            if (!AccessibilityHelp.getInstance().checkAccessibility(this@MainActivity)) {
                ToastUtil.show(text = "请授予${AppInfoUtil.appName}无障碍服务权限")
            }
            ServiceHelp.getInstance().switchInterceptAd(false)
        }

        mBinding.checkPermissionBtn.setOnClickListener {
            if (!AccessibilityHelp.getInstance().checkAccessibility(this@MainActivity)) {
                ToastUtil.show(text = "请授予${AppInfoUtil.appName}无障碍服务权限")
            } else {
                ToastUtil.show(text = "无障碍服务已授权")
            }
        }
        mBinding.closeServiceBtn.setOnClickListener {
            ServiceHelp.getInstance().unBindService()
            android.os.Process.killProcess(android.os.Process.myPid())
        }
        mBinding.reloadBtn.setOnClickListener {
            ServiceHelp.getInstance().reloadFromSd()
        }
        mBinding.toEditFileBtn.setOnClickListener {
            IntentUtil.startActivity(this@MainActivity, EditConfigActivity::class.java)
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

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && !Settings.canDrawOverlays(this)) {
            ToastUtil.show(text = "无法获取悬浮窗权限！！！")
        }
    }
}
