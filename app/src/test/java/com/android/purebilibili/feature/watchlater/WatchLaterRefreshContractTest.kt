package com.android.purebilibili.feature.watchlater

import com.android.purebilibili.core.refresh.WatchLaterRefreshBus
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest

class WatchLaterRefreshContractTest {
    @Test
    fun watchLaterRefreshSignal_emitsForActiveCollectors() = runTest {
        val events = Channel<Unit>(capacity = 1)
        val collector = backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            WatchLaterRefreshBus.changes.collect {
                events.send(Unit)
                cancel()
            }
        }

        WatchLaterRefreshBus.notifyChanged()

        events.receive()
        collector.cancel()
    }

    @Test
    fun actionRepository_notifiesWatchLaterRefreshAfterSuccessfulToggle() {
        val source = sourceText("src/main/java/com/android/purebilibili/data/repository/ActionRepository.kt")

        assertTrue(
            source.contains("WatchLaterRefreshBus.notifyChanged()"),
            "稍后再看接口成功切换后必须通知列表刷新"
        )
    }

    @Test
    fun watchLaterViewModel_collectsRefreshSignalAndReloadsList() {
        val source = sourceText("src/main/java/com/android/purebilibili/feature/watchlater/WatchLaterScreen.kt")

        assertTrue(
            source.contains("WatchLaterRefreshBus.changes.collect"),
            "稍后再看列表必须订阅刷新信号"
        )
        assertTrue(
            source.contains("loadData()"),
            "收到刷新信号后必须重新加载列表"
        )
    }

    private fun sourceText(path: String): String {
        val sourceFile = listOf(
            File(path),
            File("app/$path")
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
