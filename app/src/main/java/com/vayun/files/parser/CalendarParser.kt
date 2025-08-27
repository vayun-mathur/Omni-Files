package com.vayun.files.parser

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone

// === VCALENDAR ===
data class VCalendar(
    val prodId: String,
    val version: String,
    val calscale: CalScale? = null,
    val method: Method? = null,
    val name: String? = null,
    val description: String? = null,
    val color: String? = null,
    val events: List<VEvent> = emptyList(),
    val todos: List<VTodo> = emptyList(),
    val journals: List<VJournal> = emptyList(),
    val freeBusy: List<VFreeBusy> = emptyList(),
    val timeZones: List<VTimeZone> = emptyList()
)

enum class CalScale { GREGORIAN }
enum class Method {
    PUBLISH, REQUEST, REPLY, ADD, CANCEL, REFRESH,
    COUNTER, DECLINECOUNTER
}

// === VEVENT ===
data class VEvent(
    val uid: String,
    val dtStamp: ZonedDateTime,
    val dtStart: Temporal?,
    val dtEnd: Temporal? = null,
    val duration: Duration? = null,
    val summary: String? = null,
    val description: String? = null,
    val location: String? = null,
    val status: EventStatus? = null,
    val categories: List<String> = emptyList(),
    val recurrenceRule: String? = null,
    val rDate: List<Temporal> = emptyList(),
    val exDate: List<Temporal> = emptyList(),
    val alarms: List<VAlarm> = emptyList(),
    val organizer: String? = null,
    val attendees: List<String> = emptyList(),
    val recurrenceId: Temporal? = null,
    val sequence: Int? = null,
    val transparency: Transparency? = null,
    val classification: Classification? = null,
    val url: String? = null,
    val resources: List<String> = emptyList(),
    val geo: Geo? = null,
    val contact: String? = null,
    val percentComplete: Int? = null,
    val created: ZonedDateTime? = null,
    val lastModified: ZonedDateTime? = null
)

enum class EventStatus { TENTATIVE, CONFIRMED, CANCELLED }
enum class Transparency { OPAQUE, TRANSPARENT }
enum class Classification { PUBLIC, PRIVATE, CONFIDENTIAL }
data class Geo(val latitude: Double, val longitude: Double)

// === VTODO ===
data class VTodo(
    val uid: String,
    val dtStamp: ZonedDateTime,
    val due: Temporal? = null,
    val completed: ZonedDateTime? = null,
    val dtStart: Temporal? = null,
    val summary: String? = null,
    val description: String? = null,
    val status: TodoStatus? = null,
    val priority: Int? = null,
    val recurrenceRule: String? = null,
    val rDate: List<Temporal> = emptyList(),
    val exDate: List<Temporal> = emptyList(),
    val alarms: List<VAlarm> = emptyList(),
    val organizer: String? = null,
    val attendees: List<String> = emptyList(),
    val sequence: Int? = null,
    val percentComplete: Int? = null,
    val relatedTo: List<String> = emptyList(),
    val created: ZonedDateTime? = null,
    val lastModified: ZonedDateTime? = null,
    val url: String? = null
)

enum class TodoStatus { NEEDS_ACTION, COMPLETED, IN_PROCESS, CANCELLED }

// === VJOURNAL ===
data class VJournal(
    val uid: String,
    val dtStamp: ZonedDateTime,
    val dtStart: Temporal? = null,
    val summary: String? = null,
    val description: String? = null,
    val status: JournalStatus? = null,
    val sequence: Int? = null,
    val created: ZonedDateTime? = null,
    val lastModified: ZonedDateTime? = null
)
enum class JournalStatus { DRAFT, FINAL, CANCELLED }

