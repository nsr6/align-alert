package com.alignalert

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object ReminderScheduler {
    const val ACTION_EAT = "com.alignalert.EAT"
    const val ACTION_RETURN = "com.alignalert.RETURN"
    const val ACTION_TRAY = "com.alignalert.TRAY"
    const val ACTION_MEDICINE = "com.alignalert.MEDICINE"
    const val EXTRA_ID = "medication_id"
    const val EXTRA_MINUTES = "minutes"
    private const val CHANNEL = "gentle_reminders"
    private const val EAT_CODE = 101
    private const val RETURN_CODE = 102
    private const val TRAY_CODE = 103

    fun setupChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Gentle reminders", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Aligner and medication reminders"
        })
    }

    fun rescheduleAll(context: Context) {
        setupChannel(context)
        val store = TrackerStore(context)
        val state = store.aligner()
        scheduleTray(context, state)
        if (state.isWearing) scheduleEatAllowed(context, state.wornSince) else state.removedAt?.let { scheduleReturn(context, it) }
        store.medications().forEach { med -> med.minutes.forEach { scheduleMedication(context, med.id, it) } }
    }

    fun scheduleEatAllowed(context: Context, wornSince: Long) {
        val fourHours = wornSince + 4 * 60 * 60 * 1000L
        schedule(context, ACTION_EAT, EAT_CODE, nextDaytime(fourHours))
    }

    fun scheduleReturn(context: Context, removedAt: Long) =
        schedule(context, ACTION_RETURN, RETURN_CODE, removedAt + 60 * 60 * 1000L)

    fun scheduleTray(context: Context, state: AlignerState) =
        schedule(context, ACTION_TRAY, TRAY_CODE, state.trayStartedAt + 10L * 24 * 60 * 60 * 1000)

    fun scheduleMedication(context: Context, id: Long, minutes: Int) {
        val now = LocalDateTime.now()
        var at = now.toLocalDate().atStartOfDay().plusMinutes(minutes.toLong())
        if (!at.isAfter(now)) at = at.plusDays(1)
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val code = 300_000 + ((id % 10_000) * 1_500).toInt() + minutes
        schedule(context, ACTION_MEDICINE, code, millis, id, minutes)
    }

    private fun nextDaytime(after: Long): Long {
        val zone = ZoneId.systemDefault()
        val dateTime = java.time.Instant.ofEpochMilli(after).atZone(zone).toLocalDateTime()
        val start = LocalTime.of(8, 0)
        val end = LocalTime.of(21, 0)
        val adjusted = when {
            dateTime.toLocalTime().isBefore(start) -> dateTime.toLocalDate().atTime(start)
            !dateTime.toLocalTime().isAfter(end) -> dateTime
            else -> dateTime.toLocalDate().plusDays(1).atTime(start)
        }
        return adjusted.atZone(zone).toInstant().toEpochMilli()
    }

    private fun schedule(context: Context, action: String, code: Int, at: Long, id: Long = 0, minutes: Int = 0) {
        val intent = Intent(context, AlarmReceiver::class.java).setAction(action)
            .putExtra(EXTRA_ID, id).putExtra(EXTRA_MINUTES, minutes)
        val pending = PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val alarms = context.getSystemService(AlarmManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending)
        }
    }

    fun notify(context: Context, id: Int, title: String, body: String) {
        setupChannel(context)
        val launch = PendingIntent.getActivity(context, 900 + id, Intent(context, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        NotificationManagerCompatHolder.from(context).notify(id, NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder).setContentTitle(title).setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body)).setAutoCancel(true).setContentIntent(launch).setPriority(NotificationCompat.PRIORITY_HIGH).build())
    }
}

private object NotificationManagerCompatHolder {
    fun from(context: Context) = androidx.core.app.NotificationManagerCompat.from(context)
}
