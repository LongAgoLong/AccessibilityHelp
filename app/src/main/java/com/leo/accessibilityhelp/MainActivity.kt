package com.leo.accessibilityhelp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import com.leo.accessibilityhelplib.AccessibilityHelp
import com.leo.commonutil.app.AppInfoUtil
import com.leo.commonutil.notify.ToastUtil
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
        checkOverlayPermission()
    }

    private fun initView() {
        /**
         * 开启UI类完整包名和类名悬浮窗
         */
        openActivityTrackerBtn.setOnClickListener {
            if (!AccessibilityHelp.getInstance().checkAccessibility(this@MainActivity)) {
                ToastUtil.show(this@MainActivity, "请授予${AppInfoUtil.getAppName()}无障碍服务权限")
            }
            switchFloatingViewState(true)
        }
        closeActivityTrackerBtn.setOnClickListener {
            switchFloatingViewState(false)
        }
        /**
         * 开启/关闭开屏广告拦截功能
         */
        openInterceptAdBtn.setOnClickListener {
            if (!AccessibilityHelp.getInstance().checkAccessibility(this@MainActivity)) {
                ToastUtil.show(this@MainActivity, "请授予${AppInfoUtil.getAppName()}无障碍服务权限")
            }
            switchInterceptAd(true)
        }
        closeInterceptAdBtn.setOnClickListener {
            switchInterceptAd(false)
        }

        checkPermissionBtn.setOnClickListener {
            if (!AccessibilityHelp.getInstance().checkAccessibility(this@MainActivity)) {
                ToastUtil.show(this, "请授予${AppInfoUtil.getAppName()}无障碍服务权限")
            } else {
                ToastUtil.show(this, "无障碍服务已授权")
            }
        }
        closeServiceBtn.setOnClickListener {
            closeService()
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
                ToastUtil.show(this, "请授予${AppInfoUtil.getAppName()}悬浮窗权限")
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
