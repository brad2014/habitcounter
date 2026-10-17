package tech.wbr.habitcounter.data

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class ChartInterval {
    HOUR,
    DAY,
    MONTH,
}

data class PeriodWindow(
    val start: Instant,
    val endExclusive: Instant,
)

data class GraphBucket(
    val start: Instant,
    val endExclusive: Instant,
    val label: String,
)

data class PeriodStarts(
    val month: Instant,
    val day: Instant,
    val hour: Instant,
    val monthEndExclusive: Instant,
    val dayEndExclusive: Instant,
    val hourEndExclusive: Instant,
)

fun periodStarts(now: Instant, zone: ZoneId): PeriodStarts {
    val localNow = now.atZone(zone)
    val today = localNow.toLocalDate()
    val currentMonth = YearMonth.from(localNow)
    val monthStart = currentMonth.atDay(1).atStartOfDay(zone).toInstant()
    val monthEnd = currentMonth.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()
    val dayStart = today.atStartOfDay(zone).toInstant()
    val dayEnd = today.plusDays(1).atStartOfDay(zone).toInstant()
    val currentHour = localNow.truncatedTo(ChronoUnit.HOURS)
    val hourStart = currentHour.toInstant()
    val hourEnd = currentHour.plusHours(1).toInstant()

    return PeriodStarts(
        month = monthStart,
        day = dayStart,
        hour = hourStart,
        monthEndExclusive = monthEnd,
        dayEndExclusive = dayEnd,
        hourEndExclusive = hourEnd,
    )
}

fun graphBuckets(interval: ChartInterval, now: Instant, zone: ZoneId): List<GraphBucket> {
    val localNow = now.atZone(zone)
    return when (interval) {
        ChartInterval.MONTH -> {
            val currentMonth = YearMonth.from(localNow)
            (11 downTo 0).map { offset ->
                val month = currentMonth.minusMonths(offset.toLong())
                val start = month.atDay(1).atStartOfDay(zone).toInstant()
                val end = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant()
                val pattern = if (month.year == currentMonth.year) "MMM" else "MMM yy"
                GraphBucket(start, end, DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).format(month))
            }
        }

        ChartInterval.DAY -> {
            val today = localNow.toLocalDate()
            (30 downTo 0).map { offset ->
                val date = today.minusDays(offset.toLong())
                val start = date.atStartOfDay(zone).toInstant()
                val end = date.plusDays(1).atStartOfDay(zone).toInstant()
                val pattern = if (date.month == today.month) "d" else "MMM d"
                GraphBucket(start, end, DateTimeFormatter.ofPattern(pattern, Locale.getDefault()).format(date))
            }
        }

        ChartInterval.HOUR -> {
            val currentHour = localNow.truncatedTo(ChronoUnit.HOURS)
            (23 downTo 0).map { offset ->
                val start = currentHour.minusHours(offset.toLong()).toInstant()
                val end = start.plus(1, ChronoUnit.HOURS)
                val label = DateTimeFormatter.ofPattern("ha", Locale.getDefault())
                    .format(start.atZone(zone))
                    .lowercase(Locale.getDefault())
                GraphBucket(start, end, label)
            }
        }
    }
}

fun countEventsByBucket(eventTimes: List<Long>, buckets: List<GraphBucket>): List<Long> =
    buckets.map { bucket ->
        eventTimes.count { time ->
            val instant = Instant.ofEpochMilli(time)
            instant >= bucket.start && instant < bucket.endExclusive
        }.toLong()
    }