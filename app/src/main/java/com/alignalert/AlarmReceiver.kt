package com.alignalert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val store = TrackerStore(context)
        when (intent.action) {
            ReminderScheduler.ACTION_EAT -> if (store.aligner().isWearing) {
                ReminderScheduler.notify(context, 101, "Snack break unlocked ✨", "You’ve worn your aligners for 4 continuous hours. You can take them out when you’re ready.")
            }
            ReminderScheduler.ACTION_RETURN -> if (!store.aligner().isWearing) {
                ReminderScheduler.notify(context, 102, "Time to pop them back in", "Your aligners have been out for an hour. A quick rinse and you’re back on track.")
            }
            ReminderScheduler.ACTION_TRAY -> {
                val state = store.aligner()
                if (System.currentTimeMillis() >= state.trayStartedAt + 10L * 24 * 60 * 60 * 1000) {
                    ReminderScheduler.notify(context, 103, "New tray day! 🎉", "Tray ${state.tray} has reached 10 days. When you’re ready, switch to tray ${state.tray + 1}.")
                }
            }
            ReminderScheduler.ACTION_MEDICINE -> {
                val id = intent.getLongExtra(ReminderScheduler.EXTRA_ID, 0)
                val minutes = intent.getIntExtra(ReminderScheduler.EXTRA_MINUTES, 0)
                store.medications().firstOrNull { it.id == id }?.let { med ->
                    ReminderScheduler.notify(context, (id % Int.MAX_VALUE).toInt(), "Medication time", "It’s time to take ${med.name}${if (med.dose.isBlank()) "" else " · ${med.dose}"}.")
                    ReminderScheduler.scheduleMedication(context, id, minutes)
                }
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = ReminderScheduler.rescheduleAll(context)
}
