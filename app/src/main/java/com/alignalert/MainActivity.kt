package com.alignalert

import android.Manifest
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalPharmacy
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreTime
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay

private val Ink = Color(0xFF29243B)
private val Violet = Color(0xFF7057D5)
private val SoftViolet = Color(0xFFECE7FF)
private val Mint = Color(0xFFBFEFE1)
private val Peach = Color(0xFFFFDEC4)
private val Canvas = Color(0xFFFFFAFE)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReminderScheduler.rescheduleAll(this)
        setContent {
            AlignAlertTheme {
                val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
                App(
                    askNotifications = {
                        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                    askExactAlarms = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) {
                            startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AlignAlertTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.lightColorScheme(
            primary = Violet, onPrimary = Color.White, secondary = Color(0xFF277B6A),
            background = Canvas, surface = Color.White, onSurface = Ink
        ), content = content
    )
}

@Composable
private fun App(askNotifications: () -> Unit, askExactAlarms: () -> Unit) {
    val context = LocalContext.current
    val store = remember { TrackerStore(context) }
    var state by remember { mutableStateOf(store.aligner()) }
    var meds by remember { mutableStateOf(store.medications()) }
    var logs by remember { mutableStateOf(store.logs()) }
    var tab by remember { mutableIntStateOf(0) }
    var showAddMedication by remember { mutableStateOf(false) }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    fun refresh() { state = store.aligner(); meds = store.medications(); logs = store.logs(); now = System.currentTimeMillis() }
    fun saveAligner(next: AlignerState, log: String) {
        store.saveAligner(next); store.addLog(log); ReminderScheduler.rescheduleAll(context); refresh()
    }
    fun markMedicationTaken(id: Long) {
        store.markTaken(id)
        meds.firstOrNull { it.id == id }?.let { store.addLog("Took ${it.name}") }
        refresh()
    }
    LaunchedEffect(state.isWearing, state.wornSince, state.removedAt) {
        while (true) { now = System.currentTimeMillis(); delay(1_000) }
    }

    Scaffold(
        containerColor = Canvas,
        topBar = { Header(onBell = askNotifications, onSettings = askExactAlarms) },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                listOf("Today" to Icons.Outlined.FavoriteBorder, "Meds" to Icons.Outlined.LocalPharmacy, "History" to Icons.Outlined.History).forEachIndexed { index, item ->
                    NavigationBarItem(selected = tab == index, onClick = { tab = index }, icon = { Icon(item.second, null) }, label = { Text(item.first) })
                }
            }
        },
        floatingActionButton = {
            if (tab == 1) FloatingActionButton(onClick = { showAddMedication = true }, containerColor = Violet, contentColor = Color.White) { Icon(Icons.Outlined.Add, "Add medication") }
        }
    ) { padding ->
        AnimatedContent(tab, label = "screen") { current ->
            when (current) {
                0 -> TodayScreen(state, meds, now, Modifier.padding(padding), onOut = {
                    val elapsed = System.currentTimeMillis() - state.wornSince
                    if (elapsed >= FOUR_HOURS) saveAligner(state.copy(isWearing = false, removedAt = System.currentTimeMillis()), "Took aligners out")
                }, onIn = {
                    saveAligner(state.copy(isWearing = true, wornSince = System.currentTimeMillis(), removedAt = null), "Put aligners in")
                }, onNextTray = {
                    saveAligner(state.copy(tray = state.tray + 1, trayStartedAt = System.currentTimeMillis()), "Started tray ${state.tray + 1}")
                }, onTaken = ::markMedicationTaken)
                1 -> MedicationsScreen(meds, Modifier.padding(padding), onTaken = ::markMedicationTaken, onAdd = { showAddMedication = true })
                else -> HistoryScreen(logs, Modifier.padding(padding))
            }
        }
    }
    if (showAddMedication) MedicationDialog(onDismiss = { showAddMedication = false }, onSave = { name, dose, times ->
        store.saveMedication(Medication(System.currentTimeMillis(), name, dose, times)); ReminderScheduler.rescheduleAll(context); refresh(); showAddMedication = false
    })
}

private const val FOUR_HOURS = 4L * 60 * 60 * 1000
private const val TEN_DAYS = 10L * 24 * 60 * 60 * 1000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Header(onBell: () -> Unit, onSettings: () -> Unit) {
    CenterAlignedTopAppBar(
        title = { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("align alert", fontWeight = FontWeight.Black, letterSpacing = 0.4.sp); Text("small habits, bright smile", fontSize = 11.sp, color = Color.Gray) } },
        navigationIcon = { IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, "Reminder settings") } },
        actions = { IconButton(onClick = onBell) { Icon(Icons.Outlined.NotificationsActive, "Enable notifications") } },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Canvas)
    )
}

