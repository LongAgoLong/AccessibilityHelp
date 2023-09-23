package com.leo.accessibilityhelp.vts.dispatch

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

interface VtsBaseDispatch {
    fun onVtsEventDispatch(event: AccessibilityEvent, source: AccessibilityNodeInfo?)
}