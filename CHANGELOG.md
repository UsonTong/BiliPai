# Changelog

## [5.1.1] - 2026-02-07

### ✨ New Features (新增功能)

- **Experimental Features**: Added some experimental features for better user experience.

### 🛠 Improvements & Fixes (优化与修复)

- **System Stability**: Fixed some known issues and optimized layout performance.

## [5.1.0] - 2026-02-06

### 🛠 Improvements (优化)

- **Scrolling Performance**: Optimized list scrolling performance and reduced recomposition overhead.
- **UI Interaction**: Enhanced card press feedback and physics.

## [5.0.5] - 2026-02-05

### ✨ New Features (新增功能)

- **Video Player Optimization**: Narrowed brightness/volume trigger zones in portrait mode to prevent accidental triggers when swiping for fullscreen.
- **AI Summary**: Added support for AI-generated video summaries.
- **Music Identification**: Added support for identifying and searching for BGM in videos.
- **Version Bump**: Updated app version to 5.0.5.

### 🛠 Improvements (优化)

- **Engineering**: Removed mandatory dependency on `google-services.json` for cleaner builds.
- **Tablet Support**: Improved drawer and bottom bar interaction on tablets.
- **Messaging**: Enhanced private message loading and added video link previews.

## [5.0.1] - 2026-02-01

### ✨ New Features (新增功能)

- **Deep Link Support**: Added comprehensive support for Bilibili links (Video, Live, Space, Dynamic). Supports `bilibili.com`, `m.bilibili.com`, `live.bilibili.com`, `space.bilibili.com`, `t.bilibili.com`.
- **Playback Controls**:
  - Added "Loop Single" (单曲循环) mode.
  - Added "Shuffle" (随机播放) mode.
  - Added "Sequential" (顺序播放) mode.
  - Added "Pause on Completion" (播完暂停) logic when auto-play is disabled.
- **Settings**:
  - Fixed "Auto-Play Next" setting synchronization.

### 🐛 Bug Fixes (修复)

- **UI**: Fixed "Share" button in video detail screen not responding.
- **UI**: Renamed "IP属地" to "IP归属地" for consistency.
- **Compilation**: Resolved build errors related to `PlaylistManager` and `PlayMode`.
