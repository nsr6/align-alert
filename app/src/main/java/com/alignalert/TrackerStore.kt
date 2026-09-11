package com.alignalert

import android.content.Context
import android.util.Base64
import java.time.LocalDate

class TrackerStore(context: Context) {
    private val prefs = context.getSharedPreferences("align_alert", Context.MODE_PRIVATE)
    private fun enc(value: String) = Base64.encodeToString(value.toByteArray(), Base64.NO_WRAP)
    private fun dec(value: String) = runCatching { String(Base64.decode(value, Base64.NO_WRAP)) }.getOrDefault("")

    fun aligner(): AlignerState = AlignerState(
        tray = prefs.getInt("tray", 1),
        trayStartedAt = prefs.getLong("tray_started", System.currentTimeMillis()),
        isWearing = prefs.getBoolean("wearing", true),
        wornSince = prefs.getLong("worn_since", System.currentTimeMillis()),
        removedAt = if (prefs.contains("removed_at")) prefs.getLong("removed_at", 0) else null
    )

    fun saveAligner(state: AlignerState) {
        prefs.edit()
            .putInt("tray", state.tray)
            .putLong("tray_started", state.trayStartedAt)
            .putBoolean("wearing", state.isWearing)
            .putLong("worn_since", state.wornSince)
            .apply {
                if (state.removedAt == null) remove("removed_at") else putLong("removed_at", state.removedAt)
            }.apply()
    }

    fun medications(): List<Medication> = prefs.getString("medications", "")
        .orEmpty().split(";").filter { it.isNotBlank() }.mapNotNull { row ->
            val parts = row.split("|")
            if (parts.size < 5) null else runCatching {
                Medication(
                    id = parts[0].toLong(), name = dec(parts[1]), dose = dec(parts[2]),
                    minutes = parts[3].split(",").filter { it.isNotBlank() }.map { it.toInt() },
                    takenOn = parts[4].split(",").filter { it.isNotBlank() }.toSet()
                )
            }.getOrNull()
        }

    fun saveMedication(medication: Medication) {
        val all = medications().filterNot { it.id == medication.id } + medication
        saveMedications(all)
    }

    fun markTaken(id: Long, day: LocalDate = LocalDate.now()) {
        val all = medications().map { med ->
            if (med.id == id) med.copy(takenOn = med.takenOn + day.toString()) else med
        }
        saveMedications(all)
    }

    private fun saveMedications(list: List<Medication>) {
        val serial = list.joinToString(";") { med ->
            listOf(med.id, enc(med.name), enc(med.dose), med.minutes.joinToString(","), med.takenOn.takeLast(45).joinToString(",")).joinToString("|")
        }
        prefs.edit().putString("medications", serial).apply()
    }

    fun addLog(text: String, at: Long = System.currentTimeMillis()) {
        val old = logs().take(49)
        val serial = (listOf(LogEntry(at, text)) + old).joinToString(";") { "${it.at}|${enc(it.text)}" }
        prefs.edit().putString("logs", serial).apply()
    }

    fun logs(): List<LogEntry> = prefs.getString("logs", "").orEmpty().split(";").mapNotNull {
        val p = it.split("|")
        if (p.size != 2) null else p[0].toLongOrNull()?.let { at -> LogEntry(at, dec(p[1])) }
    }
}
