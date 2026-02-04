# BiliPai <img src="docs/images/233娘.jpeg" height="80" align="center">

<p align="center">
  <a href="README_EN.md">English</a> | <a href="README.md">简体中文</a>
</p>

<p align="center">
  <strong>原生、纯净、可扩展 —— 重新定义你的 B 站体验</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Version-5.0.4-fb7299?style=flat-square" alt="Version">
  <img src="https://img.shields.io/github/stars/jay3-yy/BiliPai?style=flat-square&color=yellow" alt="Stars">
  <img src="https://img.shields.io/github/forks/jay3-yy/BiliPai?style=flat-square&color=green" alt="Forks">
  <img src="https://img.shields.io/github/last-commit/jay3-yy/BiliPai?style=flat-square&color=purple" alt="Last Commit">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%2010+-brightgreen?style=flat-square" alt="Platform">
  <img src="https://img.shields.io/badge/APK-14MB-orange?style=flat-square" alt="Size">
  <img src="https://img.shields.io/badge/License-GPL--3.0-blue?style=flat-square" alt="License">
  <img src="https://img.shields.io/badge/Plugins-3%20Built--in-blueviolet?style=flat-square" alt="Plugins">
</p>

<p align="center">
  <a href="https://t.me/BiliPai"><img src="https://img.shields.io/badge/Telegram-Join-2CA5E0?style=flat-square&logo=telegram" alt="Telegram"></a>
  <a href="https://x.com/YangY_0x00"><img src="https://img.shields.io/badge/X-Follow-000000?style=flat-square&logo=x" alt="X"></a>
</p>

## 📸 应用预览

<p align="center">
  <img src="docs/images/screenshot_preview_1.png" alt="预览图 1" height="500">
  <img src="docs/images/screenshot_preview_2.png" alt="预览图 2" height="500">
  <img src="docs/images/screenshot_preview_3.png" alt="预览图 3" height="500">
  <img src="docs/images/screenshot_preview_4.png" alt="预览图 4" height="500">
  <img src="docs/images/screenshot_preview_5.png" alt="预览图 5" height="500">
</p>
---

## ✨ 功能亮点

### 🎬 视频播放

| 功能 | 描述 |
|-----|-----|
| **高清画质** | 支持 4K / 1080P60 / HDR / Dolby Vision (需登录/大会员) |
| **DASH 流媒体** | 自适应码率选择，无缝切换画质，流畅播放体验 |
| **弹幕系统** | 透明度、字体大小、滚动速度可调，支持弹幕密度过滤 |
| **手势控制** | 左侧上下滑动调节亮度，右侧调节音量，左右滑动快进/快退 |
| **倍速播放** | 0.5x / 0.75x / 1.0x / 1.25x / 1.5x / 2.0x |
| **画中画** | 悬浮小窗播放，多任务无缝切换 |
| **听视频模式** | 🆕 专属音频播放界面，支持沉浸式/黑胶唱片模式，歌词显示与播放列表管理 |
| **原地播放** | 长按视频封面直接预览播放，点击即可全屏，无缝衔接 |
| **后台播放** | 锁屏/切后台继续听，支持通知栏控制 |
| **播放记忆** | 自动记录观看进度，下次打开继续播放 |
| **TV 登录** | 支持 TV 端扫码登录，解锁大会员专属高画质 |
| **插件系统** | 内置空降助手、去广告、弹幕增强三大插件，可扩展架构 |

### 🔌 插件系统

| 插件 | 描述 |
|-----|-----|
| **空降助手** | 基于 BilibiliSponsorBlock 数据库，自动跳过广告/恰饭片段 |
| **去广告插件** | 智能过滤推荐流中的商业推广内容 |
| **弹幕增强** | 关键词屏蔽与高亮，打造个性化弹幕体验 |
| **夜间护眼** | 定时护眼、使用时长提醒、暖色滤镜 |
| **插件中心** | 统一管理所有插件，支持独立配置 |
| **🆕 外部插件** | 支持通过 URL 动态加载 JSON 规则插件 |

<details>
<summary><b>📖 JSON 规则插件快速入门（点击展开）</b></summary>

#### 什么是 JSON 规则插件？

JSON 规则插件是一种**无需编程**的轻量级插件格式，只需编写简单的 JSON 文件即可实现内容过滤功能。

#### 插件结构

```json
{
    "id": "my_plugin",
    "name": "我的插件",
    "description": "插件描述",
    "version": "1.0.0",
    "author": "你的名字",
    "type": "feed",
    "rules": [
        {
            "field": "title",
            "op": "contains",
            "value": "广告",
            "action": "hide"
        }
    ]
}
```

