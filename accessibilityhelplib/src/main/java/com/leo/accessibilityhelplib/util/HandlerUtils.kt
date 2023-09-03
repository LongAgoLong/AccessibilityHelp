package com.leo.accessibilityhelplib.util

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper

object HandlerUtils {
    private val mUiHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var mVtsHandler: Handler? = null

    /**
     * 主线程
     */
    fun getMainHandler(): Handler {
        return mUiHandler
    }

    /**
     * 无障碍服务线程
     */
    fun getVtsHandler(): Handler {
        return mVtsHandler ?: synchronized(this) {
            mVtsHandler ?: createVtsHandler().also { mVtsHandler = it }
        }
    }

    private fun createVtsHandler(): Handler {
        val thread = HandlerThread("vts-thread").also { it.start() }
        return Handler(thread.looper)
    }
}