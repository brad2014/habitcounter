package tech.wbr.habitcounter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.wbr.habitcounter.data.ChartInterval
import tech.wbr.habitcounter.data.CounterRecord
import tech.wbr.habitcounter.data.CounterSummary
import tech.wbr.habitcounter.data.GraphBucket
import tech.wbr.habitcounter.data.countEventsByBucket
import tech.wbr.habitcounter.data.graphBuckets
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlinx.coroutines.flow.first

data class CounterDraft(
    val id: Long = 0,
    val label: String = "",
    val colorArgb: Long = 0xFF4D7866L,
    val pinned: Boolean = false,
) {
    fun isDirty(original: CounterRecord?): Boolean = when {
        original == null -> label.isNotBlank() || colorArgb != 0xFF4D7866L || pinned
        else -> label != original.label || colorArgb != original.colorArgb || pinned != original.pinned
    }

    companion object {
        fun from(counter: CounterRecord) = CounterDraft(
            id = counter.id,
            label = counter.label,
            colorArgb = counter.colorArgb,
            pinned = counter.pinned,
        )
    }
}

private val counterPalette = listOf(
    0xFF4D7866L,
    0xFFB56E43L,
    0xFF5576A1L,
    0xFFB85158L,
    0xFF8A6B9CL,
    0xFF9A812EL,
)