@Composable
private fun TodayScreen(state: AlignerState, meds: List<Medication>, now: Long, modifier: Modifier, onOut: () -> Unit, onIn: () -> Unit, onNextTray: () -> Unit, onTaken: (Long) -> Unit) {
    val currentDuration = if (state.isWearing) now - state.wornSince else 0
    val trayProgress = ((now - state.trayStartedAt).toFloat() / TEN_DAYS).coerceIn(0f, 1f)
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text(if (state.isWearing) "Looking good!" else "Enjoy your break", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Ink) }
        item { Text(if (state.isWearing) "Your aligner is doing its quiet work." else "Remember to pop your aligners back in soon.", color = Color(0xFF625D70)) }
        item { AlignerHero(state, currentDuration, onOut, onIn) }
        item { TrayCard(state, trayProgress, onNextTray) }
        item { Text("Today’s medicine", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Ink, modifier = Modifier.padding(top = 4.dp)) }
        if (meds.isEmpty()) item { EmptyMedicationNudge() }
        else items(meds) { med -> MedicationRow(med, onTaken) }
    }
}

@Composable
private fun AlignerHero(state: AlignerState, duration: Long, onOut: () -> Unit, onIn: () -> Unit) {
    val safe = duration >= FOUR_HOURS
    Card(colors = CardDefaults.cardColors(containerColor = if (state.isWearing) SoftViolet else Peach), shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(54.dp).clip(CircleShape).background(Color.White.copy(alpha = .8f)), contentAlignment = Alignment.Center) { Icon(if (state.isWearing) Icons.Outlined.WaterDrop else Icons.Outlined.Restaurant, null, tint = Violet, modifier = Modifier.size(27.dp)) }
                Spacer(Modifier.width(14.dp))
                Column { Text(if (state.isWearing) "Aligners are in" else "Aligners are out", fontWeight = FontWeight.Bold, fontSize = 19.sp); Text(if (state.isWearing) "Continuous wear session" else "Break in progress", color = Color(0xFF625D70), fontSize = 13.sp) }
            }
            Spacer(Modifier.height(18.dp))
            if (state.isWearing) {
                Text(duration.asElapsed(), fontSize = 38.sp, fontWeight = FontWeight.Black, color = Ink)
                Text(if (safe) "Nice work — a food break is unlocked." else "${(FOUR_HOURS - duration).coerceAtLeast(0).asElapsed()} until a food break", color = Color(0xFF625D70))
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(progress = (duration.toFloat() / FOUR_HOURS).coerceIn(0f, 1f), modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = Violet, trackColor = Color.White)
                Spacer(Modifier.height(18.dp))
                Button(onClick = onOut, enabled = safe, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Violet)) { Icon(if (safe) Icons.Outlined.Restaurant else Icons.Outlined.Lock, null); Spacer(Modifier.width(8.dp)); Text(if (safe) "Take aligners out" else "Keep them in for now") }
            } else {
                val outTime = (System.currentTimeMillis() - (state.removedAt ?: System.currentTimeMillis())).coerceAtLeast(0)
                Text("Out for ${outTime.asElapsed()}", fontSize = 30.sp, fontWeight = FontWeight.Black, color = Ink)
                Text(if (outTime >= 60 * 60 * 1000) "Time to get back on track." else "You’ll get a gentle nudge after one hour.", color = Color(0xFF625D70))
                Spacer(Modifier.height(18.dp))
                Button(onClick = onIn, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF257E69))) { Icon(Icons.Outlined.Check, null); Spacer(Modifier.width(8.dp)); Text("Put aligners back in") }
            }
        }
    }
}

@Composable
private fun TrayCard(state: AlignerState, progress: Float, onNextTray: () -> Unit) {
    val days = ((System.currentTimeMillis() - state.trayStartedAt) / (24 * 60 * 60 * 1000)).toInt().coerceAtLeast(0)
    Card(colors = CardDefaults.cardColors(containerColor = Mint), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(.75f)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.CalendarMonth, null, tint = Color(0xFF277B6A)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Tray ${state.tray}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Day ${days.coerceAtMost(10)} of 10", color = Color(0xFF426B60), fontSize = 13.sp)
                Spacer(Modifier.height(7.dp)); LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = Color(0xFF277B6A), trackColor = Color.White)
            }
            if (progress >= 1f) FilledIconButton(onClick = onNextTray, containerColor = Color.White, contentColor = Color(0xFF277B6A)) { Icon(Icons.Outlined.Add, "Start next tray") }
        }
    }
}

