<div align="center">

<img src="docs/images/233娘.jpeg" height="92" />

# BiliPai

**原生 · 纯净 · 可扩展**

<sub>重新定义你的 B 站体验 · 受 iOS 设计启发的 Android 客户端</sub>

<p>
  <a href="README.md">简体中文</a> · <a href="README_EN.md">English</a> · <a href="docs/wiki/README_v8.0.6_legacy.md">Legacy README</a>
</p>

<p>
  <img src="https://img.shields.io/badge/Version-8.0.6-007AFF?style=flat-square&labelColor=ffffff" />
  <img src="https://img.shields.io/badge/Android-8.0%2B-34C759?style=flat-square&logo=android&logoColor=white" />
  <img src="https://img.shields.io/badge/Kotlin-100%25-7F52FF?style=flat-square&logo=kotlin&logoColor=white" />
  <img src="https://img.shields.io/badge/License-GPL--3.0-FF3B30?style=flat-square" />
  <img src="https://img.shields.io/github/stars/jay3-yy/BiliPai?style=flat-square&color=FF9500&labelColor=ffffff" />
</p>

<p>
  <a href="https://github.com/jay3-yy/BiliPai/releases"><img src="https://img.shields.io/badge/⬇_Download_Latest-007AFF?style=for-the-badge&labelColor=ffffff&color=007AFF" /></a>
  <a href="https://t.me/BiliPaii"><img src="https://img.shields.io/badge/Telegram-交流群-5AC8FA?style=for-the-badge&logo=telegram&logoColor=white" /></a>
  <a href="https://t.me/BiliPai"><img src="https://img.shields.io/badge/Telegram-频道-5AC8FA?style=for-the-badge&logo=telegram&logoColor=white" /></a>
  <a href="https://x.com/YangY_0x00"><img src="https://img.shields.io/badge/X-@YangY__0x00-000000?style=for-the-badge&logo=x&logoColor=white" /></a>
</p>

<sub>最后更新：2026-05-08 · v8.0.6 · 以 <a href="CHANGELOG.md">CHANGELOG</a> 与源码为准</sub>

</div>

<br/>

> [!IMPORTANT]
> 应用内默认设置面向通用场景，建议进入 **设置** 按个人习惯调整外观、动画与播放参数。

<br/>

## 📸 应用预览

<div align="center">

<img src="docs/images/screenshot_preview_1.png" height="440" />
<img src="docs/images/screenshot_preview_2.png" height="440" />
<img src="docs/images/screenshot_preview_3.png" height="440" />
<img src="docs/images/screenshot_preview_4.png" height="440" />
<img src="docs/images/screenshot_preview_5.png" height="440" />

</div>

<br/>

## 💎 设计语言

> 在 Material You 与 iOS Human Interface Guidelines 之间寻找最优解 —— 让 Android 用户也能体验到那种"软、亮、透"的桌面感。

<table>
<tr>
<td width="33%" valign="top" align="center">

### 🪟

**Liquid Glass**

底栏、卡片、播放器面板全面接入毛玻璃模糊层，背景内容随滚动微动；接入 `Haze` 与 `AndroidLiquidGlass`，在 OLED 屏与浅色壁纸下都保持清晰与质感。

</td>
<td width="33%" valign="top" align="center">

### 🧭

**iOS 风格底栏**

底部 Tab 采用胶囊指示器 + 模糊层背景，切换附带阻尼弹簧；平板与折叠屏上自动切换为侧边栏，并支持持久化收起。

</td>
<td width="33%" valign="top" align="center">

### 🎨

**Material 3 Expressive**

v8.0.6 新增 MD3E 子主题：动态 shape、表达性 typography、motion 三件套，覆盖顶栏、底栏、首页分类、搜索、视频设置面板等关键流。

</td>
</tr>
</table>

<br/>

## ✨ 你会喜欢的细节

<table>
<tr>
<td valign="top" width="50%">

#### 🎬 视频播放
- 4K / 1080P60 / HDR / Dolby Vision
- DASH 自适应码率 · 无缝画质切换
- 倍速 0.5×–2.0× · 长按倍速可上滑锁定
- 画中画 · 后台播放 · 锁屏控制
- 帧边界量化的 Seek 预览，重绘更轻
- 横屏顶部时间显示 · 全屏信息更完整

#### 🎧 听视频模式
- 沉浸式 / 黑胶唱片切换
- 歌词、播放列表、定时关闭
- 与系统媒体中心的前后切歌联动

