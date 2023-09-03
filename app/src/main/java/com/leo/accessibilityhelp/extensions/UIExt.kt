package com.leo.accessibilityhelp.extensions

import android.annotation.SuppressLint
import android.widget.CompoundButton
import com.leo.system.log.ZLog

@SuppressLint("DiscouragedPrivateApi")
fun CompoundButton.setCheckedNoEvent(isCheck: Boolean) {
    try {
        ZLog.d("CompoundButton", "setCheckedNoEvent: $isCheck")
        val clazz = CompoundButton::class.java
        val mBroadcasting = clazz.getDeclaredField("mBroadcasting")
        mBroadcasting.isAccessible = true
        mBroadcasting.setBoolean(this, true)
        isChecked = isCheck
        mBroadcasting.setBoolean(this, false)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}