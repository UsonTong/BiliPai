package com.android.purebilibili.data.repository

import com.android.purebilibili.core.network.NetworkModule
import com.android.purebilibili.core.network.WbiUtils
import com.android.purebilibili.data.model.response.HotItem
import com.android.purebilibili.data.model.response.VideoItem
import com.android.purebilibili.data.model.response.SearchUpItem
import com.android.purebilibili.data.model.response.LiveRoomSearchItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.JsonElement

object SearchRepository {
    private val api = NetworkModule.searchApi
    private val navApi = NetworkModule.api
    
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    //  [新增] 搜索分页信息
    data class SearchPageInfo(
        val currentPage: Int,
        val totalPages: Int,
        val totalResults: Int,
        val hasMore: Boolean
    )

    //  视频搜索 - 支持排序、时长过滤和分页
    suspend fun search(
        keyword: String,
        order: SearchOrder = SearchOrder.TOTALRANK,
        duration: SearchDuration = SearchDuration.ALL,
        page: Int = 1  // [新增] 页码参数
    ): Result<Pair<List<VideoItem>, SearchPageInfo>> = withContext(Dispatchers.IO) {
        try {
            val navResp = navApi.getNavInfo()
            val wbiImg = navResp.data?.wbi_img
            val imgKey = wbiImg?.img_url?.substringAfterLast("/")?.substringBefore(".") ?: ""
            val subKey = wbiImg?.sub_url?.substringAfterLast("/")?.substringBefore(".") ?: ""

            //  [修复] 使用 search/type API 的正确参数格式
            val params = mutableMapOf(
                "keyword" to keyword,
                "search_type" to "video",  // 搜索类型
                "order" to order.value,     // 排序方式
                "duration" to duration.value.toString(),  // 时长筛选
                "page" to page.toString(),  // [修改] 使用传入的页码
                "pagesize" to "30"          // 每页数量
            )
            
            //  调试日志 - 检查搜索参数
            com.android.purebilibili.core.util.Logger.d("SearchRepo", "🔍 Search params BEFORE sign: keyword=$keyword, order=${order.value}, duration=${duration.value}, page=$page")
            
            val signedParams = if (imgKey.isNotEmpty()) WbiUtils.sign(params, imgKey, subKey) else params
            
            //  调试日志 - 检查签名后的参数
            com.android.purebilibili.core.util.Logger.d("SearchRepo", "🔍 Search params AFTER sign: $signedParams")

            val response = api.search(signedParams)
            
            //  [修复] search/type API 直接返回 result 列表，不需要查找 result_type
            val videoList = response.data?.result
                ?.map { it.toVideoItem() }
                ?: emptyList()
            
            //  [新增] 提取分页信息
            val pageInfo = SearchPageInfo(
                currentPage = response.data?.page ?: page,
                totalPages = response.data?.numPages ?: 1,
                totalResults = response.data?.numResults ?: videoList.size,
                hasMore = (response.data?.page ?: page) < (response.data?.numPages ?: 1)
            )
            
            com.android.purebilibili.core.util.Logger.d("SearchRepo", "🔍 Search result: ${videoList.size} videos found, page ${pageInfo.currentPage}/${pageInfo.totalPages}, hasMore=${pageInfo.hasMore}")

            Result.success(Pair(videoList, pageInfo))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    //  UP主 搜索
    suspend fun searchUp(keyword: String): Result<List<SearchUpItem>> = withContext(Dispatchers.IO) {
        try {
            val navResp = navApi.getNavInfo()
            val wbiImg = navResp.data?.wbi_img
            val imgKey = wbiImg?.img_url?.substringAfterLast("/")?.substringBefore(".") ?: ""
            val subKey = wbiImg?.sub_url?.substringAfterLast("/")?.substringBefore(".") ?: ""

            //  [修复] 使用 search/type API，search_type = bili_user
            val params = mapOf(
                "keyword" to keyword,
                "search_type" to "bili_user",  // UP主搜索类型
                "page" to "1",
                "pagesize" to "30"
            )
            val signedParams = if (imgKey.isNotEmpty()) WbiUtils.sign(params, imgKey, subKey) else params

            com.android.purebilibili.core.util.Logger.d("SearchRepo", "🔍 UP Search params: $signedParams")

            val response = api.searchUp(signedParams)
            
            //  直接从 response.data.result 获取 UP 主列表
            val upList = response.data?.result
                ?.map { it.cleanupFields() }
                ?: emptyList()
            
            com.android.purebilibili.core.util.Logger.d("SearchRepo", "🔍 UP Search result: ${upList.size} UPs found")

            Result.success(upList)
        } catch (e: Exception) {
            e.printStackTrace()
            com.android.purebilibili.core.util.Logger.e("SearchRepo", "UP Search failed", e)
            Result.failure(e)
        }
    }

    //  热搜
    suspend fun getHotSearch(): Result<List<HotItem>> = withContext(Dispatchers.IO) {
        try {
            val response = api.getHotSearch()
            val list = response.data?.trending?.list ?: emptyList()
            Result.success(list)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    //  [新增] 番剧搜索
    suspend fun searchBangumi(
        keyword: String,
        page: Int = 1
    ): Result<Pair<List<com.android.purebilibili.data.model.response.BangumiSearchItem>, SearchPageInfo>> = withContext(Dispatchers.IO) {
        try {
            val navResp = navApi.getNavInfo()
            val wbiImg = navResp.data?.wbi_img
            val imgKey = wbiImg?.img_url?.substringAfterLast("/")?.substringBefore(".") ?: ""
            val subKey = wbiImg?.sub_url?.substringAfterLast("/")?.substringBefore(".") ?: ""

            val params = mutableMapOf(
                "keyword" to keyword,
                "search_type" to "media_bangumi",
                "page" to page.toString()
            )
            
            val signedParams = if (imgKey.isNotEmpty()) WbiUtils.sign(params, imgKey, subKey) else params

            val response = api.searchBangumi(signedParams)
            
            val bangumiList = response.data?.result?.map { item ->
                // 清理 HTML 标签
                item.copy(
                    title = item.title.replace(Regex("<.*?>"), ""),
                    cover = if (item.cover.startsWith("//")) "https:${item.cover}" else item.cover
                )
            } ?: emptyList()
            
            val pageInfo = SearchPageInfo(
                currentPage = response.data?.page ?: page,
                totalPages = response.data?.numPages ?: 1,
                totalResults = response.data?.numResults ?: bangumiList.size,
                hasMore = (response.data?.page ?: page) < (response.data?.numPages ?: 1)
            )
            
            com.android.purebilibili.core.util.Logger.d("SearchRepo", "🔍 Bangumi search result: ${bangumiList.size} items, page ${pageInfo.currentPage}/${pageInfo.totalPages}")

            Result.success(Pair(bangumiList, pageInfo))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    //  [新增] 直播搜索
    suspend fun searchLive(
        keyword: String,
        page: Int = 1
    ): Result<Pair<List<LiveRoomSearchItem>, SearchPageInfo>> = withContext(Dispatchers.IO) {
        try {
            val navResp = navApi.getNavInfo()
            val wbiImg = navResp.data?.wbi_img
            val imgKey = wbiImg?.img_url?.substringAfterLast("/")?.substringBefore(".") ?: ""
            val subKey = wbiImg?.sub_url?.substringAfterLast("/")?.substringBefore(".") ?: ""

            val params = mutableMapOf(
                "keyword" to keyword,
                "search_type" to "live_room",
                "page" to page.toString()
            )
            
            val signedParams = if (imgKey.isNotEmpty()) WbiUtils.sign(params, imgKey, subKey) else params

            val response = api.searchLive(signedParams)
            
            val liveList = response.data?.result?.map { it.cleanupFields() } ?: emptyList()
            
            val pageInfo = SearchPageInfo(
                currentPage = response.data?.page ?: page,
                totalPages = response.data?.numPages ?: 1,
                totalResults = response.data?.numResults ?: liveList.size,
                hasMore = (response.data?.page ?: page) < (response.data?.numPages ?: 1)
            )
            
            com.android.purebilibili.core.util.Logger.d("SearchRepo", "🔍 Live search result: ${liveList.size} rooms, page ${pageInfo.currentPage}/${pageInfo.totalPages}")

            Result.success(Pair(liveList, pageInfo))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    //  搜索建议/联想
    suspend fun getSuggest(keyword: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            if (keyword.isBlank()) return@withContext Result.success(emptyList())
            
            val response = api.getSearchSuggest(keyword)
            val suggestions = response.result?.tag?.map { it.value } ?: emptyList()
            Result.success(suggestions)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    //  获取搜索发现 (个性化 + 官方热搜兜底)
    suspend fun getSearchDiscover(historyKeywords: List<String>): Result<Pair<String, List<String>>> = withContext(Dispatchers.IO) {
        try {
            // 1. 个性化推荐：尝试使用最近的搜索词进行联想
            if (historyKeywords.isNotEmpty()) {
                val lastKeyword = historyKeywords.firstOrNull()
                if (!lastKeyword.isNullOrBlank()) {
                    val response = api.getSearchSuggest(lastKeyword)
                    val suggestions = response.result?.tag?.map { it.value }?.filter { it != lastKeyword }?.take(10)
                    
                    if (!suggestions.isNullOrEmpty()) {
                        return@withContext Result.success("大家都在搜 \"$lastKeyword\" 相关" to suggestions)
                    }
                }
            }
            
            // 2. 官方推荐：使用热搜词乱序 (模拟官方推荐流)
            val hotResponse = api.getHotSearch()
            val hotList = hotResponse.data?.trending?.list?.map { it.show_name }?.shuffled()?.take(10) ?: emptyList()
            
            if (hotList.isNotEmpty()) {
                return@withContext Result.success(" 热门推荐" to hotList)
            }
            
            // 3. 静态兜底
            Result.success("搜索发现" to listOf("黑神话悟空", "原神", "初音未来", "JOJO", "罗翔说刑法", "何同学", "毕业季", "猫咪", "我的世界", "战鹰"))
        } catch (e: Exception) {
            e.printStackTrace()
            // 发生异常时的最后兜底
            Result.success("搜索发现" to listOf("黑神话悟空", "原神", "初音未来", "JOJO", "罗翔说刑法", "何同学", "毕业季", "猫咪", "我的世界", "战鹰"))
        }
    }
}

//  搜索排序选项
enum class SearchOrder(val value: String, val displayName: String) {
    TOTALRANK("totalrank", "综合排序"),
    PUBDATE("pubdate", "最新发布"),
    CLICK("click", "播放最多"),
    DM("dm", "弹幕最多"),
    STOW("stow", "收藏最多")
}

//  搜索时长筛选
enum class SearchDuration(val value: Int, val displayName: String) {
    ALL(0, "全部时长"),
    UNDER_10MIN(1, "10分钟以下"),
    TEN_TO_30MIN(2, "10-30分钟"),
    THIRTY_TO_60MIN(3, "30-60分钟"),
    OVER_60MIN(4, "60分钟以上")
}