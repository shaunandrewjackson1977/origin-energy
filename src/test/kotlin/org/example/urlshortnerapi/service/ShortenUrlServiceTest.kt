package org.example.urlshortnerapi.service

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class ShortenUrlServiceTest {
    private val shortenUrlService = ShortenUrlService()

    @Test
    fun `should shorten url`() {
        val originalUrl = "https://www.originenergy.com.au/electricity-gas/plans.html"

        val slug = shortenUrlService.shorten(originalUrl)

        assertThat(slug).hasSize(7)
    }

    @Test
    fun `should return same slug when same url is shortened more than once`() {
        val url = "https://www.example.com"

        val slug1 = shortenUrlService.shorten(url)
        val slug2 = shortenUrlService.shorten(url)

        assertThat(slug1).isEqualTo(slug2)
    }

    @Test
    fun `should resolve a shortened url`() {
        val url = "https://www.example.com"
        val slug = shortenUrlService.shorten(url)

        val resolved = shortenUrlService.resolve(slug)

        assertThat(resolved?.originalUrl).isEqualTo(url)
    }

    @Test
    fun `should return null when slug does not exist`() {
        val resolved = shortenUrlService.resolve("unknown")

        assertThat(resolved).isNull()
    }

    @Test
    fun `should return different slugs for different urls`() {
        val slug1 = shortenUrlService.shorten("https://www.example.com")
        val slug2 = shortenUrlService.shorten("https://www.other.com")

        assertThat(slug1).isNotEqualTo(slug2)
    }
}