package tech.wbr.habitcounter.data

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class PeriodBucketsTest {
    private val losAngeles = ZoneId.of("America/Los_Angeles")

    @Test
    fun periodStartsUseLocalCalendarBoundaries() {
        val now = Instant.parse("2025-03-09T18:30:00Z")

        val starts = periodStarts(now, losAngeles)

        assertEquals(Instant.parse("2025-03-01T08:00:00Z"), starts.month)
        assertEquals(Instant.parse("2025-03-09T08:00:00Z"), starts.day)
        assertEquals(Instant.parse("2025-03-09T18:00:00Z"), starts.hour)
        assertEquals(now.plusMillis(1), starts.endExclusive)
    }

    @Test
    fun dailyBucketsRespectDaylightSavingTransitions() {
        val now = Instant.parse("2025-03-09T18:30:00Z")

        val buckets = graphBuckets(ChartInterval.DAY, now, losAngeles)
        val transitionDay = buckets.single { bucket ->
            bucket.start.atZone(losAngeles).toLocalDate() == LocalDate.parse("2025-03-09")
        }

        assertEquals(31, buckets.size)
        assertEquals(23, Duration.between(transitionDay.start, transitionDay.endExclusive).toHours())
    }

    @Test
    fun eventCountsUseStartInclusiveEndExclusiveBuckets() {
        val now = Instant.parse("2025-01-02T12:30:00Z")
        val buckets = graphBuckets(ChartInterval.DAY, now, ZoneId.of("UTC"))
        val first = buckets.first()
        val second = buckets[1]
        val events = listOf(
            first.start.toEpochMilli(),
            first.endExclusive.minusMillis(1).toEpochMilli(),
            first.endExclusive.toEpochMilli(),
        )

        val counts = countEventsByBucket(events, buckets)

        assertEquals(2L, counts.first())
        assertEquals(1L, counts[1])
        assertEquals(second.start, first.endExclusive)
    }
}