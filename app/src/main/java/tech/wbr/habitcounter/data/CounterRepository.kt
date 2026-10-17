package tech.wbr.habitcounter.data

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive

data class CounterSummary(
    val counter: CounterRecord,
    val monthCount: Long,
    val dayCount: Long,
    val hourCount: Long,
)

@OptIn(ExperimentalCoroutinesApi::class)
class CounterRepository(
    private val dao: CounterDao,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val clock: Clock = Clock.system(zone),
) {
    private val nowFlow = flow {
        while (currentCoroutineContext().isActive) {
            emit(clock.instant())
            delay(60_000L)
        }
    }

    fun observeSummaries(): Flow<List<CounterSummary>> = nowFlow.flatMapLatest { now ->
        val starts = periodStarts(now, zone)
        val endMillis = starts.endExclusive.toEpochMilli()
        combine(
            dao.observeCounters(),
            dao.observeCounts(starts.month.toEpochMilli(), endMillis),
            dao.observeCounts(starts.day.toEpochMilli(), endMillis),
            dao.observeCounts(starts.hour.toEpochMilli(), endMillis),
        ) { counters, monthCounts, dayCounts, hourCounts ->
            val months = monthCounts.associate { it.counterId to it.count }
            val days = dayCounts.associate { it.counterId to it.count }
            val hours = hourCounts.associate { it.counterId to it.count }
            counters.map { counter ->
                CounterSummary(
                    counter = counter,
                    monthCount = months[counter.id] ?: 0,
                    dayCount = days[counter.id] ?: 0,
                    hourCount = hours[counter.id] ?: 0,
                )
            }
        }
    }

    suspend fun saveCounter(
        id: Long,
        label: String,
        colorArgb: Long,
        pinned: Boolean,
    ): Long {
        val now = clock.millis()
        val counter = CounterRecord(
            id = id,
            label = label.trim(),
            colorArgb = colorArgb,
            pinned = pinned,
            createdAtMillis = now,
        )
        return if (id == 0L) {
            dao.insertCounter(counter)
        } else {
            dao.updateCounter(counter)
            id
        }
    }

    suspend fun increment(counterId: Long, at: Instant = clock.instant()) {
        dao.insertEvent(CounterEvent(counterId = counterId, occurredAtMillis = at.toEpochMilli()))
    }

    suspend fun reset(counterId: Long) = dao.deleteEvents(counterId)

    suspend fun delete(counterId: Long) = dao.deleteCounter(counterId)

    suspend fun eventsBetween(counterId: Long, start: Instant, endExclusive: Instant): List<Long> =
        dao.eventTimes(counterId, start.toEpochMilli(), endExclusive.toEpochMilli())
}