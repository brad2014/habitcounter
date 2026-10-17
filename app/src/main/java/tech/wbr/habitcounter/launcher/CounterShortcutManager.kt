package tech.wbr.habitcounter.launcher

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.Icon
import androidx.core.graphics.createBitmap

object CounterShortcutManager {
    fun sync(context: Context, counterId: Long, label: String, colorArgb: Long, pinned: Boolean) {
        val manager = context.getSystemService(ShortcutManager::class.java) ?: return
        val id = shortcutId(counterId)
        if (!pinned) {
            manager.removeDynamicShortcuts(listOf(id))
            if (manager.pinnedShortcuts.any { it.id == id }) {
                manager.disableShortcuts(listOf(id), "This counter is no longer pinned.")
            }
            return
        }

        val shortcut = buildShortcut(context, id, counterId, label, colorArgb)
        if (manager.pinnedShortcuts.any { it.id == id }) {
            manager.enableShortcuts(listOf(id))
            manager.updateShortcuts(listOf(shortcut))
        } else {
            manager.addDynamicShortcuts(listOf(shortcut))
            if (manager.isRequestPinShortcutSupported) manager.requestPinShortcut(shortcut, null)
        }
    }

    fun remove(context: Context, counterId: Long) {
        sync(context, counterId, "", 0L, pinned = false)
    }

    private fun buildShortcut(
        context: Context,
        id: String,
        counterId: Long,
        label: String,
        colorArgb: Long,
    ): ShortcutInfo {
        val intent = Intent(context, CounterShortcutActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .putExtra(CounterWidgetProvider.EXTRA_COUNTER_ID, counterId)
        return ShortcutInfo.Builder(context, id)
            .setShortLabel(label.take(25).ifBlank { "Counter" })
            .setLongLabel(label.ifBlank { "Counter" })
            .setIcon(Icon.createWithBitmap(shortcutIcon(colorArgb.toInt())))
            .setIntent(intent)
            .build()
    }

    private fun shortcutId(counterId: Long) = "counter-$counterId"

    private fun shortcutIcon(color: Int): Bitmap {
        val size = 128
        val bitmap = createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = color
        canvas.drawCircle(size / 2f, size / 2f, size * 0.46f, paint)
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 54f
        paint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("+1", size / 2f, 82f, paint)
        return bitmap
    }
}