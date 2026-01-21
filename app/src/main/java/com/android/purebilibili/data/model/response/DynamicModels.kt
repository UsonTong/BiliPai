// 文件路径: data/model/response/DynamicModels.kt
package com.android.purebilibili.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 *  动态页面数据模型
 * API: x/polymer/web-dynamic/v1/feed/all
 */

// --- 顶层响应 ---
@Serializable
data class DynamicFeedResponse(
    val code: Int = 0,
    val message: String = "",
    val data: DynamicFeedData? = null
)

@Serializable
data class DynamicFeedData(
    val items: List<DynamicItem> = emptyList(),
    val offset: String = "", // 分页偏移量
    val has_more: Boolean = false,
    val update_baseline: String = "",
    val update_num: Int = 0
)

// --- 动态卡片 ---
@Serializable
data class DynamicItem(
    val id_str: String = "",
    val type: String = "", // DYNAMIC_TYPE_AV, DYNAMIC_TYPE_DRAW, DYNAMIC_TYPE_WORD, DYNAMIC_TYPE_FORWARD
    val visible: Boolean = true,
    val modules: DynamicModules = DynamicModules(),
    val orig: DynamicItem? = null  //  转发动态的原始内容
)

// --- 动态模块集合 ---
@Serializable
data class DynamicModules(
    val module_author: DynamicAuthorModule? = null,
    val module_dynamic: DynamicContentModule? = null,
    val module_stat: DynamicStatModule? = null
)

// --- 作者模块 ---
@Serializable
data class DynamicAuthorModule(
    val mid: Long = 0,
    val name: String = "",
    val face: String = "",
    val pub_time: String = "", // "昨天 18:00"
    val pub_ts: Long = 0, // 时间戳
    val following: Boolean? = null,
    val official_verify: DynamicOfficialVerify? = null,
    val vip: DynamicVipInfo? = null,
    val decorate: DecorateInfo? = null
)

@Serializable
data class DynamicOfficialVerify(
    val type: Int = -1, // 0: 个人认证, 1: 机构认证, -1: 无
    val desc: String = ""
)

@Serializable
data class DynamicVipInfo(
    val type: Int = 0, // 0: 无, 1: 月度, 2: 年度
    val status: Int = 0,
    val nickname_color: String = "" // "#FB7299"
)

@Serializable
data class DecorateInfo(
    val card_url: String = "", // 装扮卡片 URL
    val name: String = ""
)

// --- 内容模块 ---
@Serializable
data class DynamicContentModule(
    val desc: DynamicDesc? = null,
    val major: DynamicMajor? = null
)

@Serializable
data class DynamicDesc(
    val text: String = "", // 动态文字内容
    val rich_text_nodes: List<RichTextNode> = emptyList()
)

@Serializable
data class RichTextNode(
    val type: String = "", // TEXT, EMOJI, AT, TOPIC
    val text: String = "",
    val emoji: EmojiInfo? = null,
    val jump_url: String? = null
)

@Serializable
data class EmojiInfo(
    val icon_url: String = "",
    val size: Int = 1,
    val text: String = ""
)

// --- 主要内容 (视频/图片/直播/图文) ---
@Serializable
data class DynamicMajor(
    val type: String = "", // MAJOR_TYPE_ARCHIVE, MAJOR_TYPE_DRAW, MAJOR_TYPE_LIVE_RCMD, MAJOR_TYPE_OPUS, MAJOR_TYPE_NONE
    val archive: ArchiveMajor? = null, // 视频
    val draw: DrawMajor? = null, // 图片
    val live_rcmd: LiveRcmdMajor? = null, //  直播
    val opus: OpusMajor? = null //  [新增] 图文动态 (新版格式)
)

//  [新增] 图文动态 (MAJOR_TYPE_OPUS) - B站新版图文格式
@Serializable
data class OpusMajor(
    val jump_url: String = "",
    val pics: List<OpusPic> = emptyList(), // 图片列表
    val summary: OpusSummary? = null, // 文字摘要
    val title: String? = null // 标题 (可选)
)

@Serializable
data class OpusPic(
    val url: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val size: Double = 0.0
)

@Serializable
data class OpusSummary(
    val text: String = "",
    val rich_text_nodes: List<RichTextNode> = emptyList()
)

//  直播推荐
@Serializable
data class LiveRcmdMajor(
    val content: String = "" // JSON string，需要解析
)

@Serializable
data class ArchiveMajor(
    val aid: String = "",
    val bvid: String = "",
    val title: String = "",
    val cover: String = "",
    val desc: String = "",
    val duration_text: String = "", // "10:24"
    val stat: ArchiveStat = ArchiveStat(),
    val jump_url: String = ""
)

@Serializable
data class ArchiveStat(
    val play: String = "0", // "123.4万"
    val danmaku: String = "0"
)

@Serializable
data class DrawMajor(
    val id: Long = 0,
    val items: List<DrawItem> = emptyList()
)

@Serializable
data class DrawItem(
    val src: String = "", // 图片 URL
    val width: Int = 0,
    val height: Int = 0
)

// --- 统计模块 ---
@Serializable
data class DynamicStatModule(
    val comment: StatItem = StatItem(),
    val forward: StatItem = StatItem(),
    val like: StatItem = StatItem()
)

@Serializable
data class StatItem(
    val count: Int = 0,
    val forbidden: Boolean = false
)

// --- 动态类型枚举 ---
enum class DynamicType(val apiValue: String) {
    VIDEO("DYNAMIC_TYPE_AV"),
    DRAW("DYNAMIC_TYPE_DRAW"),
    WORD("DYNAMIC_TYPE_WORD"),
    FORWARD("DYNAMIC_TYPE_FORWARD"),
    LIVE("DYNAMIC_TYPE_LIVE_RCMD"),
    OPUS("DYNAMIC_TYPE_DRAW"),  //  [新增] 图文动态 (使用 DRAW 类型，但 major 为 opus)
    UNKNOWN("UNKNOWN");
    
    companion object {
        fun fromApiValue(value: String): DynamicType {
            return entries.find { it.apiValue == value } ?: UNKNOWN
        }
    }
}