// === VFREEBUSY ===
data class VFreeBusy(
    val uid: String,
    val dtStamp: ZonedDateTime,
    val dtStart: Temporal,
    val dtEnd: Temporal,
    val freeBusy: List<Period> = emptyList(),
    val organizer: String? = null,
    val attendees: List<String> = emptyList(),
    val comment: String? = null,
    val url: String? = null
)
data class Period(val start: Temporal, val end: Temporal, val type: FreeBusyType = FreeBusyType.BUSY)
enum class FreeBusyType { FREE, BUSY, BUSY_TENTATIVE, BUSY_UNAVAILABLE }

// === VTIMEZONE ===
data class VTimeZone(
    val tzId: String,
    val lastModified: ZonedDateTime? = null,
    val url: String? = null,
    val xLicLocation: String? = null,
    val standard: List<TimeZoneRule> = emptyList(),
    val daylight: List<TimeZoneRule> = emptyList()
)
data class TimeZoneRule(
    val dtStart: LocalDateTime,
    val tzOffsetFrom: String,
    val tzOffsetTo: String,
    val tzName: String,
    val recurrenceRule: String? = null
)

// === VALARM ===
data class VAlarm(
    val action: AlarmAction,
    val trigger: Trigger,
    val description: String? = null,
    val summary: String? = null,
    val attendees: List<String> = emptyList(),
    val duration: Duration? = null,
    val repeat: Int? = null,
    val attach: String? = null
)
enum class AlarmAction { AUDIO, DISPLAY, EMAIL }

// === Helper Types ===
sealed interface Temporal
data class DateValue(val date: LocalDate) : Temporal
data class DateTimeValue(val dateTime: ZonedDateTime) : Temporal

sealed interface Trigger
data class AbsoluteTrigger(val time: ZonedDateTime) : Trigger
data class RelativeTrigger(val duration: Duration, val relatedTo: Related? = null) : Trigger
enum class Related { START, END }
@JvmInline value class Duration(val raw: String)

data class ICalProperty(val name: String, val params: Map<String, String> = emptyMap(), val value: String)

// === Parser ===
object CalendarParser: Parser<VCalendar>() {

    override fun parse(input: String): VCalendar {
        val unfolded = unfoldLines(input)
        val lines = unfolded.replace("\r\n", "\n").replace("\r", "\n").split("\n").map { it.trimEnd() }.filter { it.isNotBlank() }
        val iterator = lines.iterator()
        require(iterator.hasNext() && iterator.next().equals("BEGIN:VCALENDAR", true)) { "Expected BEGIN:VCALENDAR" }
        return parseComponent("VCALENDAR", iterator) as VCalendar
    }

    private fun unfoldLines(input: String) = buildString {
        input.split(Regex("\r?\n")).forEach { line ->
            if (line.startsWith(" ") || line.startsWith("\t")) append(line.drop(1)) else {
                if (isNotEmpty()) append("\n")
                append(line)
            }
        }
    }

    private fun parseLine(line: String): ICalProperty {
        val (nameAndParams, value) = line.split(":", limit = 2)
        val parts = nameAndParams.split(";")
        val name = parts[0].uppercase()
        val params = parts.drop(1).associate {
            val (k, v) = it.split("=", limit = 2)
            k.uppercase() to v
        }
        return ICalProperty(name, params, value)
    }

    private fun parseComponent(expectedName: String, iterator: Iterator<String>): Any {
        val props = mutableListOf<ICalProperty>()
        val children = mutableListOf<Any>()
        while (iterator.hasNext()) {
            val line = iterator.next()
            when {
                line.startsWith("BEGIN:") -> children.add(parseComponent(line.substringAfter("BEGIN:"), iterator))
                line.startsWith("END:") -> {
                    require(line.substringAfter("END:") == expectedName) { "Expected END:$expectedName" }
                    return buildComponent(expectedName, props, children)
                }
                else -> props.add(parseLine(line))
            }
        }
        error("Missing END:$expectedName")
    }