#### 📺 番剧 · 直播 · 动态
- 选集面板 · 季度版本切换
- HLS 直播 · 关注流动态 · GIF 完美渲染
- 图片预览：iOS 风格开关动画 + 立体过渡

</td>
<td valign="top" width="50%">

#### 🔌 插件系统
- 空降助手 · 去广告 · 弹幕增强
- 夜间护眼 · 今日推荐单 · CDN 属地优选
- 支持 JSON 规则插件，URL 一键导入

#### 🔍 智能搜索
- 300ms 防抖实时建议
- UP 空间内搜索 · 视频 BGM 识别
- 视频 / UP 主 / 番剧 分类检索

#### 🔒 隐私友好
- 无广告 · 无追踪 · 权限最小化
- 登录凭证仅存本地
- 运行日志默认不落盘
- 遥测默认仅崩溃追踪，使用统计默认关闭

#### 📥 离线缓存
- 选清晰度下载 · 音视频自动合并
- 断点续传 · 本地播放管理

</td>
</tr>
</table>

<details>
<summary><b>📋 查看完整功能矩阵（视频 / 评论 / 消息 / 个人中心 ...）</b></summary>

<br/>

### 🎬 视频播放（完整）

| 功能 | 描述 |
|-----|-----|
| **高清画质** | 支持 4K / 1080P60 / HDR / Dolby Vision (需登录/大会员) |
| **DASH 流媒体** | 自适应码率选择，无缝切换画质，流畅播放体验 |
| **弹幕系统** | 透明度、字体大小、滚动速度可调，支持弹幕密度过滤 |
| **手势控制** | 左侧上下滑动调节亮度，右侧调节音量，左右滑动快进/快退 |
| **倍速播放** | 0.5x / 0.75x / 1.0x / 1.25x / 1.5x / 2.0x，长按倍速支持上滑锁定 |
| **画中画** | 悬浮小窗播放，多任务无缝切换 |
| **听视频模式** | 🆕 专属音频播放界面，沉浸式/黑胶唱片模式、歌词、播放列表与定时关闭 |
| **AI 总结** | 🆕 智能生成视频内容摘要，快速获取核心信息 |
| **原地播放** | 长按视频封面直接预览播放，点击即可全屏，无缝衔接 |
| **后台播放** | 锁屏/切后台继续听，支持独立开关后台播放与音频焦点 |
| **播放顺序** | 播完暂停 / 顺序 / 单循环 / 列表循环 / 自动连播，横竖屏快捷切换 |
| **播放完成体验** | 关闭"自动播放下一个"后，播完不再弹强干扰操作弹窗 |
| **竖屏交互修复** | 修复竖屏连刷下一条后点赞/收藏失效，收藏改为直接打开收藏夹面板 |
| **Seek 预览优化** | 预览图按帧边界量化，拖动与点击跳转时重绘负担更低 |
| **评论体验** | 默认排序偏好（最热/最新），修复特定排序下 UP 主/置顶评论缺失 |
| **评论复制增强** | 长按进入选择面板，支持拖拽选择评论片段（含表情/富文本） |
| **横屏信息栏** | 全屏顶部新增时间显示，横屏交互信息更完整 |
| **播放记忆** | 自动记录观看进度，续播提示支持开关与同目标仅提醒一次 |
| **高画质扫码登录** | 支持扫码登录，解锁大会员专属高画质 |

### 📺 番剧追番

| 功能 | 描述 |
|-----|-----|
| **番剧首页** | 热门推荐、新番时间表、分区浏览 |
| **选集面板** | 官方风格底部弹出面板，支持季度/版本切换 |
| **横屏顶部操作** | 横屏/全屏场景补齐点赞、投币、分享入口 |
| **追番管理** | 追番列表、观看进度自动同步 |
| **弹幕支持** | 番剧同样支持完整弹幕功能 |

### 📡 直播

| 功能 | 描述 |
|-----|-----|
| **直播列表** | 热门直播、分区浏览、关注直播 |
| **高清直播流** | HLS 自适应码率播放 |
| **直播弹幕** | 实时弹幕显示 |
| **一键跳转** | 动态卡片直接进入直播间 |

### 📱 动态页面

| 功能 | 描述 |
|-----|-----|
| **动态流** | 关注 UP 主的视频/图文/转发动态 |
| **分类筛选** | 全部动态 / 仅视频动态 切换 |
| **GIF 支持** | 完美渲染动态中的 GIF 图片 |
| **图片下载** | 长按预览，一键保存到相册 |
| **图片预览** | 全局 Overlay 预览层 + iOS 风格开关动画，立体过渡切换 |
| **@ 高亮** | 动态中 @用户 自动高亮显示 |

