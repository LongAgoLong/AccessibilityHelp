package com.leo.accessibilityhelp.util

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import com.leo.accessibilityhelp.service.CstService
import com.leo.commonutil.notify.ToastUtil
import com.leo.system.LogUtil
import com.leo.system.context.ContextHelp
import java.lang.ref.WeakReference

class ServiceHelp private constructor() {
    private var mBinder: CstService.LocalBinder? = null
    private val deathProxy = DeathProxy()
    private var mHandler: ServiceHandler

    init {
        mHandler = ServiceHandler(WeakReference(this@ServiceHelp))
    }

    companion object {
        private const val TAG = "ServiceHelp"
        private const val RECONNECT_ID = 1001

        private var instance: ServiceHelp? = null
        fun getInstance(): ServiceHelp {
            return instance ?: synchronized(this) {
                instance ?: ServiceHelp().also { instance = it }
            }
        }
    }

    inner class DeathProxy : IBinder.DeathRecipient {
        override fun binderDied() {
            LogUtil.e(TAG, "binderDied()")
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
            LogUtil.e(TAG, "onServiceDisconnected()")
            mBinder?.unlinkToDeath(deathProxy, 0)
            mBinder = null
            retryConnect()
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            LogUtil.i(TAG, "onServiceConnected()")
            service?.linkToDeath(deathProxy, 0);
            mBinder = service as CstService.LocalBinder
        }
    }

    fun bindService() {
        retryConnect()
    }

    fun unBindService() {
        if (null != mBinder) {
            synchronized(this) {
                if (null == mBinder) {
                    return@synchronized
                }
                mBinder?.run {
                    getService().removeInfoView()
                    getService().toggleInterceptAd(false)
                    unlinkToDeath(deathProxy, 0)
                    ContextHelp.context.unbindService(mConnection)
                }
                mBinder = null
            }
        }
        ToastUtil.show(text = "服务连接已断开")
    }

    fun stopService() {
        val intent = Intent(ContextHelp.context, CstService::class.java)
        ContextHelp.context.stopService(intent)
    }

    private fun bind() {
        val intent = Intent(ContextHelp.context, CstService::class.java)
        ContextHelp.context.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
    }

    fun switchFloatingViewState(b: Boolean) {
        mBinder ?: LogUtil.e(TAG, "service not connect")
        if (b) mBinder?.getService()?.addInfoView() else mBinder?.getService()?.removeInfoView()
    }

    fun switchInterceptAd(b: Boolean) {
        mBinder?.run {
            getService().toggleInterceptAd(b)
            ToastUtil.show(text = "${if (b) "已开启" else "已关闭"}开屏广告拦截功能")
        }
    }

    fun reloadFromSd() {
        mBinder?.run {
            getService().loadConfigFromSdcard()
            ToastUtil.show(text = "已为你重新加载拦截配置")
        }
    }

    @SuppressLint("HandlerLeak")
    inner class ServiceHandler : Handler {
        private var weakReference: WeakReference<ServiceHelp>

        constructor(weakReference: WeakReference<ServiceHelp>) : super(Looper.getMainLooper()) {
            this.weakReference = weakReference
        }

        override fun handleMessage(msg: Message) {
            weakReference ?: return
            val help = weakReference.get()
            help ?: return
            if (msg.what == RECONNECT_ID) {
                help.retryConnect()
            }
        }
    }
}