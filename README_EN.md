# BiliPai <img src="docs/images/233娘.jpeg" height="80" align="center">

<p align="center">
  <strong>Native, Pure, Extensible — Redefining your Bilibili experience</strong>
</p>

<p align="center">
  <sub>Last updated: 2026-02-11 · Synced to v5.2.2 (source of truth: <a href="CHANGELOG.md">CHANGELOG</a> + code)</sub>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Version-5.2.2-fb7299?style=flat-square" alt="Version">
  <img src="https://img.shields.io/github/stars/jay3-yy/BiliPai?style=flat-square&color=yellow" alt="Stars">
  <img src="https://img.shields.io/github/forks/jay3-yy/BiliPai?style=flat-square&color=green" alt="Forks">
  <img src="https://img.shields.io/github/last-commit/jay3-yy/BiliPai?style=flat-square&color=purple" alt="Last Commit">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android%2010+-brightgreen?style=flat-square" alt="Platform">
  <img src="https://img.shields.io/badge/APK-14MB-orange?style=flat-square" alt="Size">
  <img src="https://img.shields.io/badge/License-GPL--3.0-blue?style=flat-square" alt="License">
  <img src="https://img.shields.io/badge/Plugins-4%20Built--in-blueviolet?style=flat-square" alt="Plugins">
</p>

<p align="center">
  <a href="https://t.me/BiliPai"><img src="https://img.shields.io/badge/Telegram-Join-2CA5E0?style=flat-square&logo=telegram" alt="Telegram"></a>
  <a href="https://x.com/YangY_0x00"><img src="https://img.shields.io/badge/X-Follow-000000?style=flat-square&logo=x" alt="X"></a>
</p>

## 📸 Preview

<p align="center">
  <img src="docs/images/screenshot_preview_1.png" alt="Preview 1" height="500">
  <img src="docs/images/screenshot_preview_2.png" alt="Preview 2" height="500">
  <img src="docs/images/screenshot_preview_3.png" alt="Preview 3" height="500">
  <img src="docs/images/screenshot_preview_4.png" alt="Preview 4" height="500">
  <img src="docs/images/screenshot_preview_5.png" alt="Preview 5" height="500">
</p>
---

## ✨ Features

### 🎬 Video Playback

| Feature | Description |
|-----|-----|
| **HD Quality** | Supports 4K / 1080P60 / HDR / Dolby Vision (Login/Premium required) |
| **DASH Streaming** | Adaptive bitrate selection, seamless quality switching, smooth playback |
| **Danmaku System** | Adjustable opacity, font size, speed, and density filtering |
| **Gesture Control** | Brightness (left), Volume (right), Seek (horizontal) |
| **Playback Speed** | 0.5x / 0.75x / 1.0x / 1.25x / 1.5x / 2.0x |
| **Picture-in-Picture** | Floating window playback for multitasking |
| **Audio Mode** | 🆕 Dedicated audio player with immersive/vinyl modes, lyrics, and playlist management |
| **Background Play** | Continue listening when screen is off or in background |
| **Playback History** | Automatically resume playback from where you left off |
| **TV Login** | Scan QR code to login as TV client to unlock high quality |
| **Plugin System** | Built-in SponsorBlock, AdBlock, Danmaku Enhancement, and Eye Protection plugins |

### 🔌 Plugin System

| Plugin | Description |
|-----|-----|
| **SponsorBlock** | Automatically skip ads/sponsor segments based on BilibiliSponsorBlock database |
| **AdBlock** | Smartly filter commercial content from recommendation feeds |
| **Danmaku Plus** | Keyword blocking and highlighting for personalized danmaku experience |
| **Night Mode** | Scheduled eye protection, usage reminders, warm color filter |
| **Plugin Center** | Unified management for all plugins with independent configurations |
| **🆕 External Plugins** | Support loading dynamic JSON rule plugins via URL |

<details>
<summary><b>📖 JSON Rule Plugin Quick Start (Click to expand)</b></summary>

#### What is a JSON Rule Plugin?

A lightweight plugin format requiring **no coding**, just a simple JSON file to implement content filtering.

#### Plugin Structure

```json
{
    "id": "my_plugin",
    "name": "My Plugin",
    "description": "Plugin description",
    "version": "1.0.0",
    "author": "Your Name",
    "type": "feed",
    "rules": [
        {
            "field": "title",
            "op": "contains",
            "value": "Ad",
            "action": "hide"
        }
    ]
}
```

