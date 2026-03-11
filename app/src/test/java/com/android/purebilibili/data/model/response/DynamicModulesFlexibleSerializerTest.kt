package com.android.purebilibili.data.model.response

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class DynamicModulesFlexibleSerializerTest {

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Test
    fun dynamicDetailResponse_parsesModulesWhenModulesIsArray() {
        val payload = """
            {
              "code": 0,
              "data": {
                "item": {
                  "id_str": "172792986898006024",
                  "modules": [
                    {
                      "module_author": {
                        "mid": 123456,
                        "name": "tester"
                      }
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<DynamicDetailResponse>(payload)

        assertEquals(123456L, response.data?.item?.modules?.module_author?.mid)
    }

    @Test
    fun dynamicDetailResponse_parsesModulesWhenModulesIsObject() {
        val payload = """
            {
              "code": 0,
              "data": {
                "item": {
                  "id_str": "172792986898006024",
                  "modules": {
                    "module_author": {
                      "mid": 654321,
                      "name": "tester2"
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<DynamicDetailResponse>(payload)

        assertEquals(654321L, response.data?.item?.modules?.module_author?.mid)
    }

    @Test
    fun dynamicDetailResponse_mergesModulesWhenModulesIsArrayFragments() {
        val payload = """
            {
              "code": 0,
              "data": {
                "item": {
                  "id_str": "172792986898006024",
                  "modules": [
                    {
                      "module_author": {
                        "mid": 123456,
                        "name": "author"
                      }
                    },
                    {
                      "module_dynamic": {
                        "desc": {
                          "text": "hello world"
                        }
                      }
                    },
                    {
                      "module_stat": {
                        "comment": { "count": 7 },
                        "forward": { "count": 3 },
                        "like": { "count": 11 }
                      }
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<DynamicDetailResponse>(payload)
        val modules = response.data?.item?.modules

        assertEquals(123456L, modules?.module_author?.mid)
        assertEquals("hello world", modules?.module_dynamic?.desc?.text)
        assertEquals(7, modules?.module_stat?.comment?.count)
        assertEquals(3, modules?.module_stat?.forward?.count)
        assertEquals(11, modules?.module_stat?.like?.count)
    }

    @Test
    fun dynamicDetailResponse_buildsRenderableContentFromOpusModulesArray() {
        val payload = """
            {
              "code": 0,
              "data": {
                "item": {
                  "id_str": "172792986898006024",
                  "modules": [
                    {
                      "module_type": "MODULE_TYPE_TITLE",
                      "module_title": {
                        "text": "标题A"
                      }
                    },
                    {
                      "module_type": "MODULE_TYPE_CONTENT",
                      "module_content": {
                        "paragraphs": [
                          {
                            "para_type": 1,
                            "text": {
                              "nodes": [
                                {
                                  "word": {
                                    "words": "第一段"
                                  }
                                },
                                {
                                  "word": {
                                    "words": "第二段"
                                  }
                                }
                              ]
                            }
                          },
                          {
                            "para_type": 2,
                            "pic": {
                              "pics": [
                                {
                                  "url": "https://i0.hdslb.com/pic1.jpg",
                                  "width": 1200,
                                  "height": 800
                                }
                              ]
                            }
                          }
                        ]
                      }
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<DynamicDetailResponse>(payload)
        val modules = response.data?.item?.modules

        assertEquals("第一段第二段", modules?.module_dynamic?.desc?.text)
        assertEquals("MAJOR_TYPE_OPUS", modules?.module_dynamic?.major?.type)
        assertEquals("标题A", modules?.module_dynamic?.major?.opus?.title)
        assertEquals(1, modules?.module_dynamic?.major?.opus?.pics?.size)
        assertEquals(
            "https://i0.hdslb.com/pic1.jpg",
            modules?.module_dynamic?.major?.opus?.pics?.firstOrNull()?.url
        )
    }

    @Test
    fun dynamicDetailResponse_prefersFullOpusParagraphsOverPreviewDescWhenBothExist() {
        val payload = """
            {
              "code": 0,
              "data": {
                "item": {
                  "id_str": "172792986898006024",
                  "modules": [
                    {
                      "module_dynamic": {
                        "desc": {
                          "text": "预览摘要"
                        },
                        "major": {
                          "type": "MAJOR_TYPE_OPUS",
                          "opus": {
                            "summary": {
                              "text": "预览摘要"
                            }
                          }
                        }
                      }
                    },
                    {
                      "module_type": "MODULE_TYPE_TITLE",
                      "module_title": {
                        "text": "完整标题"
                      }
                    },
                    {
                      "module_type": "MODULE_TYPE_CONTENT",
                      "module_content": {
                        "paragraphs": [
                          {
                            "para_type": 1,
                            "text": {
                              "nodes": [
                                {
                                  "word": {
                                    "words": "第一段完整内容"
                                  }
                                }
                              ]
                            }
                          },
                          {
                            "para_type": 1,
                            "text": {
                              "nodes": [
                                {
                                  "word": {
                                    "words": "第二段完整内容"
                                  }
                                }
                              ]
                            }
                          }
                        ]
                      }
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val response = json.decodeFromString<DynamicDetailResponse>(payload)
        val modules = response.data?.item?.modules

        assertEquals("第一段完整内容\n第二段完整内容", modules?.module_dynamic?.desc?.text)
        assertEquals("完整标题", modules?.module_dynamic?.major?.opus?.title)
        assertEquals("第一段完整内容\n第二段完整内容", modules?.module_dynamic?.major?.opus?.summary?.text)
    }
}
