package com.android.purebilibili.data.repository

import android.content.Context
import com.android.purebilibili.core.database.AppDatabase
import com.android.purebilibili.core.database.entity.BlockedUp
import kotlinx.coroutines.flow.Flow

data class BlockedUpImportItem(
    val mid: Long,
    val name: String = "",
    val face: String = ""
)

data class BlockedUpImportResult(
    val importedCount: Int,
    val existingCount: Int,
    val failedCount: Int,
    val message: String
)

internal data class BlockedUpImportPlan(
    val itemsToInsert: List<BlockedUpImportItem>,
    val existingCount: Int,
    val failedCount: Int
)

internal fun buildBlockedUpImportPlan(
    existingMids: Set<Long>,
    items: List<BlockedUpImportItem>
): BlockedUpImportPlan {
    var existingCount = 0
    var failedCount = 0
    val seenMids = mutableSetOf<Long>()
    val itemsToInsert = mutableListOf<BlockedUpImportItem>()

    items.forEach { item ->
        val mid = item.mid
        if (mid <= 0L || !seenMids.add(mid)) {
            failedCount += 1
            return@forEach
        }
        if (mid in existingMids) {
            existingCount += 1
            return@forEach
        }
        itemsToInsert += item.copy(
            name = item.name.ifBlank { "UP主$mid" }
        )
    }

    return BlockedUpImportPlan(
        itemsToInsert = itemsToInsert,
        existingCount = existingCount,
        failedCount = failedCount
    )
}

internal fun buildBlockedUpImportMessage(
    importedCount: Int,
    existingCount: Int,
    failedCount: Int
): String {
    return when {
        importedCount > 0 -> "已导入 $importedCount 个 B站黑名单用户，$existingCount 个已存在"
        existingCount > 0 && failedCount == 0 -> "B站黑名单已同步，$existingCount 个用户已在本地黑名单中"
        failedCount > 0 -> "导入完成，$importedCount 个新增，$existingCount 个已存在，$failedCount 个无效条目已跳过"
        else -> "没有可导入的 B站黑名单用户"
    }
}

class BlockedUpRepository(context: Context) {
    private val blockedUpDao = AppDatabase.getDatabase(context).blockedUpDao()

    fun getAllBlockedUps(): Flow<List<BlockedUp>> = blockedUpDao.getAllBlockedUps()

    fun isBlocked(mid: Long): Flow<Boolean> = blockedUpDao.isBlocked(mid)

    suspend fun blockUp(mid: Long, name: String, face: String) {
        val entity = BlockedUp(mid = mid, name = name, face = face)
        blockedUpDao.insert(entity)
    }

    suspend fun importBlockedUps(items: List<BlockedUpImportItem>): BlockedUpImportResult {
        val candidateMids = items.map { it.mid }.filter { it > 0L }.distinct()
        val existingMids = candidateMids
            .filter { blockedUpDao.getBlockedUp(it) != null }
            .toSet()
        val plan = buildBlockedUpImportPlan(existingMids = existingMids, items = items)

        plan.itemsToInsert.forEach { item ->
            blockedUpDao.insert(
                BlockedUp(
                    mid = item.mid,
                    name = item.name,
                    face = item.face
                )
            )
        }

        val importedCount = plan.itemsToInsert.size
        return BlockedUpImportResult(
            importedCount = importedCount,
            existingCount = plan.existingCount,
            failedCount = plan.failedCount,
            message = buildBlockedUpImportMessage(
                importedCount = importedCount,
                existingCount = plan.existingCount,
                failedCount = plan.failedCount
            )
        )
    }

    suspend fun unblockUp(mid: Long) {
        blockedUpDao.delete(mid)
    }
}