### 💬 消息中心

| 功能 | 描述 |
|-----|-----|
| **消息中心** | 统一入口查看回复我的、@我的、收到的赞、系统通知 |
| **富文本交互** | 支持表情包、@提醒、图片查看 |
| **链接预览** | 自动识别视频链接 (BV号) 并生成即时预览卡片 |
| **消息跳转** | 直接跳转视频、动态、空间、直播、番剧、音乐与网页 |

### 👤 个人中心

| 功能 | 描述 |
|-----|-----|
| **双登录方式** | 扫码登录 / 网页登录 |
| **观看历史** | 自动记录观看历史，支持云同步 |
| **收藏管理** | 收藏夹列表与视频管理 |
| **关注/粉丝** | 关注列表与粉丝列表浏览 |

### 🎨 现代 UI

| 功能 | 描述 |
|-----|-----|
| **Material You** | 动态主题色，根据壁纸自动适配 |
| **iOS 风格底栏** | 优雅的毛玻璃导航栏效果 |
| **卡片动画** | 波浪式进场 + 弹性缩放 + 共享元素过渡 |
| **骨架屏加载** | Shimmer 效果，优雅的加载占位 |
| **Lottie 动画** | 点赞/投币/收藏 精美交互反馈 |
| **庆祝动画** | 三连成功烟花粒子特效 |
| **粒子消散** | "不感兴趣"操作触发响指式粒子消散 |
| **平板适配** | 侧边栏支持持久化切换，底部栏自动居中适配大屏 |

</details>

<br/>

## 🔌 插件生态

> 内置 6 款插件随主应用稳定分发；外部 JSON 规则插件可 URL 导入；外部 Kotlin 包格式（`.bpplugin`）目前以预览、授权和 SDK 适配为主，宿主尚不执行外部 Dex。

| 形态 | 当前情况 | 入口 |
| --- | --- | --- |
| 🟦 **内置插件** | 6 个：空降助手、去广告、弹幕增强、夜间护眼、今日推荐单、CDN 属地优选 | 插件中心 |
| 🟩 **JSON / `.bp` 规则插件** | 支持 URL 导入，适合推荐流过滤、弹幕过滤/高亮 | [JSON 插件开发](docs/PLUGIN_DEVELOPMENT.md) · [社区目录](plugins/community/README.md) |
| 🟧 **外部 `.bpplugin` Kotlin 包** | SDK / 包格式 / manifest / 签名校验已就绪；外部 Dex 暂未执行 | [Plugin SDK](plugins/sdk/README.md) · [示例](plugins/samples/today-watch-remix/) |
| 🟪 **源码级原生插件** | 适合复杂播放器 / 推荐 / 弹幕能力，需重新编译 APK | [原生插件开发](docs/NATIVE_PLUGIN_DEVELOPMENT.md) |

> [!CAUTION]
> 第三方插件接入前请审阅规则或包清单，尤其是 `NETWORK`、`LOCAL_HISTORY_READ`、`LOCAL_FEEDBACK_READ`、`PLAYER_CONTROL` 等敏感能力。

<details>
<summary><b>📖 JSON 规则插件 60 秒上手</b></summary>

```json
{
  "id": "short_video_filter",
  "name": "短视频过滤",
  "type": "feed",
  "rules": [
    { "field": "duration", "op": "lt", "value": 60, "action": "hide" }
  ]
}
```

| 字段 | 说明 |
|---|---|
| `title` / `duration` / `owner.mid` / `owner.name` / `stat.view` | Feed 字段 |
| `content` | Danmaku 字段 |
| `contains` / `regex` / `lt` / `gt` / `eq` / `ne` / `startsWith` | 操作符 |

将 JSON 上传到公开 URL（如 GitHub Gist），在 **设置 → 插件中心 → 导入外部插件** 粘贴链接即可。

📚 完整文档：[PLUGIN_DEVELOPMENT.md](docs/PLUGIN_DEVELOPMENT.md) · 示例：[plugins/samples/](plugins/samples/)

</details>

<details>
<summary><b>📌 今日推荐单算法详解（默认折叠）</b></summary>

#### 通俗版

