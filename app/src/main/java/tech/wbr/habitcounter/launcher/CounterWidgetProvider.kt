package tech.wbr.habitcounter.launcher

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.RemoteViews
import androidx.core.content.edit
import tech.wbr.habitcounter.R
import tech.wbr.habitcounter.data.CounterDatabase
import tech.wbr.habitcounter.data.CounterEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CounterWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, widgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                widgetIds.forEach { updateWidget(context, it) }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onDeleted(context: Context, widgetIds: IntArray) {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        preferences.edit {
            widgetIds.forEach { remove(widgetKey(it)) }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_INCREMENT) {
            super.onReceive(context, intent)
            return
        }

        val counterId = intent.getLongExtra(EXTRA_COUNTER_ID, 0L)
        if (counterId <= 0L) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                CounterDatabase.get(context).counterDao().insertEvent(
                    CounterEvent(counterId = counterId, occurredAtMillis = System.currentTimeMillis()),
                )
                refreshCounterWidgets(context, counterId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_COUNTER_ID = "tech.wbr.habitcounter.extra.COUNTER_ID"
        const val ACTION_INCREMENT = "tech.wbr.habitcounter.action.WIDGET_INCREMENT"
        private const val PREFERENCES = "counter_widgets"
        private const val LAST_HOUR_MILLIS = 60 * 60 * 1000L

        fun requestPinForCounter(context: Context, counterId: Long) {
            val manager = AppWidgetManager.getInstance(context)
            if (!manager.isRequestPinAppWidgetSupported) return
            val options = Bundle().apply { putLong(EXTRA_COUNTER_ID, counterId) }
            manager.requestPinAppWidget(
                ComponentName(context, CounterWidgetProvider::class.java),
                options,
                null,
            )
        }

        fun saveWidgetCounter(context: Context, widgetId: Int, counterId: Long) {
            context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit { putLong(widgetKey(widgetId), counterId) }
        }

        suspend fun refreshCounterWidgets(context: Context, counterId: Long) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, CounterWidgetProvider::class.java)
            manager.getAppWidgetIds(component)
                .filter { widgetCounterId(context, it) == counterId }
                .forEach { updateWidget(context, it) }
        }

        suspend fun updateWidget(context: Context, widgetId: Int) {
            val counterId = widgetCounterId(context, widgetId)
            val dao = CounterDatabase.get(context).counterDao()
            val counter = if (counterId > 0L) dao.counter(counterId) else null
            val views = RemoteViews(context.packageName, R.layout.counter_widget)
            if (counter == null || !counter.pinned) {
                views.setTextViewText(R.id.widget_label, context.getString(R.string.widget_not_pinned))
                views.setTextViewText(R.id.widget_period, context.getString(R.string.widget_update_hint))
                views.setTextViewText(R.id.widget_count, "—")
            } else {
                val now = System.currentTimeMillis()
                val count = dao.countEvents(counter.id, now - LAST_HOUR_MILLIS, now + 1)
                views.setTextViewText(R.id.widget_label, counter.label)
                views.setTextViewText(R.id.widget_period, context.getString(R.string.widget_period_past_hour))
                views.setTextViewText(R.id.widget_count, count.toString())
                views.setInt(R.id.widget_accent, "setBackgroundColor", counter.colorArgb.toInt())
                val intent = Intent(context, CounterWidgetProvider::class.java)
                    .setAction(ACTION_INCREMENT)
                    .putExtra(EXTRA_COUNTER_ID, counter.id)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    widgetId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                views.setOnClickPendingIntent(R.id.widget_increment, pendingIntent)
            }
            AppWidgetManager.getInstance(context).updateAppWidget(widgetId, views)
        }

        private fun widgetCounterId(context: Context, widgetId: Int): Long =
            context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getLong(widgetKey(widgetId), 0L)

        private fun widgetKey(widgetId: Int) = "counter_$widgetId"
    }
}