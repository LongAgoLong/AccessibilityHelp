package com.leo.accessibilityhelp.vts.notification

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.leo.accessibilityhelp.vts.VtsManager
import com.leo.system.log.ZLog

class VtsNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (!VtsManager.getInstance().isAutoOpenWechatRedPacket()) {
            return
        }
        val extras = sbn?.notification?.extras
        // 获取接收消息APP的包名
        val notificationPkg = sbn?.packageName
        // 获取接收消息的抬头
        val notificationTitle = extras?.getString(Notification.EXTRA_TITLE)
        // 获取接收消息的内容
        val notificationText = extras?.getString(Notification.EXTRA_TEXT)
        notificationPkg ?: return
        if (notificationPkg.isNotBlank()) {
            ZLog.d("收到的消息内容包名：", notificationPkg)
            if ("com.tencent.mm" == notificationPkg
                && notificationText?.contains("[微信红包]") == true
            ) {
                // 收到微信红包了
                val intent = sbn.notification.contentIntent
                intent.send()
            }
        }
        ZLog.d("收到的消息内容", "Notification posted $notificationTitle &amp; $notificationText")
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
    }
}