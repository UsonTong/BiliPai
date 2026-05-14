package com.android.purebilibili.feature.home.components

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BottomBarMiuixStructureTest {

    @Test
    fun `android native floating branch renders through kernelsu aligned renderer`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val kernelSuRendererSource = source
            .substringAfter("private fun KernelSuAlignedBottomBar(")
            .substringBefore("@Composable\nprivate fun AndroidNativeBottomBarItem(")

        assertTrue(source.contains("KernelSuAlignedBottomBar("))
        assertTrue(kernelSuRendererSource.contains("AndroidNativeBottomBarTuning"))
        assertTrue(source.contains("resolveKernelSuFloatingBottomBarWidth("))
        assertTrue(kernelSuRendererSource.contains("resolveKernelSuBottomBarSearchLayout("))
        assertTrue(kernelSuRendererSource.contains("KernelSuBottomBarSearchCapsule("))
        assertTrue(source.contains("val collapsedSearchWidth = searchCircleSize"))
        assertTrue(kernelSuRendererSource.contains("label = \"bottomBarDockWidth\""))
        assertTrue(kernelSuRendererSource.contains("label = \"bottomBarSearchWidth\""))
        assertTrue(kernelSuRendererSource.contains("label = \"bottomBarDockContentAlpha\""))
        assertTrue(kernelSuRendererSource.contains("val searchLaunchProgressState = remember { Animatable(0f) }"))
        assertTrue(kernelSuRendererSource.contains("easing = AppMotionEasing.Continuity"))
        assertTrue(kernelSuRendererSource.contains("val launchAdjustedSearchGap = searchGap * (1f - searchLaunchProgress)"))
        assertTrue(kernelSuRendererSource.contains("scaleX = lerp(1f, searchLaunchSpec.targetScaleX, searchLaunchProgress)"))
        assertTrue(kernelSuRendererSource.contains("scaleY = lerp(1f, searchLaunchSpec.targetScaleY, searchLaunchProgress)"))
        assertTrue(kernelSuRendererSource.contains("alpha = lerp(1f, searchLaunchSpec.targetAlpha, searchLaunchProgress)"))
        assertTrue(kernelSuRendererSource.contains("onSearchLaunchTransitionFinished(searchLaunchKey)"))
        assertTrue(kernelSuRendererSource.contains(".width(dockWidth)"))
        assertTrue(kernelSuRendererSource.contains("resolveSharedBottomBarCapsuleShape("))
        assertTrue(kernelSuRendererSource.contains(".kernelSuFloatingDockSurface("))
        assertTrue(kernelSuRendererSource.contains("blurRadius = tuning.shellBlurRadiusDp.dp"))
        assertTrue(kernelSuRendererSource.contains("blur(tuning.shellBlurRadiusDp.dp.toPx())"))
        assertTrue(kernelSuRendererSource.contains("drawBackdrop("))
        assertTrue(kernelSuRendererSource.contains("vibrancy()"))
        assertTrue(kernelSuRendererSource.contains("lens("))
        assertTrue(kernelSuRendererSource.contains("rememberCombinedBackdrop(backdrop, tabsBackdrop)"))
        assertTrue(kernelSuRendererSource.contains("val tabsBackdrop = rememberLayerBackdrop()"))
        assertTrue(
            kernelSuRendererSource.contains(
                "val progress = backdropPresetProgress.shellProgress"
            )
        )
        assertTrue(kernelSuRendererSource.contains("notifyIndexChangedOnReleaseStart = false"))
        assertTrue(kernelSuRendererSource.contains("holdPressUntilReleaseTargetSettles = true"))
        assertTrue(kernelSuRendererSource.contains("dampedDragState.updateIndex(index)"))
        assertFalse(kernelSuRendererSource.contains("selectedSettlePulseKey"))
        assertFalse(kernelSuRendererSource.contains("settlePulseKey = if (index == selectedIndex)"))
        assertTrue(kernelSuRendererSource.contains("if (effectiveSearchExpanded) {\n                                    Modifier.clickable("))
        assertFalse(kernelSuRendererSource.contains("ColorFilter.tint(exportTintColor)"))
        assertFalse(kernelSuRendererSource.contains("val contentColor = Color.White"))
        assertTrue(kernelSuRendererSource.contains("val contentColor = exportItemContentColor(item, coverage)"))
        assertFalse(kernelSuRendererSource.contains("BottomBarStyleIndicatorSurface("))
        assertFalse(source.contains("internal fun BottomBarStyleIndicatorSurface("))
        assertTrue(kernelSuRendererSource.contains("velocityItemsPerSecond = dampedDragState.velocity"))
        assertTrue(kernelSuRendererSource.contains("val indicatorLayerTransform = resolveBottomBarIndicatorLayerTransform("))
        assertTrue(kernelSuRendererSource.contains("scaleX = indicatorLayerTransform.scaleX"))
        assertTrue(kernelSuRendererSource.contains("scaleY = indicatorLayerTransform.scaleY"))
        assertTrue(kernelSuRendererSource.contains("dragScaleProgress = indicatorLayerScaleProgress"))
        assertTrue(kernelSuRendererSource.contains("rememberBottomBarSettleReboundTransform("))
        assertTrue(kernelSuRendererSource.contains("dampedDragState.settledReleaseCount"))
        assertTrue(kernelSuRendererSource.contains("indicatorSettleReboundTransform.scaleX"))
        assertTrue(kernelSuRendererSource.contains("indicatorSettleReboundTransform.scaleY"))
        assertFalse(kernelSuRendererSource.contains(".offset(x = contentPadding + indicatorWidth * dampedDragState.value)"))
        assertTrue(kernelSuRendererSource.contains("val visualIndicatorPosition by remember"))
        assertTrue(kernelSuRendererSource.contains("resolveBottomBarVisualIndicatorPosition("))
        assertTrue(kernelSuRendererSource.contains("resolveBottomBarEdgeStrain("))
        assertTrue(kernelSuRendererSource.contains("val indicatorTranslationXPx by remember(density, contentPadding, indicatorWidth, visualIndicatorPosition)"))
        assertTrue(kernelSuRendererSource.contains("translationX = indicatorTranslationXPx"))
        assertFalse(kernelSuRendererSource.contains("translationX = indicatorTranslationXPx + panelOffsetPx"))
        assertFalse(
            kernelSuRendererSource.contains(
                ".width(dockWidth)\n                        .height(dockHeight)\n                        .graphicsLayer { scaleX = edgeCompressionScaleX }"
            )
        )
        assertTrue(kernelSuRendererSource.contains("scaleX = edgeCompressionScaleX"))
        assertTrue(kernelSuRendererSource.contains("chromaticAberration = true"))
        assertTrue(kernelSuRendererSource.contains("val backdropPresetProgress = resolveBottomBarBackdropPresetProgress("))
        assertTrue(kernelSuRendererSource.contains("motionProgress = motionProgress"))
        assertTrue(kernelSuRendererSource.contains("verticalProgress = verticalGlassProfile.progress"))
        assertTrue(kernelSuRendererSource.contains("pressProgress = dampedDragState.pressProgress"))
        assertTrue(kernelSuRendererSource.contains("val indicatorLayerScaleProgress = maxOf(indicatorDragScaleProgress, pressMotionProgress)"))
        assertTrue(kernelSuRendererSource.contains("val verticalGlassProfile = resolveBottomBarVerticalGlassMotionProfile("))
        assertTrue(kernelSuRendererSource.contains("scrollOffsetPx = scrollOffset"))
        assertTrue(kernelSuRendererSource.contains("nativeVerticalProgress = verticalGlassProfile.progress"))
        assertTrue(kernelSuRendererSource.contains("resolveBottomBarBackdropPresetCaptureLens("))
        assertTrue(kernelSuRendererSource.contains("resolveBottomBarBackdropPresetIndicatorLens("))
        assertTrue(kernelSuRendererSource.contains("progress = backdropPresetProgress.captureProgress"))
        assertTrue(kernelSuRendererSource.contains("progress = backdropPresetProgress.indicatorProgress"))
        assertTrue(kernelSuRendererSource.contains("shouldRenderBottomBarRefractionCapture("))
        assertTrue(kernelSuRendererSource.contains("if (shouldRenderRefractionCapture && backdrop != null)"))
        assertTrue(kernelSuRendererSource.contains(".layerBackdrop(tabsBackdrop)"))
        assertTrue(kernelSuRendererSource.contains("backdropPresetProgress.indicatorProgress > 0f"))
        assertTrue(kernelSuRendererSource.contains("scaleX = refractionMotionProfile.exportCaptureWidthScale"))
        assertTrue(kernelSuRendererSource.contains("resolveBottomBarGlassVisibleContentColor("))
        assertTrue(kernelSuRendererSource.contains("resolveBottomBarGlassExportContentColor("))
        assertTrue(kernelSuRendererSource.contains("indicatorProgress = backdropPresetProgress.indicatorProgress"))
        assertTrue(kernelSuRendererSource.contains("resolveAndroidNativeIdleIndicatorSurfaceColor("))
        assertFalse(kernelSuRendererSource.contains("resolveAndroidNativeIndicatorColor("))
        assertTrue(kernelSuRendererSource.contains("selected = coverage >= 0.5f,"))
        assertTrue(kernelSuRendererSource.contains("contentColorOverride = contentColor,"))
        assertFalse(kernelSuRendererSource.contains("selectionEmphasis = refractionMotionProfile.exportSelectionEmphasis"))
        assertTrue(kernelSuRendererSource.contains("fun itemCoverage(index: Int): Float = resolveBottomBarItemCoverage("))
        val coverageResolverSource = source
            .substringAfter("internal fun resolveBottomBarItemCoverage(")
            .substringBefore("internal fun resolveBottomBarItemMotionScale(")
        assertFalse(coverageResolverSource.contains("itemIndex == currentSelectedIndex"))
        assertTrue(coverageResolverSource.contains("indicatorPosition"))
        assertTrue(kernelSuRendererSource.contains("fun itemScale(coverage: Float): Float = if (glassEnabled)"))
        assertTrue(kernelSuRendererSource.contains("resolveBottomBarLiquidGlassHighlightAlpha("))
        assertFalse(kernelSuRendererSource.contains("val indicatorSurfaceOverlayAlpha"))
        assertFalse(kernelSuRendererSource.contains("Color.Black.copy(indicatorSurfaceOverlayAlpha)"))
        assertFalse(kernelSuRendererSource.contains("0.03f * backdropPresetProgress.indicatorProgress"))
        assertFalse(kernelSuRendererSource.contains("item = currentItem,"))
        assertFalse(kernelSuRendererSource.contains("val tintedContentBackdrop = rememberLayerBackdrop()"))
        assertFalse(kernelSuRendererSource.contains("val refractionMotionProfile by remember"))
        assertFalse(kernelSuRendererSource.contains("blur(8.dp.toPx())"))
        assertFalse(source.contains("private fun MiuixFloatingCapsuleBottomBar("))
        assertFalse(source.contains("private fun MiuixFloatingBottomBarItem("))
        assertFalse(source.contains("resolveBottomBarChromeMaterialMode("))
        assertFalse(source.contains("resolveBottomBarContainerColor("))
        assertFalse(source.contains("LocalSoftwareKeyboardController"))
        assertFalse(source.contains("focusRequester.requestFocus()"))
        assertFalse(kernelSuRendererSource.contains("enabled = searchExpanded"))
    }

    @Test
    fun `bottom bar nonlinear search motion does not change indicator dispersion or item scale`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val refractionProfileSource = source
            .substringAfter("internal fun resolveBottomBarRefractionMotionProfile(")
            .substringBefore("@Composable\nfun FrostedBottomBar(")
        val searchCapsuleSource = source
            .substringAfter("private fun KernelSuBottomBarSearchCapsule(")
            .substringBefore("@Composable\nprivate fun AndroidNativeBottomBarItem(")

        assertTrue(searchCapsuleSource.contains("label = \"bottomBarSearchFieldAlpha\""))
        assertTrue(searchCapsuleSource.contains("label = \"bottomBarSearchIconScale\""))
        assertTrue(searchCapsuleSource.contains("label = \"bottomBarSearchLongPressHorizontalScale\""))
        assertTrue(searchCapsuleSource.contains("detectTapGestures("))
        assertTrue(searchCapsuleSource.contains("onLongPress = {"))
        assertTrue(searchCapsuleSource.contains("currentHaptic(HapticType.SELECTION)"))
        assertTrue(searchCapsuleSource.contains("val currentOnSubmit by rememberUpdatedState(onSubmit)"))
        assertTrue(searchCapsuleSource.contains("val currentHaptic by rememberUpdatedState(haptic)"))
        assertTrue(searchCapsuleSource.contains("Modifier.pointerInput(Unit)"))
        assertFalse(searchCapsuleSource.contains("Modifier.pointerInput(onExpandChange)"))
        val collapsedTapSource = searchCapsuleSource
            .substringAfter("onTap = {")
            .substringBefore("},\n                            onLongPress")
        assertTrue(collapsedTapSource.contains("currentOnSubmit()"))
        assertFalse(collapsedTapSource.contains("currentOnExpandChange(true)"))
        assertTrue(searchCapsuleSource.contains("BasicTextField("))
        assertTrue(searchCapsuleSource.contains("onClick = onSubmit"))
        assertTrue(searchCapsuleSource.contains("keyboardActions = KeyboardActions(onSearch = { onSubmit() })"))
        assertTrue(searchCapsuleSource.contains("easing = AppMotionEasing.Continuity"))
        assertFalse(source.contains("private fun rememberBottomBarSettlePulseTransform("))
        assertFalse(source.contains("settlePulseKey = if (index == selectedIndex)"))
        assertTrue(refractionProfileSource.contains("rawProgress * rawProgress * (3f - 2f * rawProgress)"))
        assertFalse(refractionProfileSource.contains("resolveBottomBarIOSMotionProgress"))
    }

    @Test
    fun `search launch transition compresses bottom bar before navigation callback`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")

        val spec = resolveBottomBarSearchLaunchTransitionSpec()
        assertTrue(spec.durationMillis in 160..240)
        assertTrue(spec.targetScaleX < 1f)
        assertTrue(spec.targetScaleY < 1f)
        assertTrue(spec.targetAlpha < 1f)

        assertTrue(source.contains("searchLaunchKey: Int = 0"))
        assertTrue(source.contains("onSearchLaunchTransitionFinished: (Int) -> Unit = {}"))
        assertTrue(source.contains("if (searchLaunchKey <= 0) return@LaunchedEffect"))
        assertTrue(source.contains("searchLaunchProgressState.animateTo("))
        assertTrue(source.contains("val launchAdjustedSearchGap = searchGap * (1f - searchLaunchProgress)"))
        assertFalse(source.contains("Spacer(modifier = Modifier.width(searchGap))"))
        assertTrue(source.contains("Spacer(modifier = Modifier.width(launchAdjustedSearchGap))"))
    }

    @Test
    fun `sukisu renderer draws visible content below indicator with transparent input overlay`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val kernelSuRendererSource = source
            .substringAfter("private fun KernelSuAlignedBottomBar(")
            .substringBefore("@Composable\nprivate fun AndroidNativeBottomBarItem(")

        val visibleContentIndex = kernelSuRendererSource.indexOf(
            "val coverage = itemCoverage(index)"
        )
        val tintCaptureIndex = kernelSuRendererSource.indexOf(".layerBackdrop(tabsBackdrop)")
        val indicatorIndex = kernelSuRendererSource.indexOf("backdrop = contentBackdrop")
        val hitOverlayIndex = kernelSuRendererSource.indexOf(
            "if (!effectiveSearchExpanded) {\n                    Row(\n                        modifier = Modifier\n                            .fillMaxSize()\n                            .padding(contentPadding)\n                            .alpha(0f)\n                            .graphicsLayer { translationX = panelOffsetPx }\n                            .horizontalDragGesture",
            startIndex = indicatorIndex
        )

        assertTrue(visibleContentIndex >= 0)
        assertTrue(tintCaptureIndex > visibleContentIndex)
        assertTrue(indicatorIndex > tintCaptureIndex)
        assertTrue(hitOverlayIndex > indicatorIndex)
    }

    @Test
    fun `sukisu renderer skips dock and export content when search is stably expanded`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val kernelSuRendererSource = source
            .substringAfter("private fun KernelSuAlignedBottomBar(")
            .substringBefore("@Composable\nprivate fun AndroidNativeBottomBarItem(")

        assertTrue(kernelSuRendererSource.contains("val shouldComposeDockContent = shouldComposeBottomBarDockContent("))
        assertTrue(kernelSuRendererSource.contains("if (shouldComposeDockContent) {"))
        assertTrue(kernelSuRendererSource.contains("if (shouldRenderRefractionCapture && backdrop != null) {"))
        assertTrue(kernelSuRendererSource.contains("val captureWidth = dockWidth + launchAdjustedSearchGap + searchWidth"))
        assertTrue(kernelSuRendererSource.contains(".width(captureWidth)"))
    }

    @Test
    fun `sukisu search capsule participates in the dock aligned refraction capture`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val kernelSuRendererSource = source
            .substringAfter("private fun KernelSuAlignedBottomBar(")
            .substringBefore("@Composable\nprivate fun AndroidNativeBottomBarItem(")
        val refractionCaptureSource = kernelSuRendererSource
            .substringAfter("if (shouldRenderRefractionCapture && backdrop != null) {")
            .substringBefore("if (selectedIndex in visibleItems.indices)")

        assertFalse(source.contains("private fun KernelSuBottomBarSearchRefractionCapture("))
        assertFalse(kernelSuRendererSource.contains("KernelSuBottomBarSearchRefractionCapture("))
        assertTrue(refractionCaptureSource.contains("val captureWidth = dockWidth + launchAdjustedSearchGap + searchWidth"))
        assertTrue(refractionCaptureSource.contains(".width(captureWidth)"))
        assertTrue(refractionCaptureSource.contains(".layerBackdrop(tabsBackdrop)"))
        assertTrue(refractionCaptureSource.contains(".offset(x = dockWidth + launchAdjustedSearchGap)"))
        assertTrue(refractionCaptureSource.contains("KernelSuBottomBarSearchVisualContent("))
        assertTrue(refractionCaptureSource.contains("interactive = false"))
    }

    @Test
    fun `android native input overlay forwards press state to indicator animation`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val kernelSuRendererSource = source
            .substringAfter("private fun KernelSuAlignedBottomBar(")
            .substringBefore("@Composable\nprivate fun AndroidNativeBottomBarItem(")
        val inputTargetSource = source.substringAfter("@Composable\nprivate fun RowScope.BottomBarInputTarget(")

        assertTrue(kernelSuRendererSource.contains("onPressChanged = dampedDragState::setPressed"))
        assertTrue(kernelSuRendererSource.contains("BottomBarInputTarget("))
        assertFalse(
            kernelSuRendererSource
                .substringAfter(".horizontalDragGesture")
                .substringBefore("if (searchEnabled)")
                .contains("AndroidNativeBottomBarItem(")
        )
        assertTrue(inputTargetSource.contains("collectIsPressedAsState()"))
        assertTrue(inputTargetSource.contains("LaunchedEffect(isPressed)"))
        assertTrue(inputTargetSource.contains("DisposableEffect(Unit)"))
        assertTrue(inputTargetSource.contains("currentOnPressChanged(false)"))
    }

    @Test
    fun `ios floating bottom bar also routes to sukisu renderer`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val iosRendererSource = source
            .substringAfter("fun FrostedBottomBar(")
            .substringBefore("@Composable\nprivate fun MaterialBottomBar(")

        assertTrue(iosRendererSource.contains("KernelSuAlignedBottomBar("))
        assertTrue(iosRendererSource.contains("iconStyle = SharedFloatingBottomBarIconStyle.CUPERTINO"))
        assertTrue(iosRendererSource.contains("if (isFloating) {"))
        assertFalse(iosRendererSource.contains("if (isFloating && homeSettings.isBottomBarLiquidGlassEnabled)"))
    }

    @Test
    fun `android native miuix variant routes to dedicated miuix bottom bar renderer`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")

        assertTrue(source.contains("val androidNativeVariant = LocalAndroidNativeVariant.current"))
        assertTrue(source.contains("androidNativeVariant == AndroidNativeVariant.MIUIX"))
        assertTrue(source.contains("MiuixBottomBar("))
        assertTrue(source.contains("if (isFloating) {"))
        assertTrue(source.contains("KernelSuAlignedBottomBar("))
        assertTrue(source.contains("iconStyle = SharedFloatingBottomBarIconStyle.CUPERTINO"))
        assertTrue(source.contains("private enum class SharedFloatingBottomBarIconStyle"))
        assertTrue(source.contains("MiuixNavigationBar("))
        assertTrue(source.contains("MiuixDockedBottomBarItem("))
    }

    @Test
    fun `docked miuix bottom bar avoids floating navigation insets`() {
        val source = loadSource("app/src/main/java/com/android/purebilibili/feature/home/components/BottomBar.kt")
        val miuixRendererSource = source
            .substringAfter("private fun MiuixBottomBar(")
            .substringBefore("@Composable\nprivate fun RowScope.MiuixDockedBottomBarItem(")

        assertTrue(miuixRendererSource.contains("MiuixNavigationBar("))
        assertTrue(miuixRendererSource.contains("MiuixDockedBottomBarItem("))
        assertFalse(miuixRendererSource.contains("MiuixNavigationBarItem("))
        assertFalse(miuixRendererSource.contains("MiuixFloatingNavigationBar("))
        assertFalse(miuixRendererSource.contains("MiuixFloatingNavigationBarItem("))
    }

    private fun loadSource(path: String): String {
        val normalizedPath = path.removePrefix("app/")
        val sourceFile = listOf(
            File(path),
            File(normalizedPath)
        ).firstOrNull { it.exists() }
        require(sourceFile != null) { "Cannot locate $path from ${File(".").absolutePath}" }
        return sourceFile.readText()
    }
}
