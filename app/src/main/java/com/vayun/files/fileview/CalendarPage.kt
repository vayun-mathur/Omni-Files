package com.vayun.files.fileview

import android.Manifest
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.vayun.files.R
import com.vayun.files.ShareFAB
import com.vayun.files.parser.DateTimeValue
import com.vayun.files.parser.DateValue
import com.vayun.files.parser.CalendarParser
import com.vayun.files.parser.Temporal
import com.vayun.files.parser.VCalendar
import com.vayun.files.parser.VEvent
import com.vayun.files.showInputPopup
import java.io.File
import java.time.Duration
import java.time.LocalDateTime
import java.util.Locale
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import android.provider.CalendarContract.Events
import android.provider.CalendarContract.Calendars
import java.time.format.DateTimeFormatter
import java.time.Duration as JavaDuration
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarPage(navController: NavController, path: String) {
    val currentFile = File(path)
    val context = LocalContext.current
    val contentResolver = LocalContext.current.contentResolver
    val calendar by remember { mutableStateOf(CalendarParser.parse(contentResolver.getText(path)))}

    val showImportPopup = showInputPopup("Import Events", "New Calendar Name", default=calendar.name) {
        createOfflineCalendarFromVCalendar(navController.context, calendar, it)
    }

    val requestCalendarPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) {
            showImportPopup()
        } else {
            Toast.makeText(context, "Calendar permission denied", Toast.LENGTH_SHORT).show()
        }
    }


    Scaffold(
        topBar = { TopAppBar({Text(currentFile.name)}) },
        floatingActionButton = {
            Column {
                ShareFAB(path)
                Spacer(Modifier.height(6.dp))
                FloatingActionButton({
                    requestCalendarPermission.launch(Manifest.permission.WRITE_CALENDAR)
                }) {
                    Icon(painterResource(R.drawable.outline_download_24), null)
                }
            }
        }
    ) { innerPadding ->
        Column(Modifier.padding(innerPadding)) {
            LazyColumn {
                items(calendar.events) { event ->
                    VEventCard(event)
                }
            }
        }
    }
}