    private fun buildComponent(name: String, props: List<ICalProperty>, children: List<Any>): Any {
        return when(name) {
            "VCALENDAR" -> VCalendar(
                prodId = props.find { it.name == "PRODID" }?.value ?: error("PRODID required"),
                version = props.find { it.name == "VERSION" }?.value ?: error("VERSION required"),
                calscale = props.find { it.name == "CALSCALE" }?.value?.let { CalScale.valueOf(it.uppercase()) },
                method = props.find { it.name == "METHOD" }?.value?.let { Method.valueOf(it.uppercase()) },
                name = props.find { it.name == "NAME" || it.name == "X-WR-CALNAME" }?.value,
                description = props.find { it.name == "DESCRIPTION" }?.value,
                color = props.find { it.name.startsWith("X-APPLE-CALENDAR-COLOR") }?.value,
                events = children.filterIsInstance<VEvent>(),
                todos = children.filterIsInstance<VTodo>(),
                journals = children.filterIsInstance<VJournal>(),
                freeBusy = children.filterIsInstance<VFreeBusy>(),
                timeZones = children.filterIsInstance<VTimeZone>()
            )
            "VEVENT" -> parseVEvent(props, children)
            "VTODO" -> parseVTodo(props, children)
            "VJOURNAL" -> parseVJournal(props, children)
            "VFREEBUSY" -> parseVFreeBusy(props, children)
            "VTIMEZONE" -> parseVTimeZone(props, children)
            "VALARM" -> parseVAlarm(props)
            else -> name
        }
    }

    private fun parseVEvent(props: List<ICalProperty>, children: List<Any>): VEvent {
        return VEvent(
            uid = props.find { it.name == "UID" }?.value ?: error("UID required"),
            dtStamp = parseDateTime(props.find { it.name == "DTSTAMP" }
                ?: error("DTSTAMP required")),
            dtStart = props.find { it.name == "DTSTART" }?.let { parseTemporal(it) },
            dtEnd = props.find { it.name == "DTEND" }?.let { parseTemporal(it) },
            duration = props.find { it.name == "DURATION" }?.let { Duration(it.value) },
            summary = props.find { it.name == "SUMMARY" }?.value,
            description = props.find { it.name == "DESCRIPTION" }?.value,
            location = props.find { it.name == "LOCATION" }?.value,
            status = props.find { it.name == "STATUS" }?.value?.let { EventStatus.valueOf(it.uppercase()) },
            categories = props.filter { it.name == "CATEGORIES" }.map { it.value },
            recurrenceRule = props.find { it.name == "RRULE" }?.value,
            rDate = parseTemporalList(props, "RDATE"),
            exDate = parseTemporalList(props, "EXDATE"),
            alarms = children.filterIsInstance<VAlarm>(),
            organizer = props.find { it.name == "ORGANIZER" }?.value,
            attendees = props.filter { it.name == "ATTENDEE" }.map { it.value },
            recurrenceId = props.find { it.name == "RECURRENCE-ID" }?.let { parseTemporal(it) },
            sequence = props.find { it.name == "SEQUENCE" }?.value?.toIntOrNull(),
            transparency = props.find { it.name == "TRANSP" }?.value?.let { Transparency.valueOf(it.uppercase()) },
            classification = props.find { it.name == "CLASS" }?.value?.let { Classification.valueOf(it.uppercase()) },
            url = props.find { it.name == "URL" }?.value,
            resources = props.filter { it.name == "RESOURCES" }.map { it.value },
            geo = props.find { it.name == "GEO" }?.let { parseGeo(it.value) },
            contact = props.find { it.name == "CONTACT" }?.value,
            percentComplete = props.find { it.name == "PERCENT-COMPLETE" }?.value?.toIntOrNull(),
            created = props.find { it.name == "CREATED" }?.let { parseDateTime(it) },
            lastModified = props.find { it.name == "LAST-MODIFIED" }?.let { parseDateTime(it) }
        )
    }

