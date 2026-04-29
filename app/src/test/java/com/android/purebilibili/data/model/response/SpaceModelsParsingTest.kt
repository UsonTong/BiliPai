package com.android.purebilibili.data.model.response

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class SpaceModelsParsingTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Test
    fun decodeSpaceArticleResponse_acceptsWbiArticleShape() {
        val payload = """
            {
              "code": 0,
              "message": "0",
              "data": {
                "articles": [
                  {
                    "id": 123,
                    "title": "专栏标题",
                    "summary": "摘要",
                    "image_urls": ["https://i0.hdslb.com/bfs/article/a.jpg"],
                    "stats": {
                      "view": 456,
                      "like": 78
                    }
                  }
                ],
                "count": 9
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<SpaceArticleResponse>(payload)
        val article = response.data?.lists?.single()

        assertEquals(9, response.data?.total)
        assertEquals(123L, article?.id)
        assertEquals("专栏标题", article?.title)
        assertEquals(456, article?.stats?.view)
        assertEquals(78, article?.stats?.like)
        assertEquals(listOf("https://i0.hdslb.com/bfs/article/a.jpg"), article?.displayImageUrls())
    }

    @Test
    fun decodeSpaceArticleResponse_acceptsOpusFeedShapeWithoutBlankRows() {
        val payload = """
            {
              "code": 0,
              "message": "0",
              "data": {
                "items": [
                  {
                    "opus_id": "1056353752004427792",
                    "content": "通过 DevTools 绕过 SSR 抓包某站专栏正文接口",
                    "cover": {
                      "url": "http://i0.hdslb.com/bfs/article/cover.jpg"
                    },
                    "stat": {
                      "like": "3",
                      "view": "120"
                    }
                  }
                ],
                "has_more": true
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<SpaceArticleResponse>(payload)
        val article = response.data?.lists?.single()

        assertEquals(1056353752004427792L, article?.id)
        assertEquals("通过 DevTools 绕过 SSR 抓包某站专栏正文接口", article?.title)
        assertEquals(120, article?.stats?.view)
        assertEquals(3, article?.stats?.like)
        assertEquals(listOf("http://i0.hdslb.com/bfs/article/cover.jpg"), article?.displayImageUrls())
    }
}