- **先看你最近看了谁**：统计历史里常看的 UP 主，结合播放进度与"最近看过"权重
- **再给候选视频打分**：综合热度、UP 主匹配、新鲜度、模式偏好（轻松/学习）、夜间护眼、负反馈
- **最后做"去同质化"排序**：避免连续刷到同一个 UP，让列表更耐看

完全本地、可解释的加权排序器，根据观看行为持续微调。

#### 实现路径

> `app/src/main/java/com/android/purebilibili/feature/home/TodayWatchPolicy.kt`
> 画像：`TodayWatchProfileStore.kt` · 反馈：`TodayWatchFeedbackStore.kt`

#### 单条候选打分公式

```
score = base + creator + freshness + seenPenalty + mode + night + feedback

base       = ln(view + 1) * 0.45
creator    = ln(creatorAffinity + 1) * 2.1
freshness  = <=1天:0.8, <=3天:0.55, <=7天:0.3, <=30天:0.1, 其余:-0.05
seenPenalty = 已看过则 -2.6
```

**模式分（RELAX / LEARN）** 通过时长曲线 + 关键词 + 弹幕密度近似刺激度（calmScore）综合打分；**夜间护眼**激活时降低高刺激与超长视频权重；**负反馈**对不感兴趣视频/UP/关键词分别 -3.2 / -2.4 / -0.7（封顶 -2.8）。

**多样化贪心**：`adjusted = score - sameCreatorPenalty(1.15) - repeatPenalty(usedCount*0.75) + noveltyBonus(0.35)`

#### 隐私

完全本地运行，不上传历史用于个性化训练；支持一键清空画像与反馈。

#### 后续优化方向

| 优先级 | 优化点 |
| --- | --- |
| P0 | 候选池分层召回（偏好 UP / 高质量 / 长尾 / 负反馈避让） |
| P0 | 反馈半衰期：短期强降权、长期逐步恢复 |
| P1 | 探索 / 利用配比：70% 偏好 + 20% 新鲜 + 10% 冷门 |
| P1 | 时段与场景特征（夜间、工作日、会话时长） |
| P1 | 多样化约束升级：分区、关键词簇、时长段 |
| P2 | 可解释性校准、离线评估基线 |

