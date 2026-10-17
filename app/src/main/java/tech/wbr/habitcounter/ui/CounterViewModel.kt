package tech.wbr.habitcounter.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import tech.wbr.habitcounter.data.CounterDatabase
import tech.wbr.habitcounter.data.CounterRepository
import tech.wbr.habitcounter.data.CounterSummary
import tech.wbr.habitcounter.launcher.CounterShortcutManager
import tech.wbr.habitcounter.launcher.CounterWidgetProvider
import java.time.Instant
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CounterViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CounterRepository(CounterDatabase.get(application).counterDao())

    val counters: StateFlow<List<CounterSummary>> = repository.observeSummaries().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    fun saveCounter(id: Long, label: String, colorArgb: Long, pinned: Boolean) {
        viewModelScope.launch {
            val wasPinned = counters.value.firstOrNull { it.counter.id == id }?.counter?.pinned == true
            val savedId = repository.saveCounter(id, label, colorArgb, pinned)
            val context = getApplication<Application>()
            CounterShortcutManager.sync(context, savedId, label.trim(), colorArgb, pinned)
            if (pinned && !wasPinned) CounterWidgetProvider.requestPinForCounter(context, savedId)
            CounterWidgetProvider.refreshCounterWidgets(context, savedId)
        }
    }

    fun increment(counterId: Long) {
        viewModelScope.launch {
            repository.increment(counterId)
            CounterWidgetProvider.refreshCounterWidgets(getApplication(), counterId)
        }
    }

    fun reset(counterId: Long) {
        viewModelScope.launch {
            repository.reset(counterId)
            CounterWidgetProvider.refreshCounterWidgets(getApplication(), counterId)
        }
    }

    fun delete(counterId: Long) {
        viewModelScope.launch {
            repository.delete(counterId)
            CounterShortcutManager.remove(getApplication(), counterId)
            CounterWidgetProvider.refreshCounterWidgets(getApplication(), counterId)
        }
    }

    suspend fun eventTimes(counterId: Long, start: Instant, endExclusive: Instant): List<Long> =
        repository.eventsBetween(counterId, start, endExclusive)
}