package com.android.purebilibili.feature.settings.webdav

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebDavBackupPolicyTest {

    @Test
    fun `normalize base url trims and removes trailing slash`() {
        assertEquals(
            "https://dav.example.com",
            normalizeWebDavBaseUrl("  https://dav.example.com/  ")
        )
    }

    @Test
    fun `normalize remote dir ensures single leading slash and no trailing slash`() {
        assertEquals("/BiliPai/backups", normalizeWebDavRemoteDir("BiliPai/backups/"))
        assertEquals("/BiliPai", normalizeWebDavRemoteDir("//BiliPai//"))
    }

    @Test
    fun `build backup file name follows stable pattern`() {
        val fileName = buildWebDavBackupFileName(0L)
        assertTrue(fileName.startsWith("bilipai-backup-19700101-"))
        assertTrue(fileName.endsWith(".zip"))
    }

    @Test
    fun `parse propfind xml extracts backup entries`() {
        val xml = """
            <?xml version=\"1.0\" encoding=\"utf-8\"?>
            <d:multistatus xmlns:d=\"DAV:\">
              <d:response>
                <d:href>/remote.php/dav/files/user/BiliPai/backups/</d:href>
                <d:propstat><d:prop><d:getlastmodified>Mon, 22 Feb 2026 09:00:00 GMT</d:getlastmodified></d:prop></d:propstat>
              </d:response>
              <d:response>
                <d:href>/remote.php/dav/files/user/BiliPai/backups/bilipai-backup-20260222-090000.zip</d:href>
                <d:propstat><d:prop>
                  <d:getcontentlength>2048</d:getcontentlength>
                  <d:getlastmodified>Mon, 22 Feb 2026 09:00:00 GMT</d:getlastmodified>
                </d:prop></d:propstat>
              </d:response>
              <d:response>
                <d:href>/remote.php/dav/files/user/BiliPai/backups/bilipai-backup-20260223-100000.zip</d:href>
                <d:propstat><d:prop>
                  <d:getcontentlength>4096</d:getcontentlength>
                  <d:getlastmodified>Tue, 23 Feb 2026 10:00:00 GMT</d:getlastmodified>
                </d:prop></d:propstat>
              </d:response>
            </d:multistatus>
        """.trimIndent()

        val entries = parseWebDavBackupEntries(xml)
        assertEquals(2, entries.size)
        assertEquals("bilipai-backup-20260223-100000.zip", entries[0].fileName)
        assertEquals(4096L, entries[0].sizeBytes)
    }

    @Test
    fun `select latest backup prefers last modified descending`() {
        val entries = listOf(
            WebDavBackupEntry(
                fileName = "bilipai-backup-20260222-090000.zip",
                href = "/a.zip",
                sizeBytes = 2048,
                lastModifiedEpochMs = 1000
            ),
            WebDavBackupEntry(
                fileName = "bilipai-backup-20260223-100000.zip",
                href = "/b.zip",
                sizeBytes = 4096,
                lastModifiedEpochMs = 2000
            )
        )

        val latest = selectLatestWebDavBackup(entries)
        assertNotNull(latest)
        assertEquals("bilipai-backup-20260223-100000.zip", latest?.fileName)
    }

    @Test
    fun `resolve download url should use origin when href is absolute path`() {
        val url = resolveWebDavDownloadUrl(
            baseUrl = "https://dav.example.com/remote.php/dav/files/test",
            hrefOrPath = "/remote.php/dav/files/test/BiliPai/backups/a.zip"
        )

        assertEquals(
            "https://dav.example.com/remote.php/dav/files/test/BiliPai/backups/a.zip",
            url
        )
    }
}
