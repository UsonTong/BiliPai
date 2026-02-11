# Changelog

## [5.2.2] - 2026-02-11

### ✨ New Features (新增功能)

- **Danmaku Interaction Callback**: Wired danmaku click callback end-to-end for context menu and interaction extension scenarios.

### 🛠 Improvements & Fixes (优化与修复)

- **Portrait Video Mode Upgrade**: Improved portrait-mode player flow, including playback continuity when swiping between videos, progress synchronization across portrait/landscape transitions, and overlay control consistency.
- **Dynamic Feed UX**: Added dynamic-tab bottom reselect double-tap to top behavior and improved smoothness of Home/Dynamic return-to-top animations.
- **Forwarded Dynamic Images**: Fixed an issue where images inside forwarded dynamics could not be opened for preview.
- **Image Preview Animation Polish**: Unified open/close motion for image preview dialog across entry points, with smoother rounded-corner transitions and spring-like close rebound.
- **Inbox User Info Stability**: Improved reliability of avatar/username resolution in private-message list after repeated entry.
- **Version Bump**: Updated app version to `5.2.2`.

## [5.2.1] - 2026-02-11

### 🛠 Improvements & Fixes (优化与修复)

- **Space Dynamic Navigation**: Fixed an issue where image/text dynamics in personal space could not be opened; now dynamic cards route correctly:
  - video dynamics -> native video detail
  - non-video dynamics -> dynamic detail page (`t.bilibili.com/{id_str}`)
- **Home Double-Tap Stability**: Fixed blank area appearing when double-tapping Home from non-top position with "header auto-collapse" enabled; Home double-tap now restores top header/tabs before scroll/refresh.
- **Liquid Glass Indicator Tuning**: Improved bottom bar indicator geometry in icon+text mode so labels participate in refraction more reliably.
- **Version Bump**: Updated app version to `5.2.1`.

## [5.2.0] - 2026-02-10

### ✨ New Features (新增功能)

- **Top Tabs Style Sync**: Top category tab bar now follows bottom bar style linkage, supporting floating/non-floating, blur, and liquid glass modes with unified visual language.
- **Refraction Upgrade**: Added stronger liquid lens profile for tab/bottom indicators during horizontal slide, with a clearer spherical feel and edge-space warp.
- **Incremental Timeline Refresh**: Added optional incremental refresh for Recommend/Following/Dynamic feeds, preserving old content and prepending only new items.
- **Refresh Delta Feedback**: Added "new items count" prompt after manual refresh and an old-content divider cue in Recommend.

### 🛠 Improvements & Fixes (优化与修复)

- **Top Indicator Geometry**: Refined top indicator size/shape/centering and boundary clamping to prevent clipping and offset drift when sliding.
- **Bottom Indicator Refraction Source**: Fixed cases where icon/text were not clearly refracted by switching to icon-layer backdrop capture.
- **Default Visual Bootstrapping**: Added one-time startup migration to ensure default Home visual settings are enabled on first launch after update:
  - floating bottom bar
  - liquid glass enabled
  - top blur enabled
- **Version Bump**: Updated app version to `5.2.0`.

## [5.1.4] - 2026-02-08

### 🛠 Improvements & Fixes (优化与修复)

- **Playback Fix**: Resolved playback issues in certain scenarios.

## [5.1.3] - 2026-02-08

### ✨ New Features (新增功能)

- **Search Upgrade**: Extended search types and interaction flow (视频/UP/番剧/直播), improved suggestion/discover results, and optimized pagination/loading behavior.
- **Comment Preference**: Added configurable default comment sort preference and synchronized it across comment entry points.
- **Danmaku Plugin 2.0**: Added user ID/hash blocking for danmaku plugins, plus in-play hot refresh when danmaku plugin configs/rules change.
- **Fullscreen Clock**: Added a top time display in landscape/fullscreen overlays.
- **Settings Tips Expansion**: Added more hidden usage tips in the tips page.
- **Version Easter Egg**: Enhanced version-click visual/easter-egg effects and toggles.

### 🛠 Improvements & Fixes (优化与修复)

- **Bottom Bar UX**: Reworked bottom bar visibility rules for top-level destinations and fixed alignment/position issues when tab count changes.
- **Playback Completion UX**: When "Auto-play next" is disabled, playback completion no longer forces intrusive action popups.
- **Background Playback Fix**: Fixed an issue where switching recommended videos inside detail page could dirty lifecycle flags and cause unexpected pause on Home/background.
- **Gesture Anti-MisTouch**: Brightness/volume vertical gestures are now limited to left/right one-third zones; center zone no longer triggers accidental adjustments.
- **Like Icon Unification**: Replaced heart-like visuals with thumb-up style across key interaction surfaces.
- **Comment Logic Reliability**: Fixed missing UP/置顶 comments under some sort modes and improved mixed-source comment loading behavior.
- **Firebase Telemetry Hardening**: Strengthened Firebase Analytics + Crashlytics integration (user/session context, custom keys, screen/event tracing, and error domain reporting).
- **General Stability**: Multiple UI/state synchronization fixes across home, video detail, plugin center, and settings.

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
