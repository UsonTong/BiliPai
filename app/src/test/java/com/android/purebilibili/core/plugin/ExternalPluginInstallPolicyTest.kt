package com.android.purebilibili.core.plugin

import kotlin.test.Test
import kotlin.test.assertEquals

class ExternalPluginInstallPolicyTest {

    @Test
    fun unknownSignerPackage_requiresExplicitUserApproval() {
        val manifest = PluginCapabilityManifest(
            pluginId = "dev.example.open",
            displayName = "开放插件",
            version = "1.0.0",
            apiVersion = 1,
            entryClassName = "dev.example.OpenPlugin",
            capabilities = setOf(PluginCapability.NETWORK)
        )

        val decision = evaluateExternalPluginInstall(
            packageDescriptor = ExternalPluginPackageDescriptor(
                manifest = manifest,
                packageSha256 = "abc123",
                signerSha256 = null
            ),
            trustedSignerSha256 = emptySet()
        )

        assertEquals(
            ExternalPluginInstallDecision.RequiresUserApproval(
                manifest = manifest,
                packageSha256 = "abc123",
                signerTrusted = false,
                sensitiveCapabilities = setOf(PluginCapability.NETWORK)
            ),
            decision
        )
    }

    @Test
    fun unsupportedApiVersion_blocksInstallBeforeAuthorization() {
        val manifest = PluginCapabilityManifest(
            pluginId = "dev.example.future",
            displayName = "未来插件",
            version = "2.0.0",
            apiVersion = 99,
            entryClassName = "dev.example.FuturePlugin",
            capabilities = setOf(PluginCapability.RECOMMENDATION_CANDIDATES)
        )

        val decision = evaluateExternalPluginInstall(
            packageDescriptor = ExternalPluginPackageDescriptor(
                manifest = manifest,
                packageSha256 = "abc123",
                signerSha256 = "trusted"
            ),
            trustedSignerSha256 = setOf("trusted")
        )

        assertEquals(
            ExternalPluginInstallDecision.Rejected("不支持的插件 API 版本: 99"),
            decision
        )
    }
}
