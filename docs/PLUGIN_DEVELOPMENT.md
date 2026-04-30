# 🔌 BiliPai 插件开发指南

本文档面向想要为 BiliPai 创建自定义插件的开发者。BiliPai 提供了一个灵活的插件系统，当前主要支持三种开发路径：

| 类型 | 难度 | 适用场景 |
|------|------|----------|
| **JSON 规则插件** | ⭐ 简单 | 内容过滤、弹幕净化、关键词屏蔽 |
| **外部 `.bpplugin` Kotlin 包** | ⭐⭐ 预览 | 推荐算法、播放器/弹幕接口适配、能力授权流程验证 |
| **源码级原生 Kotlin 插件** | ⭐⭐⭐ 进阶 | 复杂功能、API 集成、自定义 UI、立即运行的深度集成 |

> [!CAUTION]
> 当前仓库已内置 5 个内置插件，并支持通过 URL 导入外部 JSON 规则插件；但插件生态仍处于早期阶段。
> `plugins/community/` 目前仅包含 1 个演示插件，社区规模和兼容性样本都还有限。
> 外部 `.bpplugin` Kotlin 包当前支持预览、签名/哈希展示和能力授权记录，宿主尚不执行外部 Dex。
> 引入第三方插件前请自行审阅规则内容、验证兼容性，并假设规则能力与导入体验会继续随版本迭代。

---

## 📋 目录

