package tech.wbr.habitcounter.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "counters")
data class CounterRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val colorArgb: Long,
    val pinned: Boolean = false,
    val createdAtMillis: Long,
)

@Entity(
    tableName = "counter_events",
    foreignKeys = [
        ForeignKey(
            entity = CounterRecord::class,
            parentColumns = ["id"],
            childColumns = ["counterId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("counterId"), Index("occurredAtMillis")],
)
data class CounterEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val counterId: Long,
    val occurredAtMillis: Long,
)

data class CounterCount(
    val counterId: Long,
    val count: Long,
)

@Dao
interface CounterDao {
    @Query("SELECT * FROM counters ORDER BY createdAtMillis, id")
    fun observeCounters(): Flow<List<CounterRecord>>

    @Query("SELECT * FROM counters ORDER BY createdAtMillis, id")
    suspend fun allCounters(): List<CounterRecord>

    @Query("SELECT * FROM counters WHERE id = :counterId LIMIT 1")
    suspend fun counter(counterId: Long): CounterRecord?

    @Query(
        """
        SELECT counterId, COUNT(*) AS count
        FROM counter_events
        WHERE occurredAtMillis >= :startMillis AND occurredAtMillis < :endMillis
        GROUP BY counterId
        """,
    )
    fun observeCounts(startMillis: Long, endMillis: Long): Flow<List<CounterCount>>

        @Query(
                """
                SELECT COUNT(*) FROM counter_events
                WHERE counterId = :counterId
                    AND occurredAtMillis >= :startMillis
                    AND occurredAtMillis < :endMillis
                """,
        )
        suspend fun countEvents(counterId: Long, startMillis: Long, endMillis: Long): Long

    @Insert
    suspend fun insertCounter(counter: CounterRecord): Long

    @Update
    suspend fun updateCounter(counter: CounterRecord)

    @Insert
    suspend fun insertEvent(event: CounterEvent)

    @Query("DELETE FROM counter_events WHERE counterId = :counterId")
    suspend fun deleteEvents(counterId: Long)

    @Query("DELETE FROM counters WHERE id = :counterId")
    suspend fun deleteCounter(counterId: Long)

    @Query(
        """
        SELECT occurredAtMillis FROM counter_events
        WHERE counterId = :counterId
          AND occurredAtMillis >= :startMillis
          AND occurredAtMillis < :endMillis
        ORDER BY occurredAtMillis
        """,
    )
    suspend fun eventTimes(
        counterId: Long,
        startMillis: Long,
        endMillis: Long,
    ): List<Long>
}

@Database(
    entities = [CounterRecord::class, CounterEvent::class],
    version = 1,
    exportSchema = true,
)
abstract class CounterDatabase : RoomDatabase() {
    abstract fun counterDao(): CounterDao

    companion object {
        @Volatile
        private var instance: CounterDatabase? = null

        fun get(context: Context): CounterDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                CounterDatabase::class.java,
                "habit-counter.db",
            ).build().also { instance = it }
        }
    }
}