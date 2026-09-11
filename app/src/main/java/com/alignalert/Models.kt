package com.alignalert

data class AlignerState(
    val tray: Int = 1,
    val trayStartedAt: Long = System.currentTimeMillis(),
    val isWearing: Boolean = true,
    val wornSince: Long = System.currentTimeMillis(),
    val removedAt: Long? = null
)

data class Medication(
    val id: Long,
    val name: String,
    val dose: String,
    val minutes: List<Int>,
    val takenOn: Set<String> = emptySet()
)

data class LogEntry(val at: Long, val text: String)