@Composable
fun VEventCard(event: VEvent) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Event title
            Text(
                text = event.summary ?: "No title",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Replace previous start/end text with:
            Text(
                text = "🕒 ${formatTemporalRangeReadable(event.dtStart, event.dtEnd)}",
                style = MaterialTheme.typography.bodyMedium
            )

            // Recurrence rule
            event.recurrenceRule?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "🔁 ${formatRRuleReadable(it)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Location
            event.location?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "📍 $it",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Status
            event.status?.let {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Status: ${it.name}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // Attendees
            if (event.attendees.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "👥 Attendees:",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                event.attendees.forEach { attendee ->
                    Text(
                        text = "- $attendee",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Description
            event.description?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

fun formatTemporalRangeReadable(start: Temporal?, end: Temporal?): String {
    if (start == null) return "Unknown start"
    val formatterDate = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())
    val formatterTime = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

    return when {
        start is DateTimeValue && end is DateTimeValue -> {
            val startDate = start.dateTime.toLocalDate()
            val endDate = end.dateTime.toLocalDate()
            val startTime = start.dateTime.toLocalTime()
            val endTime = end.dateTime.toLocalTime()

            if (startDate == endDate) {
                "${start.dateTime.format(formatterDate)}, ${startTime.format(formatterTime)} - ${endTime.format(formatterTime)}"
            } else {
                "${start.dateTime.format(DateTimeFormatter.ofPattern("EEE, MMM d h:mm a"))} - ${end.dateTime.format(DateTimeFormatter.ofPattern("EEE, MMM d h:mm a"))}"
            }
        }
        start is DateTimeValue -> start.dateTime.format(DateTimeFormatter.ofPattern("EEE, MMM d h:mm a"))
        start is DateValue -> start.date.toString()
        else -> "Unknown time"
    }
}


fun formatRRuleReadable(rrule: String): String {
    // Example: "FREQ=WEEKLY;COUNT=10;BYDAY=MO,WE,FR"
    val parts = rrule.split(";").associate {
        val (k,v) = it.split("=")
        k.uppercase() to v.uppercase()
    }

    val freq = when(parts["FREQ"]) {
        "DAILY" -> "Daily"
        "WEEKLY" -> "Weekly"
        "MONTHLY" -> "Monthly"
        "YEARLY" -> "Yearly"
        else -> "Repeats"
    }

    val byDay = parts["BYDAY"]?.split(",")?.joinToString(", ") { dayCode ->
        when(dayCode) {
            "MO" -> "Mon"
            "TU" -> "Tue"
            "WE" -> "Wed"
            "TH" -> "Thu"
            "FR" -> "Fri"
            "SA" -> "Sat"
            "SU" -> "Sun"
            else -> dayCode
        }
    }

    val count = parts["COUNT"]?.let { " ($it times)" } ?: ""
    val until = parts["UNTIL"]?.let {
        val dt = try { LocalDateTime.parse(it, DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")) } catch(e: Exception) { null }
        dt?.let { d -> " until ${d.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))}" } ?: ""
    } ?: ""

    return buildString {
        append(freq)
        if (!byDay.isNullOrEmpty()) append(" on $byDay")
        append(count)
        append(until)
    }
}

fun createOfflineCalendarFromVCalendar(
    context: Context,
    vCalendar: VCalendar,
    calendarName: String
) {
    val cr = context.contentResolver

    // Step 1: Create a new local calendar
    val accountName = "Offline Calendar"
    val calValues = ContentValues().apply {
        put(Calendars.ACCOUNT_NAME, accountName)
        put(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
        put(Calendars.NAME, calendarName)
        put(Calendars.CALENDAR_DISPLAY_NAME, calendarName)
        put(Calendars.CALENDAR_COLOR, 0xFF2196F3.toInt()) // Blue
        put(Calendars.CALENDAR_ACCESS_LEVEL, Calendars.CAL_ACCESS_OWNER)
        put(Calendars.OWNER_ACCOUNT, accountName)
        put(Calendars.VISIBLE, 1)
        put(Calendars.SYNC_EVENTS, 1)
        put(Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
    }

    val calUri = Calendars.CONTENT_URI
        .buildUpon()
        .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
        .appendQueryParameter(Calendars.ACCOUNT_NAME, accountName)
        .appendQueryParameter(Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
        .build()

    val calendarId = cr.insert(calUri, calValues)?.let { ContentUris.parseId(it) }
        ?: throw Exception("Failed to create calendar")

    // Step 2: Insert all VEVENTs
    vCalendar.events.forEach { vEvent ->
        val startZdt = (vEvent.dtStart as? DateTimeValue)?.dateTime
        val endZdt = (vEvent.dtEnd as? DateTimeValue)?.dateTime
            ?: (vEvent.duration?.let { dur -> startZdt?.plus(parseICalDuration(dur.raw)) })

        if (startZdt != null && endZdt != null) {
            val eventValues = ContentValues().apply {
                put(Events.CALENDAR_ID, calendarId)
                put(Events.TITLE, vEvent.summary)
                put(Events.DESCRIPTION, vEvent.description)
                put(Events.EVENT_LOCATION, vEvent.location)
                put(Events.DTSTART, startZdt.toInstant().toEpochMilli())
                put(Events.DTEND, endZdt.toInstant().toEpochMilli())
                put(Events.EVENT_TIMEZONE, startZdt.zone.id) // Preserve original time zone
                put(Events.AVAILABILITY, Events.AVAILABILITY_BUSY)
                put(Events.RRULE, vEvent.recurrenceRule)
            }
            cr.insert(Events.CONTENT_URI, eventValues)
        }
    }
}

// --- Helper: Parse ISO 8601 duration like PT1H30M into Java Duration ---
fun parseICalDuration(duration: String): JavaDuration {
    return try {
        JavaDuration.parse(duration)
    } catch (e: Exception) {
        // Fallback: parse manually PT#H#M#S
        var totalMillis = 0L
        val regex = Regex("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?")
        val match = regex.matchEntire(duration)
        match?.destructured?.let { (h, m, s) ->
            totalMillis += (h.toLongOrNull() ?: 0) * 3600_000
            totalMillis += (m.toLongOrNull() ?: 0) * 60_000
            totalMillis += (s.toLongOrNull() ?: 0) * 1_000
        }
        JavaDuration.ofMillis(totalMillis)
    }
}


fun parseIso(iso: String): Duration? {
    return try { Duration.parse(iso) } catch (e: Exception) { null }
}