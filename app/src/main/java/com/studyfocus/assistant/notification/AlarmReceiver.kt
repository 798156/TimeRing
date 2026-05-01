package com.studyfocus.assistant.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.studyfocus.assistant.MainActivity
import com.studyfocus.assistant.R

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("todo_title") ?: "待办事项"
        val todoId = intent.getLongExtra("todo_id", 0)
        val isWarning = intent.getBooleanExtra("is_warning", false)

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("nav_to_todo", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, todoId.toInt(), tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentTitle = if (isWarning) "⏰ 截止时间临近" else "⏰ 截止时间到了"
        val contentText = if (isWarning) "「$title」还有30分钟！" else "「$title」该完成了！"
        val bigText = if (isWarning) "「$title」\n还有30分钟就要截止了，抓紧完成吧！"
        else "「$title」\n截止时间已到，快去完成吧！"

        val notification = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_todo)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(todoId.toInt(), notification)
    }
}
