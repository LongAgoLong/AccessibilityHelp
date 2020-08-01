package com.leo.accessibilityhelp.lifecyle

import android.text.TextUtils
import androidx.annotation.NonNull
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.leo.accessibilityhelplib.AccessibilityHelp
import com.leo.commonutil.storage.IOUtil
import com.leo.commonutil.storage.SDcardUtil
import com.leo.system.ResHelp
import kotlin.math.abs

class ServiceObserver(@NonNull onObserverCallback: OnObserverCallback) : LifecycleObserver {
    private val mOnInitCallback = onObserverCallback

    companion object {
        const val AD_IDS = "skipIds.txt"
        const val AD_TEXTS = "skipTexts.txt"
        const val AD_ACT_WHITE = "activityBlackList.txt"
        const val PACKAGES = "pkgBlackList.txt"
        const val VERSION_FILE = "version.txt"
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    private fun init() {
        if (checkVersion()) {
            IOUtil.delAllFile(SDcardUtil.fileFolder!!.absolutePath)
            val adTexts = ResHelp.getFileFromAssets(AD_TEXTS)
            adTexts?.let {
                IOUtil.writeDiskText(
                    fileName = AD_TEXTS,
                    content = it,
                    base64Encode = false
                )
            }
            val ids = ResHelp.getFileFromAssets(AD_IDS)
            ids?.let {
                IOUtil.writeDiskText(
                    fileName = AD_IDS,
                    content = it,
                    base64Encode = false
                )
            }

            val actBlackStr = ResHelp.getFileFromAssets(AD_ACT_WHITE)
            actBlackStr?.let {
                IOUtil.writeDiskText(
                    fileName = AD_ACT_WHITE,
                    content = it,
                    base64Encode = false
                )
            }

            val pkgBlackSts = ResHelp.getFileFromAssets(PACKAGES)
            pkgBlackSts?.let {
                IOUtil.writeDiskText(
                    fileName = PACKAGES,
                    content = it,
                    base64Encode = false
                )
            }
            val fileFromAssets = ResHelp.getFileFromAssets(VERSION_FILE)
            fileFromAssets?.let {
                IOUtil.writeDiskText(
                    fileName = VERSION_FILE,
                    content = it,
                    base64Encode = false
                )
            }
            mOnInitCallback.onInitSuccess()
        }
    }

    private fun checkVersion(): Boolean {
        val diskVersionText =
            IOUtil.getDiskText(fileName = VERSION_FILE)
        val assetsVersion = ResHelp.getFileFromAssets(VERSION_FILE)!!.toInt()
        if (TextUtils.isEmpty(diskVersionText)) {
            return true
        } else {
            try {
                val toInt = diskVersionText!!.toInt()
                if (abs(toInt - assetsVersion) != 0) {
                    return true
                }
            } catch (e: NumberFormatException) {
                return true
            }
        }
        return false
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    private fun onDestroy() {
        AccessibilityHelp.getInstance().mIActivityInfoImpl = null
    }

    interface OnObserverCallback {
        fun onInitSuccess()
    }
}