#### 支持的字段

| 类型 | 字段 | 说明 |
|------|------|------|
| **Feed** | `title` | 视频标题 |
| **Feed** | `duration` | 视频时长（秒） |
| **Feed** | `owner.mid` | UP 主 UID |
| **Feed** | `owner.name` | UP 主名称 |
| **Feed** | `stat.view` | 播放量 |
| **Danmaku** | `content` | 弹幕内容 |

#### 操作符

| 操作符 | 说明 | 示例 |
|--------|------|------|
| `contains` | 包含 | `"value": "广告"` |
| `regex` | 正则匹配 | `"value": "震惊.*必看"` |
| `lt` / `gt` | 小于 / 大于 | `"value": 60` |
| `eq` / `ne` | 等于 / 不等于 | `"value": 123456` |
| `startsWith` | 以...开头 | `"value": "【"` |

#### 示例：短视频过滤器

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

#### 安装方式

1. 将 JSON 文件上传到公开可访问的 URL（如 GitHub Gist）
2. 在 BiliPai 中进入 **设置 → 插件中心 → 导入外部插件**
3. 粘贴链接并安装

</details>

> 📚 **完整文档**: [插件开发指南](docs/PLUGIN_DEVELOPMENT.md)
>
> 🧩 **示例插件**: [plugins/samples/](plugins/samples/)

### 📺 番剧追番

| 功能 | 描述 |
|-----|-----|
| **番剧首页** | 热门推荐、新番时间表、分区浏览 |
| **选集面板** | 官方风格底部弹出面板，支持季度/版本切换 |
| **追番管理** | 追番列表、观看进度自动同步 |
| **弹幕支持** | 番剧同样支持完整弹幕功能 |

### 📡 直播功能

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
| **@ 高亮** | 动态中 @用户 自动高亮显示 |

### 💬 私信聊天

| 功能 | 描述 |
|-----|-----|
| **消息列表** | 支持查看历史消息，分页加载 |
| **富文本交互** | 支持表情包、@提醒、图片查看 |
| **链接预览** | 自动识别视频链接 (BV号) 并生成即时预览卡片 |
| **深色适配** | 聊天界面完美适配深色模式 |

### 📥 离线缓存

| 功能 | 描述 |
|-----|-----|
| **视频下载** | 支持选择画质下载，音视频自动合并 |
| **断点续传** | 网络中断后自动恢复下载 |
| **下载管理** | 清晰的下载列表与进度显示 |
| **本地播放** | 离线视频管理与播放 |

### 🔍 智能搜索

| 功能 | 描述 |
|-----|-----|
| **实时建议** | 输入时实时搜索建议 (300ms 防抖优化) |
| **热门榜单** | 展示当前热门搜索词 |
| **历史记录** | 搜索历史自动保存，支持去重 |
| **分类搜索** | 视频 / UP主 / 番剧 分类检索 |

### 🎨 现代 UI 设计

| 功能 | 描述 |
|-----|-----|
| **Material You** | 动态主题色，根据壁纸自动适配 |
| **深色模式** | 完美适配系统深色模式 |
| **iOS 风格底栏** | 优雅的毛玻璃导航栏效果 |
| **卡片动画** | 波浪式进场动画 + 弹性缩放 + 共享元素过渡 |
| **骨架屏加载** | Shimmer 效果，优雅的加载占位 |
| **Lottie 动画** | 点赞/投币/收藏 精美交互反馈 |
| **庆祝动画** | 三连成功烟花粒子特效 |
| **粒子消散** | "不感兴趣"操作触发灭霸响指式粒子消散动画 |
| **平板适配** | 侧边栏支持持久化切换，底部栏自动居中适配大屏体验 |

### 👤 个人中心

| 功能 | 描述 |
|-----|-----|
| **双登录方式** | 扫码登录 / 网页登录 |
| **个人信息** | 头像、昵称、等级、硬币数展示 |
| **观看历史** | 自动记录观看历史，支持云同步 |
| **收藏管理** | 收藏夹列表与视频管理 |
| **关注/粉丝** | 关注列表与粉丝列表浏览 |

### 🔒 隐私友好

- 🚫 **无广告** - 纯净观看体验，无任何广告植入
- 🔐 **权限最小化** - 仅申请必要权限 (无位置/通讯录/电话)
- 💾 **数据本地存储** - 登录凭证仅存本地，不上传任何隐私数据
- 🔍 **开源透明** - 完整源码公开，接受社区审查

---

## 📦 下载安装

<a href="https://github.com/jay3-yy/BiliPai/releases">
  <img src="https://img.shields.io/badge/Download-Latest%20Release-fb7299?style=for-the-badge&logo=github" alt="Download">
