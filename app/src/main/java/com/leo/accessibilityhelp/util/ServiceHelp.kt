package com.leo.accessibilityhelp.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.leo.accessibilityhelp.CstService
import com.leo.commonutil.notify.ToastUtil
import com.leo.system.ContextHelp
import com.leo.system.LogUtil

class ServiceHelp {
    private val TAG = "ServiceHelp"
    private val RECONNECT_ID = 1001
    private var mBinder: CstService.LocalBinder? = null
    private val mHandler: Handler = Handler(Looper.getMainLooper(), Handler.Callback { msg ->
        when (msg.what) {
            RECONNECT_ID -> {
                retryConnect()
            }
        }
        return@Callback true
    })

    companion object {
        private var instance: ServiceHelp? = null
        fun getInstance(): ServiceHelp {
            return instance ?: synchronized(this) {
                instance ?: ServiceHelp().also { instance = it }
            }
        }
    }

    inner class DeathProxy : IBinder.DeathRecipient {
        override fun binderDied() {
            mBinder = null
            retryConnect()
        }
    }

    fun retryConnect() {
        if (null == mBinder) {
            if (mHandler.hasMessages(RECONNECT_ID)) {
                mHandler.removeMessages(RECONNECT_ID)
            }
            bind()
            mHandler.sendEmptyMessageDelayed(RECONNECT_ID, 5000)
        }
    }

    private val mConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            mBinder = null
            retryConnect()
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val deathProxy = DeathProxy()
            service?.linkToDeath(deathProxy, 0);
            mBinder = service as CstService.LocalBinder
        }
    }

    fun bindService() {
        retryConnect()
    }

    fun unBindService() {
        ContextHelp.context.unbindService(mConnection)
    }

    private fun bind() {
        val intent = Intent(ContextHelp.context, CstService::class.java)
        ContextHelp.context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
    }

    fun switchFloatingViewState(b: Boolean) {
        if (null == mBinder) {
            LogUtil.e(TAG, "service not connect")
            return
        }
        mBinder!!.switchFloatingViewState(b)
    }

    fun switchInterceptAd(b: Boolean) {
        if (null == mBinder) {
            LogUtil.e(TAG, "service not connect")
            return
        }
        mBinder!!.switchInterceptAd(b)
        if (b) {
            ToastUtil.show(text = "已开启开屏广告拦截功能")
        } else {
            ToastUtil.show(text = "已关闭开屏广告拦截功能")
        }
    }
}