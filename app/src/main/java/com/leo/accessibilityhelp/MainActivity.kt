package com.leo.accessibilityhelp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.leo.accessibilityhelplib.AccessibilityHelp
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    companion object {
        private const val REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkOverlayPermission()
        initView()
    }

    private fun initView() {
        /**
         * 开启UI类完整包名和类名悬浮窗
         */
        openActivityTrackerBtn.setOnClickListener {
            if (AccessibilityHelp.instance.checkAccessibility(this@MainActivity)) {
                switchFloatingViewState(true)
            }
        }
        closeActivityTrackerBtn.setOnClickListener {
            switchFloatingViewState(false)
        }
        closeServiceBtn.setOnClickListener {
            closeService()
        }
    }

    /**
     * 开启/关闭悬浮窗
     */
    private fun switchFloatingViewState(b: Boolean) {
        val intent = Intent(this@MainActivity, CstService::class.java)
        intent.putExtra(CstService.COMMAND, if (b) CstService.COMMAND_OPEN else CstService.COMMAND_CLOSE)
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
                val uri = Uri.parse("package:$packageName")
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri)
                startActivityForResult(intent, REQUEST_CODE)
                Toast.makeText(this, "请先授予悬浮窗权限", Toast.LENGTH_LONG).show()
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