</a>

### 系统要求

| 项目 | 要求 |
|-----|-----|
| **Android 版本** | Android 10+ (API 29) |
| **处理器架构** | 64 位 (arm64-v8a) |
| **推荐版本** | Android 12+ 获得完整 Material You 体验 |
| **安装包大小** | ~14 MB |

### 安装步骤

1. 在 [Releases](https://github.com/jay3-yy/BiliPai/releases) 页面下载最新 APK
2. 在设备上点击安装 (可能需要允许"未知来源"应用)
3. 打开应用，扫码或网页登录 Bilibili 账号
4. 开始享受纯净的 B 站体验！

---

## 🛠 技术栈

### 核心框架

| 类别 | 技术 | 说明 |
|-----|-----|-----|
| **语言** | Kotlin 1.9+ | 100% Kotlin 开发 |
| **UI 框架** | Jetpack Compose | 声明式 UI，Material 3 设计语言 |
| **架构模式** | MVVM + Clean Architecture | 分层清晰，易于维护 |

### 网络与数据

| 类别 | 技术 | 说明 |
|-----|-----|-----|
| **网络请求** | Retrofit + OkHttp | RESTful API 调用 |
| **序列化** | Kotlinx Serialization | JSON 解析 |
| **本地存储** | Room + DataStore | 数据库 + 偏好设置 |
| **图片加载** | Coil Compose | 支持 GIF 解码 |

### 媒体播放

| 类别 | 技术 | 说明 |
|-----|-----|-----|
| **视频播放** | ExoPlayer (Media3) | DASH / HLS / MP4 支持 |
| **弹幕引擎** | DanmakuFlameMaster | B 站官方弹幕库 |
| **硬件解码** | MediaCodec | 高效硬件加速 |

### UI 增强

| 类别 | 技术 | 说明 |
|-----|-----|-----|
| **动画** | Lottie Compose | 高品质矢量动画 |
| **毛玻璃** | Haze | iOS 风格模糊效果 |
| **Material You** | Material 3 | 动态取色主题 |

---

## 📂 项目结构

```
app/src/main/java/com/android/purebilibili/
├── app/                # Application 初始化
├── core/               # 核心工具类
│   ├── network/        # 网络配置
│   ├── utils/          # 工具函数
│   └── ui/             # 通用 UI 组件
├── data/               # 数据层
│   ├── api/            # API 接口定义
│   ├── model/          # 数据模型
│   └── repository/     # 数据仓库
├── domain/             # 领域层
│   └── usecase/        # 用例
├── feature/            # 功能模块
│   ├── home/           # 首页
│   ├── video/          # 视频播放
│   ├── bangumi/        # 番剧
│   ├── live/           # 直播
│   ├── dynamic/        # 动态
│   ├── search/         # 搜索
│   ├── download/       # 下载
│   ├── profile/        # 个人中心
│   ├── settings/       # 设置
│   └── login/          # 登录
└── navigation/         # 导航
```

---

## 🗺️ 路线图

> [!NOTE]
> 文档内容可能不会实时更新，部分功能可能在最新代码中已经实现。请以实际 App 体验为准。

### ✅ 已完成功能

- [x] 首页推荐流 + 瀑布流布局
- [x] 视频播放 + 弹幕 + 手势控制
- [x] 番剧/影视播放 + 选集面板
- [x] 直播播放 + 分区浏览
- [x] 动态页面 + 图片下载 + GIF 支持
- [x] 离线下载 + 本地播放
- [x] 搜索 + 历史记录
- [x] Material You + 深色模式
- [x] TV 扫码登录 (解锁高画质)
- [x] 共享元素过渡动画
- [x] 插件系统核心架构
- [x] 内置插件 (空降助手 / 去广告 / 弹幕增强)

### 🚧 开发中

- [ ] 弹幕发送功能
- [x] 平板/折叠屏适配 (侧边栏+底栏优化)
- [ ] 应用内更新检测

### 📋 计划中

- [ ] 观看历史云同步
- [ ] 收藏夹管理
- [ ] 多账户切换
- [ ] 英文/繁体中文支持

---

## 🔄 更新日志

查看完整更新记录：[CHANGELOG.md](CHANGELOG.md)

### 最近更新 (v5.0.4)

- 🛠 **工程优化**: 移除对 `google-services.json` 的强制依赖，支持纯净编译
- 📱 **平板适配**: 优化 Pad 端侧边栏交互与底部导航栏布局
- 💬 **私信升级**: 完善私信列表分页加载，新增视频链接预览卡片
- ✨ **视觉特效**: 新增"不感兴趣"粒子消散动画，优化顶部栏折叠效果
- 🐛 **修复**: 修复已知崩溃与网络连接问题

### 历史更新 (v4.3.0)

- 🛠 **修复**: 修复视频加载时的黑屏问题，强制启用 HTTPS 加载资源
- 🐛 **修复**: 修复竖屏模式下弹幕显示异常及自定义下载路径支持 (v4.2.4)
- 💅 **优化**: 优化画中画模式体验及评论区表情包显示 (v4.2.3)
- ✨ **新功能**: 新增官方启动图 (Splash Wallpaper) 与访客模式优化 (v4.2.0)

---

## 🏗️ 构建项目

```bash
# 克隆仓库
git clone https://github.com/jay3-yy/BiliPai.git
cd BiliPai

# 使用 Android Studio 打开项目
# 或使用命令行构建
./gradlew assembleDebug
```

### 构建要求

- JDK 17+
- Android Studio Hedgehog (2023.1.1) 或更高版本
- Android SDK 34
- Gradle 8.2+
- (可选) `google-services.json`: 放置于 `app/` 目录下以启用 Firebase 功能。如无此文件，构建脚本将自动跳过相关插件，不影响编译运行。

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

---

## 🙏 致谢

| 项目 | 说明 |
|-----|-----|
| [Jetpack Compose](https://developer.android.com/jetpack/compose) | 声明式 UI 框架 |
| [ExoPlayer (Media3)](https://github.com/androidx/media) | 媒体播放引擎 |
| [DanmakuFlameMaster](https://github.com/bilibili/DanmakuFlameMaster) | B 站弹幕引擎 |
| [DanmakuRenderEngine](https://github.com/bytedance/DanmakuRenderEngine) | 字节跳动高性能弹幕引擎 |
| [bilibili-API-collect](https://github.com/SocialSisterYi/bilibili-API-collect) | B 站 API 文档 |
| [Haze](https://github.com/chrisbanes/haze) | 毛玻璃效果库 |
| [Backdrop](https://github.com/Kyant0/AndroidLiquidGlass) | 液态玻璃效果 |
| [Lottie](https://github.com/airbnb/lottie-android) | Airbnb 动画库 |
| [Coil](https://github.com/coil-kt/coil) | Kotlin 图片加载库 |
| [Compose Shimmer](https://github.com/valentinilk/compose-shimmer) | 骨架屏加载效果 |
| [Compose Cupertino](https://github.com/alexzhirkevich/compose-cupertino) | iOS 风格 UI 组件 |
| [ZXing](https://github.com/zxing/zxing) | 二维码生成 |
| [Room](https://developer.android.com/training/data-storage/room) | 数据库持久化 |
| [DataStore](https://developer.android.com/topic/libraries/architecture/datastore) | 偏好设置存储 |
| [Retrofit](https://github.com/square/retrofit) | HTTP 网络请求 |
| [OkHttp](https://github.com/square/okhttp) | HTTP 客户端 |
| [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization) | Kotlin 序列化库 |
| [Firebase Crashlytics](https://firebase.google.com/docs/crashlytics) | 崩溃追踪分析 |
| [Orbital](https://github.com/skydoves/Orbital) | 共享元素过渡动画 |
| [AndroidX Palette](https://developer.android.com/training/material/palette-colors) | 动态取色引擎 |
| [LeakCanary](https://github.com/square/leakcanary) | 内存泄漏检测 |
| [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) | 后台任务管理 |

---

## ⚠️ 免责声明

> [!CAUTION]
>
> 1. 本项目仅供 **学习交流**，严禁用于商业用途
> 2. 数据来源 Bilibili 官方 API，版权归上海幻电信息科技有限公司所有
> 3. 登录信息仅保存本地，不会上传任何隐私数据
> 4. 使用本应用观看内容时，请遵守相关法律法规
> 5. 如涉及版权问题，请联系删除

---

## 📄 许可证

本项目采用 [GPL-3.0 License](LICENSE) 开源协议

这意味着：

- ✅ 可以自由使用、修改和分发
- ✅ 修改后的代码必须同样开源
- ❌ 不得用于商业目的
- ❌ 不得移除原作者信息

## ⭐ Star History

如果这个项目对你有帮助，欢迎点个 Star ⭐

[![Star History Chart](https://api.star-history.com/svg?repos=jay3-yy/BiliPai&type=Date)](https://github.com/jay3-yy/BiliPai/stargazers)

---

<p align="center">
  Made with ❤️ by <a href="https://x.com/YangY_0x00">YangY</a>
  <br>
  <sub>( ゜- ゜)つロ 干杯~</sub>
</p>
