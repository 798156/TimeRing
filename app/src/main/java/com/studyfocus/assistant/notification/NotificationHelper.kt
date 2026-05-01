package com.studyfocus.assistant.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast

object NotificationHelper {

    const val CHANNEL_ID = "todo_deadline_channel"
    const val CHANNEL_NAME = "待办提醒"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "待办事项截止时间提醒"
                enableVibration(true)
            }
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    fun scheduleDeadlineAlarm(context: Context, todoId: Long, title: String, deadlineMillis: Long) {
        val alarmTime = deadlineMillis - 30 * 60 * 1000L
        if (alarmTime <= System.currentTimeMillis()) return
        try {
            val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("todo_title", title)
                putExtra("todo_id", todoId)
                putExtra("is_warning", true)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, todoId.toInt(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmMgr.setExact(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent)
        } catch (e: SecurityException) {
            Toast.makeText(context, "请在设置中授予「闹钟和提醒」权限以接收提醒", Toast.LENGTH_LONG).show()
        } catch (_: Exception) {}
    }

    fun cancelAlarm(context: Context, todoId: Long) {
        try {
            val alarmMgr = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context, todoId.toInt(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmMgr.cancel(pendingIntent)
        } catch (_: Exception) {}
    }
}
