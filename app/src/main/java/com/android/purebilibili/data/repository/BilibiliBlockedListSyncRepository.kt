package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.BilibiliApi
import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.store.TokenManager
import com.android.purebilibili.data.model.response.RelationTagItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val BLOCKED_LIST_PAGE_SIZE = 100
private const val BLOCKED_LIST_MAX_PAGES = 120
private const val BLOCKED_LIST_PAGE_DELAY_MS = 220L

class BilibiliBlockedListSyncRepository(
    private val localRepository: BlockedUpRepository,
    private val api: BilibiliApi = NetworkModule.api
) {
    suspend fun importFromBilibili(): Result<BlockedUpImportResult> = withContext(Dispatchers.IO) {
        if (TokenManager.sessDataCache.isNullOrEmpty()) {
            return@withContext Result.failure(Exception("请先登录后再同步 B站黑名单"))
        }

        try {
            val tagsResponse = api.getRelationTags()
            if (tagsResponse.code != 0) {
                return@withContext Result.failure(
                    Exception(tagsResponse.message.ifBlank { "获取 B站关系分组失败: ${tagsResponse.code}" })
                )
            }

            val blockedTag = findBilibiliBlockedListTag(tagsResponse.data)
                ?: return@withContext Result.failure(Exception("当前账号接口未返回黑名单分组"))
            val remoteMids = fetchBlockedListMids(blockedTag.tagid)
            val importItems = remoteMids.map { mid ->
                BlockedUpImportItem(mid = mid, name = "UP主$mid", face = "")
            }

            Result.success(localRepository.importBlockedUps(importItems))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun fetchBlockedListMids(tagId: Long): List<Long> {
        val result = linkedSetOf<Long>()
        var page = 1

        while (page <= BLOCKED_LIST_MAX_PAGES) {
            val response = api.getRelationTagMembers(
                tagId = tagId,
                pageSize = BLOCKED_LIST_PAGE_SIZE,
                page = page
            )
            if (response.code != 0) {
                throw Exception(response.message.ifBlank { "获取 B站黑名单成员失败: ${response.code}" })
            }

            val mids = response.data
                .asSequence()
                .map { it.mid }
                .filter { it > 0L }
                .toList()

            result.addAll(mids)
            if (mids.size < BLOCKED_LIST_PAGE_SIZE) break
            page += 1
            delay(BLOCKED_LIST_PAGE_DELAY_MS)
        }

        return result.toList()
    }
}

internal fun findBilibiliBlockedListTag(tags: List<RelationTagItem>): RelationTagItem? {
    return tags.firstOrNull { tag ->
        val normalizedName = tag.name.normalizeRelationTagText()
        val normalizedTip = tag.tip.normalizeRelationTagText()
        normalizedName.contains("黑名单") ||
            normalizedName.contains("拉黑") ||
            normalizedTip.contains("黑名单") ||
            normalizedTip.contains("拉黑")
    }
}

private fun String.normalizeRelationTagText(): String {
    return lowercase()
        .replace(Regex("\\s+"), "")
        .replace("_", "")
        .replace("-", "")
}
