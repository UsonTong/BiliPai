package com.android.purebilibili.feature.dynamic.components

import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.android.purebilibili.core.util.BilibiliNavigationTargetParser
import com.android.purebilibili.data.model.response.DynamicDesc
import com.android.purebilibili.data.model.response.RichTextNode

internal const val DYNAMIC_RICH_TEXT_URL_TAG = "URL"

internal enum class DynamicRichTextOpenMode {
    IN_APP,
    EXTERNAL
}

private val DYNAMIC_RICH_TEXT_URL_PATTERN =
    """((https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])""".toRegex()

internal fun buildDynamicRichTextAnnotatedString(
    desc: DynamicDesc,
    primaryColor: Color,
    textColor: Color
): AnnotatedString {
    return buildAnnotatedString {
        if (desc.rich_text_nodes.isNotEmpty()) {
            desc.rich_text_nodes.forEach { node ->
                appendDynamicRichTextNode(
                    node = node,
                    primaryColor = primaryColor,
                    textColor = textColor
                )
            }
        } else {
            appendDynamicRichTextPlainText(
                text = desc.text,
                primaryColor = primaryColor
            )
        }
    }
}

internal fun resolveDynamicDescForImages(
    desc: DynamicDesc,
    hasImages: Boolean
): DynamicDesc {
    if (!hasImages || !isDynamicStandaloneImagePlaceholder(desc.text)) return desc
    return desc.copy(
        text = "",
        rich_text_nodes = desc.rich_text_nodes.filterNot { node ->
            isDynamicStandaloneImagePlaceholder(node.text)
        }
    )
}

internal fun shouldRenderDynamicRichText(desc: DynamicDesc?): Boolean {
    if (desc == null) return false
    if (desc.text.isNotBlank()) return true
    return desc.rich_text_nodes.any { node ->
        node.text.isNotBlank() && !isDynamicStandaloneImagePlaceholder(node.text)
    }
}

private fun isDynamicStandaloneImagePlaceholder(text: String): Boolean {
    return text.trim() in setOf("[图片]", "【图片】")
}

internal fun resolveDynamicRichTextOpenMode(
    rawUrl: String
): DynamicRichTextOpenMode? {
    val url = rawUrl.trim()
    if (url.isBlank()) return null

    if (BilibiliNavigationTargetParser.parse(url) != null || isDynamicRichTextInAppHost(url)) {
        return DynamicRichTextOpenMode.IN_APP
    }
    return DynamicRichTextOpenMode.EXTERNAL
}

private fun AnnotatedString.Builder.appendDynamicRichTextNode(
    node: RichTextNode,
    primaryColor: Color,
    textColor: Color
) {
    val nodeType = node.type.removePrefix("RICH_TEXT_NODE_TYPE_")
    when {
        nodeType == "EMOJI" && node.emoji?.icon_url?.isNotEmpty() == true -> {
            appendInlineContent(id = node.text, alternateText = node.text)
        }

        shouldRenderDynamicRichTextLink(nodeType, node) -> {
            appendDynamicRichTextLink(
                displayText = node.text,
                targetUrl = resolveDynamicRichTextLinkTarget(node),
                primaryColor = primaryColor
            )
        }

        nodeType == "AT" || nodeType == "TOPIC" -> {
            withStyle(SpanStyle(color = primaryColor, fontWeight = FontWeight.Medium)) {
                append(node.text)
            }
        }

        else -> {
            withStyle(SpanStyle(color = textColor)) {
                appendDynamicRichTextPlainText(
                    text = node.text,
                    primaryColor = primaryColor
                )
            }
        }
    }
}

private fun shouldRenderDynamicRichTextLink(
    nodeType: String,
    node: RichTextNode
): Boolean {
    if (nodeType in setOf("AT", "TOPIC")) return false
    if (nodeType in setOf("WEB", "LINK", "URL")) return true
    return !resolveDynamicRichTextLinkTarget(node).isNullOrBlank() &&
        DYNAMIC_RICH_TEXT_URL_PATTERN.containsMatchIn(node.text)
}

private fun resolveDynamicRichTextLinkTarget(node: RichTextNode): String? {
    normalizeDynamicRichTextUrl(node.jump_url)?.let { return it }
    return DYNAMIC_RICH_TEXT_URL_PATTERN.find(node.text)?.value
}

private fun AnnotatedString.Builder.appendDynamicRichTextPlainText(
    text: String,
    primaryColor: Color
) {
    var lastIndex = 0
    DYNAMIC_RICH_TEXT_URL_PATTERN.findAll(text).forEach { match ->
        if (match.range.first > lastIndex) {
            append(text.substring(lastIndex, match.range.first))
        }
        appendDynamicRichTextLink(
            displayText = match.value,
            targetUrl = match.value,
            primaryColor = primaryColor
        )
        lastIndex = match.range.last + 1
    }
    if (lastIndex < text.length) {
        append(text.substring(lastIndex))
    }
}

private fun AnnotatedString.Builder.appendDynamicRichTextLink(
    displayText: String,
    targetUrl: String?,
    primaryColor: Color
) {
    val resolvedUrl = targetUrl?.trim().takeUnless { it.isNullOrEmpty() } ?: displayText
    pushStringAnnotation(tag = DYNAMIC_RICH_TEXT_URL_TAG, annotation = resolvedUrl)
    withStyle(
        SpanStyle(
            color = primaryColor,
            fontWeight = FontWeight.Medium,
            textDecoration = TextDecoration.Underline
        )
    ) {
        append(displayText)
    }
    pop()
}

private fun normalizeDynamicRichTextUrl(rawUrl: String?): String? {
    val url = rawUrl?.trim().orEmpty()
    if (url.isBlank()) return null
    return when {
        url.startsWith("//") -> "https:$url"
        else -> url
    }
}

private fun isDynamicRichTextInAppHost(url: String): Boolean {
    val normalized = normalizeDynamicRichTextUrl(url) ?: return false
    val host = runCatching { java.net.URI(normalized) }
        .getOrNull()
        ?.host
        ?.lowercase()
        .orEmpty()
    return host.contains("b23.tv") || host.contains("bilibili.com")
}
