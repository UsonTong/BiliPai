# Today Watch Remix Sample

This is a minimal external Kotlin recommendation plugin sample for the BiliPai
Plugin SDK.

Current host behavior: BiliPai can preview, save, and authorize the generated
`.bpplugin`. It does not execute this plugin yet.

## Build

From this directory:

```bash
./gradlew packageBpPlugin
```

The sample uses a local composite build when it lives inside the BiliPai
repository. If you copy it outside this repository, replace
`0.1.0-SNAPSHOT` with the JitPack tag or commit you want to target.
Set `ANDROID_HOME` or add a local `local.properties` with `sdk.dir=...` if
Gradle cannot find your Android SDK.

The output is written to:

```text
build/distributions/today-watch-remix.bpplugin
```

The package contains:

- `plugin-manifest.json`
- compiled sample classes as `classes.jar`

`classes.jar` is included only as a compiled payload for the preview-era package.
Runtime Dex loading is intentionally not enabled yet.