- [JSON 规则插件（推荐入门）](#-json-规则插件推荐入门)
  - [快速开始](#快速开始)
  - [插件结构](#插件结构)
  - [字段参考](#字段参考)
  - [操作符大全](#操作符大全)
  - [示例插件](#示例插件)
- [外部 `.bpplugin` Kotlin 包（预览）](#-外部-bpplugin-kotlin-包预览)
  - [开发步骤](#开发步骤)
  - [推荐插件最小示例](#推荐插件最小示例)
  - [打包为 `.bpplugin`](#打包为-bpplugin)
- [源码级原生 Kotlin 插件](#-源码级原生-kotlin-插件)
  - [插件接口](#插件接口)
  - [插件类型](#插件类型)
- [安装与分发](#-安装与分发)
- [常见问题](#-常见问题)

---

## 📝 JSON 规则插件（推荐入门）

JSON 规则插件是最简单的插件形式，只需编写一个 JSON 文件即可实现内容过滤功能。无需编程基础！

### 快速开始

1. 创建一个 `.json` 文件
2. 按照下面的格式编写规则
3. 上传到任意公开可访问的 URL（如 GitHub Gist、Cloudflare R2）
4. 在 BiliPai 中通过 **设置 → 插件中心 → 导入外部插件** 安装

### 插件结构

```json
{
    "id": "my_plugin",           // 唯一标识符（英文、下划线）
    "name": "我的插件",           // 显示名称
    "description": "插件描述",    // 简短描述
    "version": "1.0.0",          // 版本号
    "author": "你的名字",         // 作者
    "type": "feed",              // 插件类型: "feed" 或 "danmaku"
    "rules": [                   // 规则数组
        {
            "field": "title",    // 匹配字段
            "op": "contains",    // 操作符
            "value": "广告",      // 匹配值
            "action": "hide"     // 动作
        }
    ]
}
```

### 字段参考

#### Feed 插件（推荐流过滤）

| 字段 | 说明 | 示例值 |
|------|------|--------|
| `title` | 视频标题 | `"震惊"` |
| `duration` | 视频时长（秒） | `60` |
| `owner.mid` | UP 主 UID | `12345678` |
| `owner.name` | UP 主名称 | `"某UP主"` |
| `stat.view` | 播放量 | `100000` |
| `stat.like` | 点赞数 | `5000` |

#### Danmaku 插件（弹幕过滤）

| 字段 | 说明 | 示例值 |
|------|------|--------|
| `content` | 弹幕内容 | `"666"` |
| `userId` | 发送者 UID | `12345678` |
| `type` | 弹幕类型 | `1` |

### 操作符大全

| 操作符 | 说明 | 示例 |
|--------|------|------|
| `eq` | 等于 | `"op": "eq", "value": 60` |
| `ne` | 不等于 | `"op": "ne", "value": 0` |
| `lt` | 小于 | `"op": "lt", "value": 60` |
| `le` | 小于等于 | `"op": "le", "value": 60` |
| `gt` | 大于 | `"op": "gt", "value": 100000` |
| `ge` | 大于等于 | `"op": "ge", "value": 100000` |
| `contains` | 包含 | `"op": "contains", "value": "广告"` |
| `startsWith` | 以...开头 | `"op": "startsWith", "value": "【"` |
| `endsWith` | 以...结尾 | `"op": "endsWith", "value": "】"` |
| `regex` | 正则匹配 | `"op": "regex", "value": "^[哈]{5,}$"` |
| `in` | 在列表中 | `"op": "in", "value": [123, 456]` |

### 动作类型

| 动作 | 说明 | 可选参数 |
|------|------|----------|
| `hide` | 隐藏匹配内容 | 无 |
| `highlight` | 高亮显示（仅弹幕） | `style` 对象 |

#### 高亮样式

```json
{
    "action": "highlight",
    "style": {
        "color": "#FFD700",    // 十六进制颜色
        "bold": true,          // 粗体
        "scale": 1.2           // 缩放比例
    }
}
```

### 🆕 复合条件（AND/OR）

从 v3.2.0 开始，支持使用 `and` 和 `or` 组合多个条件实现更精确的过滤。

#### AND 条件

所有子条件**都必须满足**时才触发动作：

```json
{
    "condition": {
        "and": [
            { "field": "duration", "op": "lt", "value": 60 },
            { "field": "title", "op": "contains", "value": "搬运" }
        ]
    },
    "action": "hide"
}
```

#### OR 条件

**任一**子条件满足时即触发动作：

```json
{
    "condition": {
        "or": [
            { "field": "owner.name", "op": "contains", "value": "营销号" },
            { "field": "title", "op": "regex", "value": "震惊.*必看" }
        ]
    },
    "action": "hide"
}
```

#### 嵌套条件

支持 AND/OR 嵌套实现复杂逻辑：

```json
{
    "condition": {
        "and": [
            { "field": "stat.view", "op": "lt", "value": 1000 },
            {
                "or": [
                    { "field": "title", "op": "contains", "value": "广告" },
                    { "field": "title", "op": "contains", "value": "推广" }
                ]
            }
        ]
    },
    "action": "hide"
}
```

> 💡 **向后兼容**：旧格式 `field/op/value` 仍然有效，无需修改现有插件。

### 示例插件

#### 1️⃣ 短视频过滤器

过滤时长小于 60 秒的短视频：

```json
{
    "id": "short_video_filter",
    "name": "短视频过滤",
    "description": "隐藏时长小于60秒的视频",
    "version": "1.0.0",
    "author": "BiliPai",
    "type": "feed",
    "rules": [
        {
            "field": "duration",
            "op": "lt",
            "value": 60,
            "action": "hide"
        }
    ]
}
```

#### 2️⃣ 标题关键词过滤

过滤标题党视频：

```json
{
    "id": "keyword_filter",
    "name": "标题关键词过滤",
    "description": "过滤包含指定关键词的视频",
    "version": "1.0.0",
    "author": "BiliPai",
    "type": "feed",
    "rules": [
        {
            "field": "title",
            "op": "contains",
            "value": "广告",
            "action": "hide"
        },
        {
            "field": "title",
            "op": "regex",
            "value": "震惊.*必看",
            "action": "hide"
        }
    ]
}
```

#### 3️⃣ 弹幕净化器

过滤刷屏弹幕，高亮同传翻译：

```json
{
    "id": "danmaku_cleaner",
    "name": "弹幕净化",
    "description": "过滤刷屏弹幕，高亮同传翻译",
    "version": "1.0.0",
    "author": "BiliPai",
    "type": "danmaku",
    "rules": [
        {
            "field": "content",
            "op": "regex",
            "value": "^[哈]{5,}$",
            "action": "hide"
        },
        {
            "field": "content",
            "op": "startsWith",
            "value": "【",
            "action": "highlight",
            "style": {
                "color": "#FFD700",
                "bold": true
            }
        }
    ]
}
```

---

## 📦 外部 `.bpplugin` Kotlin 包（预览）

外部 `.bpplugin` 是面向未来插件生态的 Kotlin 包格式。它适合提前开发推荐算法、播放器和弹幕接口适配，并验证插件包预览、签名展示和能力授权流程。

> [!IMPORTANT]
> 当前宿主只解析和保存 `.bpplugin`，不会执行外部 Dex。如果你需要插件立即影响应用行为，请使用[源码级原生 Kotlin 插件](#-源码级原生-kotlin-插件)。

### 开发步骤

1. 创建 Android Library 插件工程。
2. 添加 SDK 依赖：

```kotlin
repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.jay3-yy.BiliPai:plugin-sdk:<tag-or-commit>")
}
```

3. 选择接口：
   - `RecommendationPluginApi`：推荐队列排序和分组。
   - `PlayerPluginApi`：播放器状态读取、跳过片段、跳转控制等预留接口。
   - `DanmakuPluginApi`：弹幕过滤和样式化预留接口。
4. 实现入口类，并声明 `PluginCapabilityManifest`。
5. 在插件包根目录创建 `plugin-manifest.json`，字段与代码里的 `capabilityManifest` 保持一致。
6. 编译插件模块，生成 `classes.jar`。
7. 将 `plugin-manifest.json`、可选 `plugin-signature.json`、`classes.jar` 打包为 `.bpplugin`。
8. 在 BiliPai 插件中心选择 `.bpplugin`，检查 manifest、SHA-256、签名状态和能力授权提示。

完整 SDK 中文说明见：[Plugin SDK](../plugins/sdk/README.md)。

### Manifest 示例

`.bpplugin` 根目录必须包含 `plugin-manifest.json`：

```json
{
  "pluginId": "dev.example.today_watch_remix",
  "displayName": "Today Watch Remix",
  "version": "1.0.0",
  "apiVersion": 1,
  "entryClassName": "dev.example.todaywatchremix.TodayWatchRemixPlugin",
  "capabilities": [
    "RECOMMENDATION_CANDIDATES",
    "LOCAL_HISTORY_READ"
  ]
}
```

| 字段 | 说明 |
| --- | --- |
| `pluginId` | 全局唯一 ID，建议使用反向域名 |
| `displayName` | 插件中心展示名称 |
| `version` | 插件版本 |
| `apiVersion` | SDK API 版本，当前为 `1` |
| `entryClassName` | 插件入口类完整类名 |
| `capabilities` | 插件申请的能力 |

### 能力说明

| 能力 | 含义 |
| --- | --- |
| `RECOMMENDATION_CANDIDATES` | 读取推荐候选，用于排序或筛选 |
| `LOCAL_HISTORY_READ` | 读取本地历史摘要和偏好画像 |
| `LOCAL_FEEDBACK_READ` | 读取本地“不感兴趣”等反馈 |
| `NETWORK` | 访问远程服务 |
| `PLUGIN_STORAGE` | 读写插件本地配置或缓存 |
| `PLAYER_STATE` / `PLAYER_CONTROL` | 读取或控制播放器 |
| `DANMAKU_STREAM` / `DANMAKU_MUTATION` | 读取或改写弹幕 |

敏感能力会在安装前要求用户确认。开发者应只申请当前插件确实需要的能力。

### 推荐插件最小示例

```kotlin
class TodayWatchRemixPlugin : RecommendationPluginApi {
    override val capabilityManifest = PluginCapabilityManifest(
        pluginId = "dev.example.today_watch_remix",
        displayName = "Today Watch Remix",
        version = "1.0.0",
        apiVersion = 1,
        entryClassName = "dev.example.todaywatchremix.TodayWatchRemixPlugin",
        capabilities = setOf(
            PluginCapability.RECOMMENDATION_CANDIDATES,
            PluginCapability.LOCAL_HISTORY_READ
        )
    )

    override fun buildRecommendations(request: RecommendationRequest): RecommendationResult {
        val ranked = request.candidateVideos
            .filterNot { it.bvid in request.feedbackSignals.consumedBvids }
            .sortedWith(compareByDescending<PluginVideoCandidate> { it.likeCount }.thenByDescending { it.playCount })
            .take(request.queueLimit)
            .mapIndexed { index, candidate ->
                RecommendedVideo(
                    video = candidate,
                    score = 100.0 - index,
                    confidence = 0.7f,
                    explanation = "按点赞优先，其次按播放量排序"
                )
            }

        return RecommendationResult(
            sourcePluginId = capabilityManifest.pluginId,
            mode = request.mode,
            items = ranked,
            historySampleCount = request.historyVideos.size,
            sceneSignals = request.sceneSignals
        )
    }
}
```

可直接运行的示例见：[Today Watch Remix](../plugins/samples/today-watch-remix/)。

### 打包为 `.bpplugin`

`.bpplugin` 本质是 ZIP 文件。最小包内容：

```text
today-watch-remix.bpplugin
├── plugin-manifest.json
└── classes.jar
```

Gradle 打包任务示例：

```kotlin
val packageBpPlugin by tasks.registering(Zip::class) {
    dependsOn("assembleRelease")
    archiveBaseName.set("today-watch-remix")
    archiveExtension.set("bpplugin")
    destinationDirectory.set(rootProject.layout.buildDirectory.dir("distributions"))

    from(rootProject.layout.projectDirectory.file("plugin-manifest.json"))
    from(layout.buildDirectory.file("intermediates/aar_main_jar/release/syncReleaseLibJars/classes.jar")) {
        rename { "classes.jar" }
    }
}
```

构建后在 BiliPai 插件中心选择生成的 `.bpplugin`，确认插件名称、版本、SHA-256、签名状态和能力授权是否符合预期。

---

## 🔧 源码级原生 Kotlin 插件

> ⚠️ 原生插件需要修改源码并重新编译，适合有 Android 开发经验的开发者

### 插件接口

所有插件必须实现 `Plugin` 基础接口：

```kotlin
interface Plugin {
    val id: String           // 唯一标识符
    val name: String         // 显示名称
    val description: String  // 插件描述
    val version: String      // 版本号
    val author: String       // 作者
    val icon: ImageVector?   // 图标（可选）
    
    suspend fun onEnable() {}     // 启用回调
    suspend fun onDisable() {}    // 禁用回调
    
    @Composable
    fun SettingsContent() {}      // 配置界面
}
```

### 插件类型

#### PlayerPlugin - 播放器插件

用于控制视频播放行为，如自动跳过片段：

```kotlin
interface PlayerPlugin : Plugin {
    suspend fun onVideoLoad(bvid: String, cid: Long)
    suspend fun onPositionUpdate(positionMs: Long): SkipAction?
    fun onVideoEnd()
}

// 跳过动作
sealed class SkipAction {
    object None : SkipAction()
    data class SkipTo(val positionMs: Long, val reason: String) : SkipAction()
    data class ShowButton(val skipToMs: Long, val label: String, val segmentId: String) : SkipAction()
}
```

**示例**: [SponsorBlockPlugin](../app/src/main/java/com/android/purebilibili/feature/plugin/SponsorBlockPlugin.kt)

#### FeedPlugin - 推荐流插件

用于过滤首页推荐视频：

```kotlin
interface FeedPlugin : Plugin {
    fun shouldShowItem(item: VideoItem): Boolean
}
```

#### DanmakuPlugin - 弹幕插件

用于处理弹幕样式和过滤：

```kotlin
interface DanmakuPlugin : Plugin {
    fun processDanmaku(danmaku: DanmakuItem): DanmakuItem?
}
```

### 注册插件

在 `PureApplication.kt` 的插件初始化区域注册：

```kotlin
Looper.myQueue().addIdleHandler {
    PluginManager.initialize(this)
    PluginManager.register(SponsorBlockPlugin())
    PluginManager.register(YourCustomPlugin())  // 添加你的插件
    false
}
```

---

## 📤 安装与分发

### JSON 插件分发

1. **GitHub Gist** - 创建一个公开的 Gist，使用 Raw 链接
2. **GitHub 仓库** - 放在仓库中，使用 Raw 文件链接
3. **Cloudflare R2 / S3** - 上传到云存储
4. **个人服务器** - 确保 HTTPS 可访问

### 链接格式要求

- 必须以 `.json` 结尾
- 必须是直接下载链接（不是 HTML 页面）
- 建议使用 HTTPS

---

## ❓ 常见问题

**Q: 插件安装后为什么没生效？**

A: 确保插件已启用（开关为开），部分插件需要重启应用。

**Q: 如何调试我的 JSON 插件？**

A: 使用在线 JSON 验证器检查语法，确保所有字段都正确。

**Q: 正则表达式不生效？**

A: 确保正则表达式语法正确，可以在 [regex101](https://regex101.com/) 上测试。

**Q: 可以组合多个条件吗？**

A: ✅ 支持！使用 `and` 和 `or` 复合条件可以组合多个条件。参见上方的[复合条件（AND/OR）](#-复合条件andor)章节。

---

## 🤝 社区插件

欢迎分享你的插件！提交 PR 到本仓库的 `plugins/community/` 目录。

---

<p align="center">
  <sub>Made with ❤️ by BiliPai Team</sub>
</p>
