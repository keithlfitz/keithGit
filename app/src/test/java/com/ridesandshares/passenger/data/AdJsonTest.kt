package com.ridesandshares.passenger.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdJsonTest {
    @Test
    fun parsesACatalogList() {
        val ads = AdJson.parse(
            """
            [
              {
                "id": "harbor-rye",
                "businessName": "Harbor & Rye",
                "tagline": "Espresso nearby.",
                "image": "images/harbor-rye.png",
                "infoUrl": "https://ridesandshares.example/a/harbor-rye"
              }
            ]
            """.trimIndent(),
        )

        assertEquals(1, ads.size)
        assertEquals("harbor-rye", ads[0].id)
        assertEquals("Harbor & Rye", ads[0].businessName)
        assertEquals("Espresso nearby.", ads[0].tagline)
        assertEquals("images/harbor-rye.png", ads[0].image)
        assertEquals("https://ridesandshares.example/a/harbor-rye", ads[0].infoUrl)
    }

    @Test
    fun ignoresUnknownFields() {
        val ads = AdJson.parse(
            """
            [{"id":"a","businessName":"A","tagline":"t","image":"i.png","infoUrl":"https://example.com/a","extra":1}]
            """.trimIndent(),
        )
        assertEquals("a", ads.single().id)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsAnObjectInsteadOfAList() {
        AdJson.parse("""{"id":"a"}""")
    }

    @Test
    fun reportsMissingFieldsAndNonHttpsLinks() {
        val problems = Advertisement(
            id = "cafe",
            businessName = "",
            tagline = "Hi",
            image = "images/cafe.png",
            infoUrl = "http://insecure.example/cafe",
        ).problems()

        assertTrue(problems.any { it.contains("businessName") })
        assertTrue(problems.any { it.contains("https") })
    }

    @Test
    fun catalogFromDropsInvalidAds() {
        val catalog = Catalog.from(
            listOf(
                Advertisement("ok", "Ok", "Line", "a.png", "https://example.com/ok"),
                Advertisement("bad", "Bad", "Line", "b.png", "notaurl"),
            ),
        )
        assertTrue(catalog is Catalog.Invalid)
    }
}
