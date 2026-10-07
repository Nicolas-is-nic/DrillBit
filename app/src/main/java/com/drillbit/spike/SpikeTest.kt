package com.drillbit.spike

import android.app.Activity
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.drillbit.MainActivity

/*
 * S0 真机验证代码（临时）：验证卓易通容器下两项系统能力，F3/F4 定案后删除或转正。
 * - 定时通知：AlarmManager 精确闹钟 -> 杀进程后 5 分钟触发系统通知，点击拉起 App（验证 F3 可行性）
 * - 钉屏：SettingsScreen 里直接调 Activity.startLockTask（验证 F4-L2 可行性）
 */

/** 预约 5 分钟后的本地通知（USE_EXACT_ALARM 已声明，API 31+ 未授权时降级为非精确闹钟） */
object SpikeTest {

    private const val NOTIFY_ID = 2001
    private const val CHANNEL_ID = "spike-verify"

    fun scheduleNotification(context: Context, delayMillis: Long = 5 * 60 * 1000) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(
            context,
            1001,
            Intent(context, SpikeAlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val at = System.currentTimeMillis() + delayMillis
        val exactAllowed = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()
        if (exactAllowed) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            // 容器未授予精确闹钟时的降级路径（触发时间可能有几分钟漂移）
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    /** 立即发一条通知（接收器收到闹钟广播时调用） */
    fun postNotification(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "验证通知", NotificationManager.IMPORTANCE_HIGH),
        )
        val tap = PendingIntent.getActivity(
            context,
            1002,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("DrillBit 验证通知")
            .setContentText("杀进程后定时通知触发成功，通知能力可用；点击本条应回到 App")
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIFY_ID, notification)
    }

    /** 钉屏开关：未钉时请求钉屏，已钉时解除（startLockTask 系统弹确认框，退出兜底为长按返回+多任务） */
    fun togglePin(activity: Activity, pinned: Boolean): Boolean {
        return try {
            if (pinned) {
                activity.stopLockTask()
            } else {
                activity.startLockTask()
            }
            !pinned
        } catch (e: Exception) {
            false
        }
    }
}

/** 闹钟到点广播：发通知（App 可能已被杀，靠系统拉起本接收器进程） */
class SpikeAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        SpikeTest.postNotification(context)
    }
}
