package tech.wbr.habitcounter.ui

import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import tech.wbr.habitcounter.data.ChartInterval
import tech.wbr.habitcounter.data.CounterRecord

private sealed interface AppRoute {
    data object Home : AppRoute
    data class Edit(val counterId: Long) : AppRoute
    data class Graph(val counterId: Long, val interval: ChartInterval) : AppRoute
}

@Composable
fun CounterApp(counterViewModel: CounterViewModel = viewModel()) {
    val summaries by counterViewModel.counters.collectAsState()
    var route by remember { mutableStateOf<AppRoute>(AppRoute.Home) }
    var draft by remember { mutableStateOf(CounterDraft()) }
    var showUnsavedDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val editingId = (route as? AppRoute.Edit)?.counterId
    val original = editingId?.let { id -> summaries.firstOrNull { it.counter.id == id }?.counter }
    val draftIsDirty = editingId != null && draft.isDirty(original)

    BackHandler(enabled = route != AppRoute.Home) {
        if (route is AppRoute.Edit && draftIsDirty) showUnsavedDialog = true
        else route = AppRoute.Home
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF356B5B),
            onPrimary = Color.White,
            secondary = Color(0xFFB56E43),
            background = Color(0xFFF5F6F1),
            surface = Color(0xFFF5F6F1),
            surfaceVariant = Color(0xFFE8EBE4),
            onSurface = Color(0xFF202923),
            onSurfaceVariant = Color(0xFF5E685F),
        ),
    ) {
        Surface {
            when (val current = route) {
                AppRoute.Home -> HomeScreen(
                    counters = summaries,
                    onNewCounter = {
                        draft = CounterDraft()
                        route = AppRoute.Edit(0)
                    },
                    onIncrement = counterViewModel::increment,
                    onEdit = { counter ->
                        draft = CounterDraft.from(counter)
                        route = AppRoute.Edit(counter.id)
                    },
                    onGraph = { id, interval -> route = AppRoute.Graph(id, interval) },
                )

                is AppRoute.Edit -> CounterEditorScreen(
                    draft = draft,
                    isExisting = current.counterId != 0L,
                    onDraftChange = { draft = it },
                    onBack = {
                        if (draftIsDirty) showUnsavedDialog = true else route = AppRoute.Home
                    },
                    onSave = {
                        if (draft.label.isNotBlank()) {
                            counterViewModel.saveCounter(draft.id, draft.label, draft.colorArgb, draft.pinned)
                            route = AppRoute.Home
                        }
                    },
                    onReset = { showResetDialog = true },
                    onDelete = { showDeleteDialog = true },
                )

                is AppRoute.Graph -> {
                    val summary = summaries.firstOrNull { it.counter.id == current.counterId }
                    if (summary != null) {
                        CounterGraphScreen(
                            counter = summary,
                            interval = current.interval,
                            onIntervalChange = { route = current.copy(interval = it) },
                            onBack = { route = AppRoute.Home },
                            onSettings = {
                                draft = CounterDraft.from(summary.counter)
                                route = AppRoute.Edit(summary.counter.id)
                            },
                            loadEventTimes = counterViewModel::eventTimes,
                        )
                    }
                }
            }
        }
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Save edits?") },
            confirmButton = {
                TextButton(onClick = {
                    if (draft.label.isNotBlank()) {
                        counterViewModel.saveCounter(draft.id, draft.label, draft.colorArgb, draft.pinned)
                        route = AppRoute.Home
                        showUnsavedDialog = false
                    }
                }) { Text("Save edits") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUnsavedDialog = false
                    route = AppRoute.Home
                }) { Text("Discard edits") }
            },
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset history?") },
            text = { Text("This removes every recorded instance for this counter.") },
            confirmButton = {
                TextButton(onClick = {
                    counterViewModel.reset(draft.id)
                    showResetDialog = false
                    route = AppRoute.Home
                }) { Text("Reset counter") }
            },
            dismissButton = { TextButton(onClick = { showResetDialog = false }) { Text("Cancel") } },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete counter?") },
            text = { Text("The counter and its recorded history will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    counterViewModel.delete(draft.id)
                    showDeleteDialog = false
                    route = AppRoute.Home
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } },
        )
    }
}