#### Supported Fields

| Type | Field | Description |
|------|------|------|
| **Feed** | `title` | Video Title |
| **Feed** | `duration` | Video Duration (seconds) |
| **Feed** | `owner.mid` | Uploader UID |
| **Feed** | `owner.name` | Uploader Name |
| **Feed** | `stat.view` | Play Count |
| **Danmaku** | `content` | Danmaku Content |

#### Operators

| Operator | Description | Example |
|--------|------|------|
| `contains` | Contains string | `"value": "Ad"` |
| `regex` | Regular expression | `"value": "Shocking.*Must Watch"` |
| `lt` / `gt` | Less than / Greater than | `"value": 60` |
| `eq` / `ne` | Equal / Not Equal | `"value": 123456` |
| `startsWith` | Starts with | `"value": "【"` |

#### Example: Short Video Filter

```json
{
    "id": "short_video_filter",
    "name": "Short Video Filter",
    "type": "feed",
    "rules": [
        { "field": "duration", "op": "lt", "value": 60, "action": "hide" }
    ]
}
```

#### Installation

1. Upload the JSON file to a publicly accessible URL (e.g., GitHub Gist)
2. In BiliPai, go to **Settings → Plugin Center → Import External Plugin**
3. Paste the URL and install

</details>

> 📚 **Full Documentation**: [Plugin Development Guide](docs/PLUGIN_DEVELOPMENT.md)
>
> 🧩 **Sample Plugins**: [plugins/samples/](plugins/samples/)

### 📺 Anime / Bangumi

| Feature | Description |
|-----|-----|
| **Bangumi Home** | Hot recommendations, schedule, categorical browsing |
| **Episode Selection** | Official style bottom sheet for switching episodes/seasons |
| **Tracking** | Watch list management and progress synchronization |
| **Danmaku** | Full danmaku support for anime |

### 📡 Live Streaming

| Feature | Description |
|-----|-----|
| **Live List** | Hot live streams, categories, followed streamers |
| **HD Streaming** | HLS adaptive bitrate playback |
| **Live Danmaku** | Real-time danmaku display |
| **Quick Access** | Jump to live room directly from dynamic cards |

### 📱 Dynamic Feed

| Feature | Description |
|-----|-----|
| **Feeds** | View videos/posts/reposts from followed uploaders |
| **Filtering** | Switch between All / Video Only |
| **GIF Support** | Perfect rendering of GIF images in dynamic posts |
| **Image Download** | Long press to preview and save to gallery |
| **@ Highlighting** | Auto-highlight @User mentions |

### 📥 Offline Cache

| Feature | Description |
|-----|-----|
| **Download** | Select quality, auto-merge audio/video |
| **Resumable** | Auto-resume downloads after network interruption |
| **Management** | Clear download list and progress display |
| **Local Playback** | Manage and play offline videos |

### 🔍 Smart Search

| Feature | Description |
|-----|-----|
| **Real-time Suggestions** | Search suggestions while typing (300ms debounce) |
| **Trending** | Display current hot search terms |
| **History** | Auto-save search history with deduplication |
| **Categories** | Search by Video / Uploader / Anime |

### 🎨 Modern UI Design

| Feature | Description |
|-----|-----|
| **Material You** | Dynamic theming based on wallpaper |
| **Dark Mode** | Perfect dark mode support |
| **iOS Style Bar** | Elegant frosted glass navigation bar |
| **Animations** | Wave entrance, elastic scaling, shared element transitions |
| **Shimmer** | Elegant loading placeholders |
| **Lottie** | Beautiful interactions for Like/Coin/Fav |
| **Celebration** | Particle effects for successful interactions |

### 👤 Profile

| Feature | Description |
|-----|-----|
| **Dual Login** | QR Code / Web Login |
| **Info** | Avatar, nickname, level, coin display |
| **History** | Auto-record watch history with cloud sync support |
| **Favorites** | Manage favorites and playlists |
| **Following** | Browse following/fans list |

### 🔒 Privacy Friendly

- 🚫 **No Ads** - Pure viewing experience, no ad injections
- 🔐 **Minimal Permissions** - Only essential permissions (No Location/Contacts/Phone)
- 💾 **Local Storage** - Login credentials stored locally, no privacy data upload
- 🔍 **Open Source** - Full source code available for review

---

## 📦 Download & Install

