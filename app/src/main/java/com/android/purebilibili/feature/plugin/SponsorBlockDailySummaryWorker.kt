package com.android.purebilibili.feature.plugin

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.android.purebilibili.R
import com.android.purebilibili.app.SPONSOR_BLOCK_NOTIFICATION_CHANNEL_ID
import com.android.purebilibili.core.plugin.PluginStore
import com.android.purebilibili.core.util.Logger
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.util.concurrent.TimeUnit

private const val TAG = "SponsorBlockDailySummary"
private const val SPONSOR_BLOCK_DAILY_SUMMARY_WORK_NAME = "sponsor_block_daily_summary"
private const val SPONSOR_BLOCK_DAILY_SUMMARY_NOTIFICATION_ID = 0x5B10C

class SponsorBlockDailySummaryWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val config = readConfig() ?: return Result.success()
        val records = SponsorBlockInsightStore.readRecords(applicationContext)
        val notification = buildSponsorBlockDailySummaryNotification(
            config = config,
            records = records,
            dayStartMs = currentLocalDayStartMs()
        ) ?: return Result.success()
        postNotification(notification)
        return Result.success()
    }

    private suspend fun readConfig(): SponsorBlockConfig? {
        return runCatching {
            val json = PluginStore.getConfigJson(applicationContext, SPONSOR_BLOCK_PLUGIN_ID)
                ?: return SponsorBlockConfig.default()
            Json.decodeFromString<SponsorBlockConfig>(json).normalized()
        }.onFailure { error ->
            Logger.w(TAG, "读取空降助手配置失败: ${error.message}")
        }.getOrNull()
    }

    @SuppressLint("MissingPermission")
    private fun postNotification(content: SponsorBlockDailySummaryNotification) {
        val manager = NotificationManagerCompat.from(applicationContext)
        if (!manager.areNotificationsEnabled()) return
        val notification = NotificationCompat.Builder(
            applicationContext,
            SPONSOR_BLOCK_NOTIFICATION_CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()
        runCatching {
            manager.notify(SPONSOR_BLOCK_DAILY_SUMMARY_NOTIFICATION_ID, notification)
        }.onFailure { error ->
            Logger.w(TAG, "发送空降助手汇总通知失败: ${error.message}")
        }
    }
}

internal fun scheduleSponsorBlockDailySummary(
    context: Context,
    enabled: Boolean
) {
    val workManager = WorkManager.getInstance(context.applicationContext)
    if (!enabled) {
        workManager.cancelUniqueWork(SPONSOR_BLOCK_DAILY_SUMMARY_WORK_NAME)
        return
    }
    val request = PeriodicWorkRequestBuilder<SponsorBlockDailySummaryWorker>(
        repeatInterval = 1,
        repeatIntervalTimeUnit = TimeUnit.DAYS
    ).build()
    workManager.enqueueUniquePeriodicWork(
        SPONSOR_BLOCK_DAILY_SUMMARY_WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}