    private fun parseVTodo(props: List<ICalProperty>, children: List<Any>): VTodo {
        return VTodo(
            uid = props.find { it.name == "UID" }?.value ?: error("UID required"),
            dtStamp = parseDateTime(props.find { it.name == "DTSTAMP" }
                ?: error("DTSTAMP required")),
            dtStart = props.find { it.name == "DTSTART" }?.let { parseTemporal(it) },
            due = props.find { it.name == "DUE" }?.let { parseTemporal(it) },
            completed = props.find { it.name == "COMPLETED" }?.let { parseDateTime(it) },
            summary = props.find { it.name == "SUMMARY" }?.value,
            description = props.find { it.name == "DESCRIPTION" }?.value,
            status = props.find { it.name == "STATUS" }?.value?.let { TodoStatus.valueOf(it.uppercase()) },
            priority = props.find { it.name == "PRIORITY" }?.value?.toIntOrNull(),
            recurrenceRule = props.find { it.name == "RRULE" }?.value,
            rDate = parseTemporalList(props, "RDATE"),
            exDate = parseTemporalList(props, "EXDATE"),
            alarms = children.filterIsInstance<VAlarm>(),
            organizer = props.find { it.name == "ORGANIZER" }?.value,
            attendees = props.filter { it.name == "ATTENDEE" }.map { it.value },
            sequence = props.find { it.name == "SEQUENCE" }?.value?.toIntOrNull(),
            percentComplete = props.find { it.name == "PERCENT-COMPLETE" }?.value?.toIntOrNull(),
            relatedTo = props.filter { it.name == "RELATED-TO" }.map { it.value },
            created = props.find { it.name == "CREATED" }?.let { parseDateTime(it) },
            lastModified = props.find { it.name == "LAST-MODIFIED" }?.let { parseDateTime(it) },
            url = props.find { it.name == "URL" }?.value
        )
    }

    private fun parseVJournal(props: List<ICalProperty>, children: List<Any>): VJournal {
        return VJournal(
            uid = props.find { it.name == "UID" }?.value ?: error("UID required"),
            dtStamp = parseDateTime(props.find { it.name == "DTSTAMP" }
                ?: error("DTSTAMP required")),
            dtStart = props.find { it.name == "DTSTART" }?.let { parseTemporal(it) },
            summary = props.find { it.name == "SUMMARY" }?.value,
            description = props.find { it.name == "DESCRIPTION" }?.value,
            status = props.find { it.name == "STATUS" }?.value?.let { JournalStatus.valueOf(it.uppercase()) },
            sequence = props.find { it.name == "SEQUENCE" }?.value?.toIntOrNull(),
            created = props.find { it.name == "CREATED" }?.let { parseDateTime(it) },
            lastModified = props.find { it.name == "LAST-MODIFIED" }?.let { parseDateTime(it) }
        )
    }

    private fun parseVFreeBusy(props: List<ICalProperty>, children: List<Any>): VFreeBusy {
        val fbPeriods = props.filter { it.name == "FREEBUSY" }.map { periodProp ->
            val parts = periodProp.value.split("/")
            Period(parseTemporal(ICalProperty("FBSTART", emptyMap(), parts[0])), parseTemporal(ICalProperty("FBEND", emptyMap(), parts[1])))
        }
        return VFreeBusy(
            uid = props.find { it.name == "UID" }?.value ?: error("UID required"),
            dtStamp = parseDateTime(props.find { it.name == "DTSTAMP" }
                ?: error("DTSTAMP required")),
            dtStart = props.find { it.name == "DTSTART" }?.let { parseTemporal(it) } ?: error("DTSTART required"),
            dtEnd = props.find { it.name == "DTEND" }?.let { parseTemporal(it) } ?: error("DTEND required"),
            freeBusy = fbPeriods,
            organizer = props.find { it.name == "ORGANIZER" }?.value,
            attendees = props.filter { it.name == "ATTENDEE" }.map { it.value },
            comment = props.find { it.name == "COMMENT" }?.value,
            url = props.find { it.name == "URL" }?.value
        )
    }

