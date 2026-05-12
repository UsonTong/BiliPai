package com.android.purebilibili.data.model.response

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class LiveRoomParsingTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `v3 area room list maps area aliases and cover fallbacks`() {
        val response = json.decodeFromString(
            LiveResponse.serializer(),
            """
            {
              "code": 0,
              "message": "success",
              "data": {
                "count": 438,
                "has_more": 1,
                "list": [
                  {
                    "roomid": 545068,
                    "uid": 8739477,
                    "title": "德云色 17点 AL VS IG！",
                    "uname": "老实憨厚的笑笑",
                    "online": 374244,
                    "cover": "",
                    "user_cover": "",
                    "system_cover": "https://example.com/keyframe.jpg",
                    "face": "https://example.com/face.jpg",
                    "area_v2_parent_name": "网游",
                    "area_v2_name": "英雄联盟"
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val room = response.data?.list?.first()

        assertEquals("英雄联盟", room?.areaName)
        assertEquals("网游", room?.parentName)
        assertEquals("https://example.com/keyframe.jpg", room?.displayCover())
    }

    @Test
    fun `v3 area room list uses watched show when online is zero`() {
        val response = json.decodeFromString(
            LiveResponse.serializer(),
            """
            {
              "code": 0,
              "message": "success",
              "data": {
                "list": [
                  {
                    "roomid": 6,
                    "title": "直播中",
                    "online": 0,
                    "watched_show": {
                      "num": 45678,
                      "text_small": "4.5万"
                    }
                  }
                ]
              }
            }
            """.trimIndent()
        )

        val room = response.data?.list?.first()

        assertEquals(45678, room?.viewerCount())
    }
}
