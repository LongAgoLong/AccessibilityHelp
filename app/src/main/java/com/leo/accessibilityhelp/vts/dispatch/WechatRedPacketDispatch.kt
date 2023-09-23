package com.leo.accessibilityhelp.vts.dispatch

import android.app.Notification
import android.app.PendingIntent
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.leo.accessibilityhelplib.AccessibilityHelp
import com.leo.accessibilityhelplib.util.VtsTouchUtils
import com.leo.commonutil.storage.SPHelp
import com.leo.system.context.ContextHelper
import com.leo.system.log.ZLog

class WechatRedPacketDispatch(private val allAppInfoMap: Map<String, List<String>>) :
    VtsBaseDispatch {

    @Volatile
    var isAutoOpenWechatRedPacket: Boolean = true
        set(value) {
            SPHelp.getInstance().put(key = KEY_AUTO_OPEN_WECHAT_RED_PACKET, o = value)
            field = value
        }

    companion object {
        private const val KEY_AUTO_OPEN_WECHAT_RED_PACKET = "KEY_AUTO_OPEN_WECHAT_RED_PACKET"
        private const val TAG = "WechatRedPacketDispatch"
    }

    init {
        val settingsOn =
            AccessibilityHelp.getInstance().isAccessibilitySettingsOn(ContextHelper.context)
        isAutoOpenWechatRedPacket = if (settingsOn) {
            SPHelp.getInstance().getBoolean(
                ContextHelper.context,
                KEY_AUTO_OPEN_WECHAT_RED_PACKET, false
            )
        } else {
            false
        }
    }

    override fun onVtsEventDispatch(event: AccessibilityEvent, source: AccessibilityNodeInfo?) {
        if (!isAutoOpenWechatRedPacket) {
            return
        }
        if (event.eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            handleNotification(event)
        } else if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            || event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) {
            val className = event.className.toString()
//            ZLog.w(TAG, "测试无障碍id = $className")
            when (className) {
                "com.tencent.mm.ui.LauncherUI" -> {
                    getPacket()
                }

                "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI" -> {
                    openPacket()
                }

                "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyNotHookReceiveUI" -> {
                    val service = AccessibilityHelp.getInstance().mService ?: return
                    VtsTouchUtils.handleBackEvent(service)
                }

                "com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI" -> {
                    close()
                }
            }
        }
    }

    /**
     * 处理通知栏信息
     *
     * 如果是微信红包的提示信息,则模拟点击
     *
     * @param event
     */
    private fun handleNotification(event: AccessibilityEvent) {
        val texts = event.text
        if (texts.isNotEmpty()) {
            for (text in texts) {
                val content = text.toString()
                // 如果微信红包的提示信息,则模拟点击进入相应的聊天窗口
                if (content.contains("[微信红包]")) {
                    if (event.parcelableData != null && event.parcelableData is Notification) {
                        val notification: Notification? = event.parcelableData as Notification?
                        val pendingIntent: PendingIntent = notification!!.contentIntent
                        try {
                            pendingIntent.send()
                        } catch (e: PendingIntent.CanceledException) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }

    /**
     * 关闭红包详情界面,实现自动返回聊天窗口
     */
    private fun close() {
        val service = AccessibilityHelp.getInstance().mService ?: return
        val nodeInfo = service.rootInActiveWindow
        if (nodeInfo != null) {
            val list = nodeInfo.findAccessibilityNodeInfosByText("的红包")
            if (list.isNotEmpty()) {
                val info = list[0]
                val rect = Rect()
                info.getBoundsInScreen(rect)
                VtsTouchUtils.clickScreen(rect.centerX(), rect.centerY())
                /*val parent = list[0].parent.parent.parent
                if (parent != null) {
                    for (j in 0 until parent.childCount) {
                        val child = parent.getChild(j)
                        if (child != null && child.isClickable) {
                            child.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        }
                    }
                }*/
            }
        }
    }

    /**
     * 模拟点击,拆开红包
     */
    private fun openPacket() {
        val service = AccessibilityHelp.getInstance().mService ?: return
        val nodeInfo = service.rootInActiveWindow
        if (nodeInfo != null) {
            val list = nodeInfo.findAccessibilityNodeInfosByText("的红包")
            for (i in 0 until list.size) {
                val info = list[i]
                val rect = Rect()
                info.getBoundsInScreen(rect)
                VtsTouchUtils.clickScreen(rect.centerX(), rect.centerY())
                /*val parent = list[i].parent
                if (parent != null) {
                    for (j in 0 until parent.childCount) {
                        val child = parent.getChild(j)
                        if (child != null && child.isClickable) {
                            child.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        }
                    }
                }*/
            }
        }
    }

    /**
     * 模拟点击,打开抢红包界面
     */
    private fun getPacket() {
        val service = AccessibilityHelp.getInstance().mService ?: return
        val rootNode = service.rootInActiveWindow
        val caches: ArrayList<Any> = ArrayList()
        recycle(rootNode, caches)
        if (caches.isNotEmpty()) {
            for (index in 0 until caches.size) {
                // 是否有效的红包
                val isValidPacket = index == 0 || caches[index - 1] !is String
                if (caches[index] is AccessibilityNodeInfo && isValidPacket) {
                    val node = caches[index] as AccessibilityNodeInfo
                    val rect = Rect()
                    node.getBoundsInScreen(rect)
                    VtsTouchUtils.clickScreen(rect.centerX(), rect.centerY())
                }
            }
        }
    }

    /**
     * 递归查找当前聊天窗口中的红包信息
     *
     * 聊天窗口中的红包都存在"领取红包"一词,因此可根据该词查找红包
     *
     * @param node
     */
    private fun recycle(node: AccessibilityNodeInfo, caches: ArrayList<Any>) {
        if (node.childCount == 0) {
            if (node.text != null) {
                val text = node.text.toString()
                if ("已过期" == text
                    || "已被领完" == text
                    || "已领取" == text
                ) {
                    caches.add("#")
                }
                if ("微信红包" == text) {
                    caches.add(node)
                }
            }
        } else {
            for (i in 0 until node.childCount) {
                if (node.getChild(i) != null) {
                    recycle(node.getChild(i), caches)
                }
            }
        }
    }
}