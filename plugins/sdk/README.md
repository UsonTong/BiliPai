# BiliPai Plugin SDK

This SDK is the public API surface for external Kotlin plugin authors.

Current status: preview and authorization only. BiliPai can parse a `.bpplugin`,
show its manifest, hash, signer state, and requested capabilities, then save the
package and authorization record. BiliPai does not execute external Dex yet.

## Dependency

JitPack coordinate:

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

The SDK is UI-free. It does not expose Compose, Android `Context`, account
tokens, host repositories, or BiliPai internal singletons.

## Manifest

Every `.bpplugin` must contain a root-level `plugin-manifest.json`:

```json
{
  "pluginId": "dev.example.today_watch_remix",
  "displayName": "Today Watch Remix",
  "version": "1.0.0",
  "apiVersion": 1,
  "entryClassName": "dev.example.TodayWatchRemixPlugin",
  "capabilities": [
    "RECOMMENDATION_CANDIDATES",
    "LOCAL_HISTORY_READ"
  ]
}
```

The manifest maps to `PluginCapabilityManifest` in the SDK.

## Capabilities

Capabilities are declared up front and are shown before install:

- `RECOMMENDATION_CANDIDATES`: read recommendation candidates for ranking.
- `LOCAL_HISTORY_READ`: read local history summary signals.
- `LOCAL_FEEDBACK_READ`: read local feedback such as dislikes.
- `NETWORK`: access remote services.
- `PLUGIN_STORAGE`: read/write plugin-local settings or cache.
- `PLAYER_STATE` / `PLAYER_CONTROL`: read or control playback.
- `DANMAKU_STREAM` / `DANMAKU_MUTATION`: read or mutate danmaku.

Sensitive capabilities still require explicit user approval even when the
package signer is trusted.

## Package Format

`.bpplugin` is a ZIP file. Version 1 requires:

- `plugin-manifest.json` at the ZIP root.
- Optional `plugin-signature.json` at the ZIP root.
- Optional compiled payload entries. Current host builds only preview and
  authorization records, so compiled code is not executed.

BiliPai computes the full package SHA-256. Authorization is bound to
`pluginId + packageSha256`, so changing package contents requires approval again.

## Signature Metadata

Optional `plugin-signature.json`:

```json
{
  "formatVersion": 1,
  "keyId": "official",
  "algorithm": "SHA256withRSA",
  "signatureBase64": "..."
}
```

Supported algorithms are `SHA256withRSA` and `SHA256withECDSA`. Signatures only
affect source trust display; they do not bypass capability approval.

## Sample

See `plugins/samples/today-watch-remix/` for a minimal recommendation plugin
that packages a previewable `.bpplugin`.
