package tech.wbr.habitcounter.launcher

import android.os.Bundle
import android.content.pm.ShortcutManager
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import tech.wbr.habitcounter.data.CounterDatabase
import tech.wbr.habitcounter.data.CounterEvent
import kotlinx.coroutines.launch

class CounterShortcutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val counterId = intent.getLongExtra(CounterShortcutManager.EXTRA_COUNTER_ID, 0L)
        lifecycleScope.launch {
            if (counterId > 0L) {
                getSystemService(ShortcutManager::class.java)?.reportShortcutUsed("counter-$counterId")
                CounterDatabase.get(this@CounterShortcutActivity).counterDao().insertEvent(
                    CounterEvent(counterId = counterId, occurredAtMillis = System.currentTimeMillis()),
                )
                CounterShortcutManager.refresh(this@CounterShortcutActivity, counterId)
            }
            finish()
        }
    }
}