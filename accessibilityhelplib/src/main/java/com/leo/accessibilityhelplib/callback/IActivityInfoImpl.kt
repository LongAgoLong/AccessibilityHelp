package com.leo.accessibilityhelplib.callback

import android.view.accessibility.AccessibilityEvent

interface IActivityInfoImpl {
    fun onAccessibilityEvent(event: AccessibilityEvent)
}