> 完整版（含历史预处理 / 画像融合 / 关键词限幅 / 冷启动策略等）见 [README_v8.0.6_legacy.md](docs/wiki/README_v8.0.6_legacy.md#-今日推荐单算法详解默认折叠)

</details>

<br/>

## 📦 下载

<div align="center">

<a href="https://github.com/jay3-yy/BiliPai/releases">
  <img src="https://img.shields.io/badge/⬇_最新版本_Release-007AFF?style=for-the-badge&labelColor=ffffff" />
</a>

</div>

| 项目 | 要求 |
|-----|-----|
| 🤖 **系统** | Android 8.0+ (API 26) |
| 🧠 **架构** | 64 位 (arm64-v8a) |
| ⭐ **推荐** | Android 12+ 获得完整 Material You 体验 |
| 📦 **包大小** | 因 ABI 与构建方式而异，请以 Releases 实际产物为准 |

> 点击安装可能需要允许"未知来源"应用 · 打开后扫码或网页登录即可使用。

<br/>

## 🛠 技术栈

<div align="center">

![Kotlin](https://img.shields.io/badge/Kotlin-007AFF?style=for-the-badge&logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack_Compose-0969DA?style=for-the-badge&logo=jetpackcompose&logoColor=white)
![Material 3](https://img.shields.io/badge/Material_3_Expressive-5AC8FA?style=for-the-badge&logo=materialdesign&logoColor=white)
![Media3](https://img.shields.io/badge/Media3_ExoPlayer-34C759?style=for-the-badge&logo=android&logoColor=white)
![Coroutines](https://img.shields.io/badge/Coroutines_%2B_Flow-AF52DE?style=for-the-badge&logo=kotlin&logoColor=white)

</div>

| 类别 | 选型 |
|-----|-----|
| **语言** | Kotlin 1.9+ · 100% Kotlin |
| **UI** | Jetpack Compose · Material 3 / MD3E · MVVM + Clean Architecture |
| **网络** | Retrofit · OkHttp · Kotlinx Serialization |
| **存储** | Room · DataStore |
| **媒体** | ExoPlayer (Media3) · DanmakuFlameMaster · MediaCodec |
| **视觉特效** | 🪟 [Haze](https://github.com/chrisbanes/haze) · 💎 [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) · 🍎 [Compose Cupertino](https://github.com/alexzhirkevich/compose-cupertino) |
| **动画** | Lottie Compose · Orbital · Compose Shimmer |
| **图片** | Coil Compose（含 GIF 解码） |

<br/>

## 📚 文档与社区

| 入口 | 链接 |
| --- | --- |
| 📘 Wiki 首页 | [docs/wiki/README.md](docs/wiki/README.md) |
| 🤖 AI / LLM 入口 | [llms.txt](llms.txt) · 别名 `AI.txt` / `llm.txt` · [AI 导航](docs/wiki/AI.md) |
| 🧭 功能矩阵 | [docs/wiki/FEATURE_MATRIX.md](docs/wiki/FEATURE_MATRIX.md) |
| 🏛️ 架构 | [docs/wiki/ARCHITECTURE.md](docs/wiki/ARCHITECTURE.md) |
| 📦 发布流程 | [docs/wiki/RELEASE_WORKFLOW.md](docs/wiki/RELEASE_WORKFLOW.md) |
| ✅ QA 手册 | [docs/wiki/QA.md](docs/wiki/QA.md) |
| 📜 旧版 README | [docs/wiki/README_v8.0.6_legacy.md](docs/wiki/README_v8.0.6_legacy.md) |

<br/>

## 🗺️ 路线图

> 同步于 2026-04-17 · 以最新 Release / `CHANGELOG.md` / 主分支为准

<details>
<summary><b>✅ 已完成（点击展开）</b></summary>

- 首页推荐流 + 瀑布流布局
- 视频播放 + 弹幕 + 手势 + 画中画 + 后台
- 听视频模式 + 收藏夹/稍后再看播放列表 + 顺序/随机/单曲循环
- 番剧/影视播放 + 选集面板
- 直播播放 + 分区浏览
- 动态页面 + 图片下载 + GIF 支持
- 图片预览文案与过渡升级
- 离线下载 + 当前视频批量缓存 + 本地播放
- 搜索 + 历史记录
- 原生专栏搜索 + 详情页
- Material You + 深色模式
- 高画质扫码登录 + 首播清晰度鉴权修复
- 横屏控制栏增强
- 共享元素过渡 + 返回首页动效优化
- 平板/折叠屏适配
- 应用内更新（手动 + 自动 + 启动提示 + 应用内下载/安装）
- 插件系统核心架构 + 6 款内置插件
- Firebase Analytics + Crashlytics
- 评论/动态可选择复制
- 消息中心分类页 + 链接直达
- 竖屏视频点赞/收藏交互修复
- Seek 预览重绘优化 + 底栏跨 Tab 快速切换优化

</details>

**🚧 进行中** — 文档站与 Wiki 持续补全（模块 API / 调试手册 / 回归清单）

**📋 计划中** — 观看历史云同步 · 收藏夹管理 · 多账户切换 · 英文/繁体中文支持

<br/>

## 🔄 最近更新（v8.0.6 · 2026-05-06）

- 🎨 **@Jay3-yy** 新增安卓原生 MD3E / Material 3 Expressive 子风格，接入 shape、typography、motion 与外观设置
- 🧩 **@Jay3-yy** 深度适配顶栏、底栏、首页分类、共享列表、搜索、通用列表与视频设置面板的 MD3E 策略
- 🔄 **[@chenx-dust](https://github.com/chenx-dust) [#267](https://github.com/jay3-yy/BiliPai/pull/267)** 修复平板屏幕旋转体验，调整屏幕大小检测方式
- 📺 **[@chenx-dust](https://github.com/chenx-dust) [#267](https://github.com/jay3-yy/BiliPai/pull/267)** 同步修复视频和直播方向策略

完整记录 → [CHANGELOG.md](CHANGELOG.md)

<br/>

## 🏗️ 构建

```bash
git clone https://github.com/jay3-yy/BiliPai.git
cd BiliPai
./gradlew assembleDebug
```

**要求**：JDK 21+ · Android Studio 2024.1+ · Compile SDK 36 · Gradle 8.13+
**可选**：`google-services.json` 放入 `app/` 启用 Firebase；缺失则自动跳过。

<br/>

## 🤝 贡献

欢迎提交 Issue 和 Pull Request！Fork → `feature/xxx` 分支 → Commit → PR。

<br/>

## 🙏 致谢

| 项目 | 用途 |
|-----|-----|
| [Jetpack Compose](https://developer.android.com/jetpack/compose) | 声明式 UI 框架 |
| [ExoPlayer (Media3)](https://github.com/androidx/media) | 媒体播放引擎 |
| [DanmakuFlameMaster](https://github.com/bilibili/DanmakuFlameMaster) | B 站官方弹幕引擎 |
| [DanmakuRenderEngine](https://github.com/bytedance/DanmakuRenderEngine) | 字节跳动高性能弹幕 |
| [bilibili-API-collect](https://github.com/SocialSisterYi/bilibili-API-collect) | B 站 API 文档 |
| [PiliPlus](https://github.com/bggRGjQaUbCoE/PiliPlus) | 播放链路与移动端体验参考 |
| [BilibiliSponsorBlock](https://github.com/hanydd/BilibiliSponsorBlock) | 空降助手数据与 API |
| [Haze](https://github.com/chrisbanes/haze) | 🪟 毛玻璃效果 |
| [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass) | 💎 液态玻璃效果 |
| [Compose Cupertino](https://github.com/alexzhirkevich/compose-cupertino) | 🍎 iOS 风格组件 |
| [Miuix](https://github.com/compose-miuix-ui/miuix) | Miuix 风格组件 |
| [Lottie](https://github.com/airbnb/lottie-android) | 矢量动画 |
| [Coil](https://github.com/coil-kt/coil) | Kotlin 图片加载 |
| [Orbital](https://github.com/skydoves/Orbital) | 共享元素过渡 |

<details>
<summary><b>查看完整致谢列表（30+ 项）</b></summary>

| 项目 | 用途 |
|-----|-----|
| [Compose Shimmer](https://github.com/valentinilk/compose-shimmer) | 骨架屏加载 |
| [ZXing](https://github.com/zxing/zxing) | 二维码生成 |
| [Room](https://developer.android.com/training/data-storage/room) | 数据库持久化 |
| [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) | 偏好设置存储 |
| [Retrofit](https://github.com/square/retrofit) | HTTP 网络请求 |
| [Retrofit Kotlinx Serialization Converter](https://github.com/JakeWharton/retrofit2-kotlinx-serialization-converter) | 序列化转换器 |
| [OkHttp](https://github.com/square/okhttp) | HTTP 客户端 |
| [Brotli Decoder](https://github.com/google/brotli) | Brotli 解压 |
| [Cling](https://github.com/4thline/cling) | DLNA/UPnP 投屏 |
| [Jetty](https://github.com/jetty/jetty.project) | 内嵌 HTTP/Servlet 容器 |
| [NanoHTTPD](https://github.com/NanoHttpd/nanohttpd) | 轻量本地代理服务 |
| [pinyin4j](https://sourceforge.net/projects/pinyin4j/) | 中文拼音转换 |
| [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization) | Kotlin 序列化 |
| [Firebase Crashlytics](https://firebase.google.com/docs/crashlytics) | 崩溃追踪 |
| [AndroidX Palette](https://developer.android.com/training/material/palette-colors) | 动态取色 |
| [LeakCanary](https://github.com/square/leakcanary) | 内存泄漏检测 |
| [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) | 后台任务 |
| [MockK](https://github.com/mockk/mockk) | 单元测试 Mock |
| [Turbine](https://github.com/cashapp/turbine) | Flow 测试断言 |
| [biliSendCommAntifraud](https://github.com/freedom-introvert/biliSendCommAntifraud) | 评论反诈检测参考 |

如有遗漏，欢迎通过 Issue / PR 补充致谢。

</details>

<br/>

## ⚠️ 免责声明

> [!CAUTION]
>
> 1. 本项目仅供 **学习交流**，严禁用于商业用途
> 2. 数据来源 Bilibili 官方 API，版权归上海幻电信息科技有限公司所有
> 3. 登录信息仅保存本地，不会上传任何隐私数据
> 4. 使用本应用观看内容时，请遵守相关法律法规
> 5. 如涉及版权问题，请联系删除

<br/>

## 📄 许可证

[GPL-3.0 License](LICENSE) · 可自由使用 / 修改 / 分发；修改后必须同样开源；不得用于商业用途；不得移除原作者信息。

<br/>

## ⭐ Star History

<div align="center">

[![Star History Chart](https://api.star-history.com/svg?repos=jay3-yy/BiliPai&type=Date)](https://github.com/jay3-yy/BiliPai/stargazers)

</div>

<br/>

---

<div align="center">

Made with ❤️ by <a href="https://x.com/YangY_0x00">YangY</a>

<sub>( ゜- ゜)つロ 干杯~</sub>

</div>