    private fun parseVTimeZone(props: List<ICalProperty>, children: List<Any>): VTimeZone {
        return VTimeZone(
            tzId = props.find { it.name == "TZID" }?.value ?: error("TZID required"),
            lastModified = props.find { it.name == "LAST-MODIFIED" }?.let { parseDateTime(it) },
            url = props.find { it.name == "TZURL" }?.value,
            xLicLocation = props.find { it.name == "X-LIC-LOCATION" }?.value,
            standard = children.filterIsInstance<TimeZoneRule>(),
            daylight = children.filterIsInstance<TimeZoneRule>()
        )
    }

    private fun parseVAlarm(props: List<ICalProperty>): VAlarm {
        return VAlarm(
            action = props.find { it.name == "ACTION" }?.value?.let { AlarmAction.valueOf(it.uppercase()) } ?: error("ACTION required"),
            trigger = props.find { it.name == "TRIGGER" }?.let { parseTrigger(it) } ?: error("TRIGGER required"),
            description = props.find { it.name == "DESCRIPTION" }?.value,
            summary = props.find { it.name == "SUMMARY" }?.value,
            attendees = props.filter { it.name == "ATTENDEE" }.map { it.value },
            duration = props.find { it.name == "DURATION" }?.value?.let { Duration(it) },
            repeat = props.find { it.name == "REPEAT" }?.value?.toIntOrNull(),
            attach = props.find { it.name == "ATTACH" }?.value
        )
    }


    private val dateTimeUTC = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX")
    private val dateTimeLocal = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")

    private fun parseDateTime(prop: ICalProperty): ZonedDateTime {
        // 1. If TZID exists, use that zone
        val zoneId: ZoneId? = prop.params["TZID"]?.let { tz ->
            try {
                TimeZone.getTimeZone(tz).toZoneId()
            } catch (e: Exception) {
                null
            }
        }

        return when {
            prop.value.endsWith("Z") -> {
                // UTC absolute time
                ZonedDateTime.parse(prop.value, dateTimeUTC)
            }
            prop.value.length == 15 -> {
                // Local time with optional TZID
                val ldt = LocalDateTime.parse(prop.value, dateTimeLocal)
                if (zoneId != null) ldt.atZone(zoneId)
                else ldt.atZone(ZoneId.systemDefault()) // floating time: assume system zone
            }
            else -> error("Unsupported DATE-TIME format: ${prop.value}")
        }
    }

    private fun parseTemporal(prop: ICalProperty): Temporal =
        if (prop.params["VALUE"] == "DATE" || prop.value.length == 8)
            DateValue(LocalDate.parse(prop.value, DateTimeFormatter.BASIC_ISO_DATE))
        else
            DateTimeValue(parseDateTime(prop))


    private fun parseTrigger(prop: ICalProperty): Trigger {
        val related = prop.params["RELATED"]?.let { Related.valueOf(it.uppercase()) }

        return if (prop.value.startsWith("P") || prop.value.startsWith("+P") || prop.value.startsWith("-P")) {
            // Relative duration trigger
            RelativeTrigger(Duration(prop.value), related)
        } else {
            // Absolute trigger: respect TZID if present
            AbsoluteTrigger(parseDateTime(prop))
        }
    }

    private fun parseTemporalList(props: List<ICalProperty>, name: String): List<Temporal> =
        props.filter { it.name == name }.flatMap { prop ->
            // RDATE/EXDATE may contain multiple comma-separated datetimes
            prop.value.split(",").map { singleValue ->
                val tempProp = prop.copy(value = singleValue)
                parseTemporal(tempProp)
            }
        }

    private fun parseGeo(value: String): Geo {
        val (lat, lon) = value.split(";").map { it.toDouble() }
        return Geo(lat, lon)
    }
}