@Composable
private fun MedicationRow(med: Medication, onTaken: (Long) -> Unit) {
    val taken = LocalDate.now().toString() in med.takenOn
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFFFEFE4)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.LocalPharmacy, null, tint = Color(0xFFBB653A)) }
            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(med.name, fontWeight = FontWeight.Bold); Text(listOfNotNull(med.dose.takeIf { it.isNotBlank() }, med.minutes.joinToString(" · ") { it.asClock() }).joinToString("  •  "), fontSize = 12.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            if (taken) AssistChip(onClick = {}, label = { Text("Taken") }, leadingIcon = { Icon(Icons.Outlined.Check, null, Modifier.size(16.dp)) }) else OutlinedButton(onClick = { onTaken(med.id) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) { Text("Take") }
        }
    }
}

@Composable
private fun EmptyMedicationNudge() = Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.LocalPharmacy, null, tint = Violet); Spacer(Modifier.width(12.dp)); Text("Add medications to keep every dose in view.", color = Color(0xFF625D70)) } }

@Composable
private fun MedicationsScreen(meds: List<Medication>, modifier: Modifier, onTaken: (Long) -> Unit, onAdd: () -> Unit) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Medication routine", fontWeight = FontWeight.Bold, fontSize = 28.sp); Text("Tiny check-ins for the things that matter.", color = Color.Gray); Spacer(Modifier.height(10.dp)) }
        if (meds.isEmpty()) item { Card(colors = CardDefaults.cardColors(containerColor = SoftViolet), shape = RoundedCornerShape(24.dp)) { Column(Modifier.padding(22.dp)) { Text("Your cabinet is empty", fontWeight = FontWeight.Bold, fontSize = 18.sp); Text("Add a medication and we’ll keep its schedule gently on your radar.", color = Color(0xFF625D70)); Spacer(Modifier.height(12.dp)); Button(onClick = onAdd) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(6.dp)); Text("Add medication") } } }
        else items(meds) { MedicationRow(it, onTaken) }
    }
}

@Composable
private fun HistoryScreen(logs: List<LogEntry>, modifier: Modifier) {
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Your rhythm", fontWeight = FontWeight.Bold, fontSize = 28.sp); Text("A calm little record of your progress.", color = Color.Gray); Spacer(Modifier.height(8.dp)) }
        if (logs.isEmpty()) item { EmptyMedicationNudge() }
        items(logs) { entry -> Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.MoreTime, null, tint = Violet); Spacer(Modifier.width(12.dp)); Column { Text(entry.text, fontWeight = FontWeight.Medium); Text(entry.at.asDateTime(), fontSize = 12.sp, color = Color.Gray) } } } }
    }
}

@Composable
private fun MedicationDialog(onDismiss: () -> Unit, onSave: (String, String, List<Int>) -> Unit) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }
    var times by remember { mutableStateOf(listOf(8 * 60)) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add medication") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Medication name") }, singleLine = true)
            OutlinedTextField(value = dose, onValueChange = { dose = it }, label = { Text("Dose, e.g. 1 tablet") }, singleLine = true)
            Text("Daily reminder times", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            times.forEach { minute -> AssistChip(onClick = {}, label = { Text(minute.asClock()) }, trailingIcon = { IconButton(onClick = { times = times - minute }) { Icon(Icons.Outlined.NightsStay, "Remove time", Modifier.size(15.dp)) } }) }
            OutlinedButton(onClick = {
                val first = times.firstOrNull() ?: 8 * 60
                TimePickerDialog(context, { _, hour, min -> times = (times + hour * 60 + min).distinct().sorted() }, first / 60, first % 60, false).show()
            }) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(6.dp)); Text("Add time") }
        }
    }, confirmButton = { Button(onClick = { onSave(name.trim(), dose.trim(), times) }, enabled = name.isNotBlank() && times.isNotEmpty()) { Text("Save reminders") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

private fun Long.asElapsed(): String { val totalMinutes = (this / 60_000).coerceAtLeast(0); return "%d:%02d".format(Locale.getDefault(), totalMinutes / 60, totalMinutes % 60) }
private fun Int.asClock(): String { val h = this / 60; val m = this % 60; val suffix = if (h < 12) "AM" else "PM"; val shown = when (h % 12) { 0 -> 12; else -> h % 12 }; return "%d:%02d %s".format(Locale.getDefault(), shown, m, suffix) }
private fun Long.asDateTime(): String = Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEE, MMM d · h:mm a"))
