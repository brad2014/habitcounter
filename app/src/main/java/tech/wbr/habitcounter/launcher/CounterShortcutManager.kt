package tech.wbr.habitcounter.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.Icon
import androidx.core.graphics.createBitmap
import tech.wbr.habitcounter.data.CounterDatabase
import tech.wbr.habitcounter.data.CounterRecord

object CounterShortcutManager {
    const val EXTRA_COUNTER_ID = "tech.wbr.habitcounter.extra.COUNTER_ID"
    private const val LAST_HOUR_MILLIS = 60 * 60 * 1000L
    private const val PRIMARY_BLUE = 0xFF2563EB.toInt()

    /** Re-reads the counter and publishes, updates or disables its launcher shortcut. */
    suspend fun refresh(context: Context, counterId: Long, requestPin: Boolean = false) {
        val manager = context.getSystemService(ShortcutManager::class.java) ?: return
        val id = shortcutId(counterId)
        val dao = CounterDatabase.get(context).counterDao()
        val counter = dao.counter(counterId)
        val isPinnedOnLauncher = manager.pinnedShortcuts.any { it.id == id }
        if (counter == null || !counter.pinned) {
            manager.removeDynamicShortcuts(listOf(id))
            if (isPinnedOnLauncher) manager.disableShortcuts(listOf(id), "This counter is no longer pinned.")
            return
        }

        val now = System.currentTimeMillis()
        val count = dao.countEvents(counterId, now - LAST_HOUR_MILLIS, now + 1)
        val shortcut = buildShortcut(context, id, counter, count)
        val isDynamic = manager.dynamicShortcuts.any { it.id == id }
        if (isDynamic || isPinnedOnLauncher) {
            manager.enableShortcuts(listOf(id))
            manager.updateShortcuts(listOf(shortcut))
        }
        if (!isDynamic && manager.dynamicShortcuts.size < manager.maxShortcutCountPerActivity) {
            manager.addDynamicShortcuts(listOf(shortcut))
        }
        if (requestPin && !isPinnedOnLauncher && manager.isRequestPinShortcutSupported) {
            manager.requestPinShortcut(shortcut, null)
        }
    }

    private fun buildShortcut(context: Context, id: String, counter: CounterRecord, count: Long): ShortcutInfo {
        val label = counter.label.trim()
        val intent = Intent(context, CounterShortcutActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .putExtra(EXTRA_COUNTER_ID, counter.id)
        return ShortcutInfo.Builder(context, id)
            .setShortLabel(label.take(25).ifBlank { "Counter" })
            .setLongLabel(label.ifBlank { "Counter" })
            .setIcon(Icon.createWithAdaptiveBitmap(shortcutIcon(counter.colorArgb.toInt(), count)))
            .setIntent(intent)
            .build()
    }

    private fun shortcutId(counterId: Long) = "counter-$counterId"

    // Drawn in a 108dp adaptive-icon grid; content stays inside the 66dp safe area (21..87).
    private fun shortcutIcon(color: Int, count: Long): Bitmap {
        val px = 432
        val bitmap = createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.scale(px / 108f, px / 108f)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, 108f, 108f, paint)
        paint.color = color
        canvas.drawRect(0f, 54f, 108f, 108f, paint)

        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        val text = if (count > 999) "999+" else count.toString()
        paint.color = PRIMARY_BLUE
        drawFitted(canvas, paint, text, RectF(36f, 29f, 72f, 51f))
        return bitmap
    }

    private fun drawFitted(canvas: Canvas, paint: Paint, text: String, area: RectF) {
        val bounds = Rect()
        paint.textSize = 100f
        paint.getTextBounds(text, 0, text.length, bounds)
        paint.textSize = 100f * minOf(area.width() / bounds.width(), area.height() / bounds.height())
        paint.getTextBounds(text, 0, text.length, bounds)
        canvas.drawText(text, area.centerX() - bounds.exactCenterX(), area.centerY() - bounds.exactCenterY(), paint)
    }
}