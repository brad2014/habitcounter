package tech.wbr.habitcounter.launcher

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import tech.wbr.habitcounter.data.CounterDatabase
import tech.wbr.habitcounter.data.CounterRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CounterWidgetConfigureActivity : ComponentActivity() {
    private var widgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(Activity.RESULT_CANCELED)
        widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        val suggestedCounterId = AppWidgetManager.getInstance(this)
            .getAppWidgetOptions(widgetId)
            .getLong(CounterWidgetProvider.EXTRA_COUNTER_ID, 0L)

        setContent {
            val counters by produceState(initialValue = emptyList<CounterRecord>()) {
                value = withContext(Dispatchers.IO) {
                    CounterDatabase.get(this@CounterWidgetConfigureActivity).counterDao()
                        .allCounters()
                        .filter { it.pinned }
                }
            }
            MaterialTheme {
                WidgetCounterPicker(
                    counters = counters,
                    onSelect = ::finishConfiguration,
                    suggestedCounterId = suggestedCounterId,
                )
            }
        }
    }

    private fun finishConfiguration(counter: CounterRecord) {
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                CounterWidgetProvider.saveWidgetCounter(this@CounterWidgetConfigureActivity, widgetId, counter.id)
                CounterWidgetProvider.updateWidget(this@CounterWidgetConfigureActivity, widgetId)
            }
            setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
            finish()
        }
    }
}

@Composable
private fun WidgetCounterPicker(
    counters: List<CounterRecord>,
    onSelect: (CounterRecord) -> Unit,
    suggestedCounterId: Long,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Choose a counter", style = MaterialTheme.typography.headlineSmall)
        if (counters.isEmpty()) {
            Text("Pin a counter in HabitCounter before adding its widget.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        counters.forEach { counter ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(counter.label, modifier = Modifier.weight(1f), color = Color(counter.colorArgb))
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { onSelect(counter) }) {
                        Text(if (counter.id == suggestedCounterId) "Add" else "Choose")
                    }
                }
            }
        }
    }
}