<a href="https://github.com/jay3-yy/BiliPai/releases">
  <img src="https://img.shields.io/badge/Download-Latest%20Release-fb7299?style=for-the-badge&logo=github" alt="Download">
</a>

### Requirements

| Item | Requirement |
|-----|-----|
| **Android Version** | Android 10+ (API 29) |
| **Architecture** | 64-bit (arm64-v8a) |
| **Recommended** | Android 12+ for full Material You experience |
| **Size** | ~14 MB |

### Installation

1. Download the latest APK from [Releases](https://github.com/jay3-yy/BiliPai/releases)
2. Install on your device (Unknown Sources permission may be required)
3. Open app, login via QR code or Web
4. Enjoy the pure Bilibili experience!

---

## 🛠 Tech Stack

### Core Framework

| Category | Technology | Description |
|-----|-----|-----|
| **Language** | Kotlin 1.9+ | 100% Kotlin |
| **UI** | Jetpack Compose | Declarative UI, Material 3 |
| **Architecture** | MVVM + Clean Architecture | Clear separation, maintainable |

### Network & Data

| Category | Technology | Description |
|-----|-----|-----|
| **Network** | Retrofit + OkHttp | RESTful API |
| **Serialization** | Kotlinx Serialization | JSON parsing |
| **Storage** | Room + DataStore | Database + Preferences |
| **Image** | Coil Compose | GIF support |

### Media

| Category | Technology | Description |
|-----|-----|-----|
| **Player** | ExoPlayer (Media3) | DASH / HLS / MP4 |
| **Danmaku** | DanmakuFlameMaster | Official Bilibili engine |
| **Decoding** | MediaCodec | Hardware acceleration |

### UI Enhancements

| Category | Technology | Description |
|-----|-----|-----|
| **Animation** | Lottie Compose | High quality vector animations |
| **Blur** | Haze | iOS style frosted glass |
| **Theming** | Material 3 | Dynamic color extraction |

---

## 🗺️ Roadmap

### ✅ Completed

- [x] Home Waterfall Feed
- [x] Video Player + Danmaku + Gestures
- [x] Anime/Movie Playback
- [x] Live Streaming
- [x] Dynamic Feed
- [x] Offline Download
- [x] Search + History
- [x] Material You + Dark Mode
- [x] TV Login (High Quality)
- [x] Shared Element Transitions
- [x] Plugin System Core
- [x] Built-in Plugins

### 🚧 WIP

- [ ] Danmaku Sending
- [x] Tablet/Foldable Support (Basic)
- [ ] In-app Update

### 📋 Planned

- [ ] History Cloud Sync
- [ ] Favorites Management
- [ ] Multi-account
- [ ] English/Traditional Chinese Support

---

## 🔄 Changelog

See full changelog: [CHANGELOG.md](CHANGELOG.md)

### Latest (v5.2.2 · 2026-02-11)

- 🛠 **Portrait playback flow**: Improved portrait-mode switching continuity and progress sync across portrait/landscape transitions.
- 🛠 **Dynamic UX**: Added double-tap bottom-tab reselect to scroll Dynamic feed back to top; fixed non-clickable images in forwarded dynamics.
- 🛠 **Inbox stability**: Improved private-message list user info resolution to reduce intermittent avatar/username loading failures.
- 🎨 **Image preview motion**: Unified open/close transitions with smoother corner morphing and spring-like close rebound.

---

## 🏗️ Build

```bash
git clone https://github.com/jay3-yy/BiliPai.git
cd BiliPai
./gradlew assembleDebug
```

---

## 🤝 Contributing

Issues and Pull Requests are welcome!

1. Fork the repository
2. Create feature branch
3. Commit changes
4. Push to branch
5. Submit Pull Request

---

## ⚠️ Disclaimer

> [!CAUTION]
>
> 1. This project is for **learning purposes only**. Commercial use is strictly prohibited.
> 2. Data source: Bilibili Official API. Copyright belongs to Shanghai Hupu Information Technology Co., Ltd.
> 3. Login info is stored locally and never uploaded.
> 4. Please comply with local laws and regulations.
> 5. Contact for deletion if copyright infringement occurs.

---

## 📄 License

[GPL-3.0 License](LICENSE)

---

## ☕ Support

If you like BiliPai, buy me a coffee ☕

<p align="center">
  <img src="docs/donate.jpg" alt="Donation" width="300">
</p>
