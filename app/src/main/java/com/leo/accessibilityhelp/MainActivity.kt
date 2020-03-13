package com.leo.accessibilityhelp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.leo.accessibilityhelp.databinding.ActivityMainBinding
import com.leo.accessibilityhelp.util.ServiceHelp
import com.leo.accessibilityhelplib.AccessibilityHelp
import com.leo.commonutil.app.AppInfoUtil
import com.leo.commonutil.notify.ToastUtil


class MainActivity : AppCompatActivity() {
    private lateinit var mBinding: ActivityMainBinding

    companion object {
        private const val REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        initView()
        checkOverlayPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        ServiceHelp.getInstance().unBindService()
    }

    private fun initView() {
        /**
         * 开启UI类完整包名和类名悬浮窗
         */
        mBinding.openActivityTrackerBtn.setOnClickListener {
            if (!AccessibilityHelp.getInstance().checkAccessibility(this@MainActivity)) {
                ToastUtil.show(text = "请授予${AppInfoUtil.appName}无障碍服务权限")
            }
            ServiceHelp.getInstance().switchFloatingViewState(true)
        }
        mBinding.closeActivityTrackerBtn.setOnClickListener {
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
        }
    }

    /**
     * 开启/关闭悬浮窗
     */
    private fun switchFloatingViewState(b: Boolean) {
        val intent = Intent(this, CstService::class.java)
        intent.putExtra(CstService.TYPE, CstService.TYPE_COMMAND)
        intent.putExtra(
            CstService.KEY_COMMAND,
            if (b) CstService.COMMAND_OPEN else CstService.COMMAND_CLOSE
        )
        startService(intent)
    }

    private fun switchInterceptAd(b: Boolean) {
        val intent = Intent(this, CstService::class.java)
        intent.putExtra(CstService.TYPE, CstService.TYPE_INTERCEPT_AD)
        intent.putExtra(CstService.KEY_INTERCEPT_AD, b)
        startService(intent)
    }

    /**
     * 关闭服务
     */
    private fun closeService() {
        val intent = Intent(this@MainActivity, CstService::class.java)
        stopService(intent)
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                ToastUtil.show(text = "请授予${AppInfoUtil.appName}悬浮窗权限")
                val uri = Uri.parse("package:$packageName")
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri)
                startActivityForResult(intent, REQUEST_CODE)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && !Settings.canDrawOverlays(this)) {
            checkOverlayPermission()
        }
    }
}
