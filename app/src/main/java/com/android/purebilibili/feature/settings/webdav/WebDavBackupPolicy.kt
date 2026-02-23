package com.android.purebilibili.feature.settings.webdav

import java.net.URLDecoder
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private val RESPONSE_BLOCK_REGEX = Regex(
    pattern = "<[^>]*:?response[^>]*>(.*?)</[^>]*:?response>",
    options = setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
)
private val RFC_1123_FORMATTER: DateTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME
private val FILE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.US)

internal const val DEFAULT_WEBDAV_REMOTE_DIR = "/BiliPai/backups"

data class WebDavBackupEntry(
    val fileName: String,
    val href: String,
    val sizeBytes: Long,
    val lastModifiedEpochMs: Long
)

internal fun normalizeWebDavBaseUrl(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return ""
    return trimmed.removeSuffix("/")
}

internal fun normalizeWebDavRemoteDir(raw: String): String {
    val parts = raw
        .trim()
        .replace('\\', '/')
        .split('/')
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    if (parts.isEmpty()) return DEFAULT_WEBDAV_REMOTE_DIR
    return "/" + parts.joinToString("/")
}

internal fun buildWebDavBackupFileName(epochMs: Long): String {
    val time = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneOffset.UTC)
    return "bilipai-backup-${time.format(FILE_TIME_FORMATTER)}.zip"
}

internal fun parseWebDavBackupEntries(xml: String): List<WebDavBackupEntry> {
    return RESPONSE_BLOCK_REGEX
        .findAll(xml)
        .mapNotNull { match ->
            val block = match.groupValues.getOrNull(1) ?: return@mapNotNull null
            val href = extractXmlTagValue(block, "href") ?: return@mapNotNull null
            val decodedHref = decodeXml(href)
            val fileName = extractFileNameFromHref(decodedHref) ?: return@mapNotNull null
            if (!fileName.endsWith(".zip", ignoreCase = true)) return@mapNotNull null

            val size = extractXmlTagValue(block, "getcontentlength")
                ?.trim()
                ?.toLongOrNull()
                ?: 0L
            val lastModifiedMs = parseLastModified(
                raw = extractXmlTagValue(block, "getlastmodified"),
                fileName = fileName
            )

            WebDavBackupEntry(
                fileName = fileName,
                href = decodedHref,
                sizeBytes = size,
                lastModifiedEpochMs = lastModifiedMs
            )
        }
        .sortedWith(compareByDescending<WebDavBackupEntry> { it.lastModifiedEpochMs }
            .thenByDescending { it.fileName })
        .toList()
}

internal fun selectLatestWebDavBackup(entries: List<WebDavBackupEntry>): WebDavBackupEntry? {
    return entries
        .maxWithOrNull(compareBy<WebDavBackupEntry> { it.lastModifiedEpochMs }.thenBy { it.fileName })
}

internal fun resolveWebDavDownloadUrl(baseUrl: String, hrefOrPath: String): String {
    val candidate = hrefOrPath.trim()
    if (candidate.startsWith("http://") || candidate.startsWith("https://")) {
        return candidate
    }

    val normalizedBase = normalizeWebDavBaseUrl(baseUrl)
    if (candidate.startsWith("/")) {
        val baseUri = URI(normalizedBase)
        val origin = "${baseUri.scheme}://${baseUri.authority}"
        return origin + candidate
    }
    return "$normalizedBase/$candidate"
}

private fun extractXmlTagValue(block: String, localTagName: String): String? {
    val regex = Regex(
        pattern = "<[^>]*:?$localTagName[^>]*>(.*?)</[^>]*:?$localTagName>",
        options = setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE)
    )
    val raw = regex.find(block)?.groupValues?.getOrNull(1)?.trim() ?: return null
    return raw.takeIf { it.isNotEmpty() }
}

private fun extractFileNameFromHref(href: String): String? {
    val normalized = href.trim()
    if (normalized.endsWith("/")) return null
    val segment = normalized.substringAfterLast('/', missingDelimiterValue = "")
    if (segment.isBlank()) return null
    return runCatching {
        URLDecoder.decode(segment, StandardCharsets.UTF_8.name())
    }.getOrDefault(segment)
}

private fun parseLastModified(raw: String?, fileName: String): Long {
    if (!raw.isNullOrBlank()) {
        try {
            return ZonedDateTime.parse(raw.trim(), RFC_1123_FORMATTER).toInstant().toEpochMilli()
        } catch (_: DateTimeParseException) {
            // fall through to filename parsing
        }
    }

    val fileTimePart = fileName
        .removePrefix("bilipai-backup-")
        .removeSuffix(".zip")
    return runCatching {
        LocalDateTime.parse(fileTimePart, FILE_TIME_FORMATTER)
            .toInstant(ZoneOffset.UTC)
            .toEpochMilli()
    }.getOrDefault(0L)
}

private fun decodeXml(raw: String): String {
    return raw
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
}