@Composable
fun HomeScreen(
    counters: List<CounterSummary>,
    onNewCounter: () -> Unit,
    onIncrement: (Long) -> Unit,
    onEdit: (CounterRecord) -> Unit,
    onGraph: (Long, ChartInterval) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 18.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("HABITCOUNTER", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Text("Your counters", style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Serif)
            }
            IconButton(onClick = onNewCounter) { Icon(Icons.Outlined.Add, contentDescription = "New counter") }
        }

        val monthLabel = remember { DateTimeFormatter.ofPattern("MMM", Locale.getDefault()).format(YearMonth.now()) }
        if (counters.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                Column {
                    Text("Start with one small signal.", style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Serif)
                    Spacer(Modifier.height(14.dp))
                    Text("Create a counter to begin collecting events.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = onNewCounter) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("New counter")
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(22.dp))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.weight(1f))
                    MetricHeading(monthLabel, Modifier.weight(1f))
                    MetricHeading("Today", Modifier.weight(1f))
                    MetricHeading("Hour", Modifier.weight(1f))
                    Spacer(Modifier.width(84.dp))
                }
                HorizontalDivider(modifier = Modifier.padding(top = 7.dp, bottom = 3.dp))
                counters.forEach { summary ->
                    CounterRow(
                        summary = summary,
                        onIncrement = { onIncrement(summary.counter.id) },
                        onEdit = { onEdit(summary.counter) },
                        onGraph = { interval -> onGraph(summary.counter.id, interval) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun MetricHeading(label: String, modifier: Modifier) {
    Text(label, modifier = modifier, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun CounterRow(
    summary: CounterSummary,
    onIncrement: () -> Unit,
    onEdit: () -> Unit,
    onGraph: (ChartInterval) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(Color(summary.counter.colorArgb)))
            Spacer(Modifier.width(9.dp))
            Text(summary.counter.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            IconButton(onClick = onIncrement, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Outlined.Add, contentDescription = "Record ${summary.counter.label}")
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(42.dp)) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit ${summary.counter.label}")
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MetricButton(summary.monthCount, Modifier.weight(1f)) { onGraph(ChartInterval.MONTH) }
            MetricButton(summary.dayCount, Modifier.weight(1f)) { onGraph(ChartInterval.DAY) }
            MetricButton(summary.hourCount, Modifier.weight(1f)) { onGraph(ChartInterval.HOUR) }
            Spacer(Modifier.width(80.dp))
        }
    }
}

@Composable
private fun MetricButton(count: Long, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(5.dp),
    ) {
        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(horizontal = 11.dp)) {
            Text(count.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun CounterEditorScreen(
    draft: CounterDraft,
    isExisting: Boolean,
    onDraftChange: (CounterDraft) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp).padding(top = 14.dp, bottom = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
            Text(if (isExisting) "Edit counter" else "New counter", style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Serif)
        }
        Spacer(Modifier.height(26.dp))
        Text("LABEL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = draft.label,
            onValueChange = { onDraftChange(draft.copy(label = it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("e.g. Water") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        )
        Spacer(Modifier.height(25.dp))
        Text("COLOR", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(modifier = Modifier.padding(top = 13.dp), horizontalArrangement = Arrangement.spacedBy(15.dp)) {
            counterPalette.forEach { colorValue ->
                val selected = colorValue == draft.colorArgb
                Surface(
                    onClick = { onDraftChange(draft.copy(colorArgb = colorValue)) },
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = Color(colorValue),
                    border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else null,
                ) {
                    if (selected) Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Check, contentDescription = "Selected color", tint = Color.White)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Pin to home screen", style = MaterialTheme.typography.titleMedium)
                Text("Make this counter available outside the app.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = draft.pinned, onCheckedChange = { onDraftChange(draft.copy(pinned = it)) })
        }
        Spacer(Modifier.weight(1f))
        if (isExisting) {
            HorizontalDivider()
            Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onReset) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Reset history")
                }
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Delete")
                }
            }
        }
        Button(onClick = onSave, enabled = draft.label.isNotBlank(), modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(5.dp)) {
            Text("Save")
        }
    }
}

@Composable
fun CounterGraphScreen(
    counter: CounterSummary,
    interval: ChartInterval,
    onIntervalChange: (ChartInterval) -> Unit,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    loadEventTimes: suspend (Long, Instant, Instant) -> List<Long>,
) {
    val now = remember { Instant.now() }
    val zone = remember { ZoneId.systemDefault() }
    val buckets = remember(interval, now, zone) { graphBuckets(interval, now, zone) }
    val chartScrollState = rememberScrollState()
    var counts by remember(interval, counter.counter.id) { mutableStateOf(emptyList<Long>()) }

    LaunchedEffect(interval, buckets.size) {
        snapshotFlow { chartScrollState.maxValue }.first { it > 0 }
        chartScrollState.scrollTo(chartScrollState.maxValue)
    }

    LaunchedEffect(counter.counter.id, buckets) {
        val times = loadEventTimes(counter.counter.id, buckets.first().start, buckets.last().endExclusive)
        counts = countEventsByBucket(times, buckets)
    }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 14.dp, bottom = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
            Column(modifier = Modifier.weight(1f)) {
                Text(counter.counter.label, style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Serif)
                Text("Activity", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, contentDescription = "Count settings") }
        }
        Spacer(Modifier.height(26.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val options = listOf(ChartInterval.HOUR, ChartInterval.DAY, ChartInterval.MONTH)
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = interval == option,
                    onClick = { onIntervalChange(option) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                ) {
                    Text(when (option) {
                        ChartInterval.HOUR -> "24 hours"
                        ChartInterval.DAY -> "31 days"
                        ChartInterval.MONTH -> "12 months"
                    })
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(counts.sum().toString(), style = MaterialTheme.typography.displaySmall, fontFamily = FontFamily.Serif)
        Text(intervalDescription(interval), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(25.dp))
        HorizontalDivider()
        BucketChart(
            buckets = buckets,
            counts = counts,
            color = Color(counter.counter.colorArgb),
            interval = interval,
            scrollState = chartScrollState,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
    }
}

@Composable
private fun BucketChart(
    buckets: List<GraphBucket>,
    counts: List<Long>,
    color: Color,
    interval: ChartInterval,
    scrollState: androidx.compose.foundation.ScrollState,
    modifier: Modifier = Modifier,
) {
    val peak = max(1L, counts.maxOrNull() ?: 0L).toFloat()
    Row(
        modifier = modifier.horizontalScroll(scrollState).padding(top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(if (interval == ChartInterval.MONTH) 12.dp else 7.dp),
    ) {
        buckets.forEachIndexed { index, bucket ->
            val count = counts.getOrElse(index) { 0L }
            val barHeight = (112f * count / peak).coerceAtLeast(3f)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                Text(count.toString(), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(5.dp))
                Box(Modifier.width(if (interval == ChartInterval.MONTH) 22.dp else 15.dp)
                    .height(barHeight.dp)
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(color))
                Spacer(Modifier.height(7.dp))
                Text(bucket.label, fontSize = 9.sp, maxLines = 1, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun intervalDescription(interval: ChartInterval): String = when (interval) {
    ChartInterval.HOUR -> "Instances in the past 24 hours"
    ChartInterval.DAY -> "Instances in the past 31 days"
    ChartInterval.MONTH -> "Instances in the past 12 months"
}