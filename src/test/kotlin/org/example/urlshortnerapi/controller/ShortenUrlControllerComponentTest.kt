package org.example.urlshortnerapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.example.urlshortnerapi.model.ShortenUrlRequest
import org.example.urlshortnerapi.model.ShortenUrlResponse
import org.example.urlshortnerapi.model.UrlInfoResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.client.RestTestClient
import tools.jackson.databind.ObjectMapper
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class ShortenUrlControllerComponentTest(
    @Autowired private val client: RestTestClient,
    @Autowired private val objectMapper: ObjectMapper
) {
    private val validUrl = "https://www.originenergy.com.au/electricity-gas/plans.html"

    @Nested
    inner class UrlShorteningTests {
        @Test
        fun `should return new shortened url when original url valid`() {
            val request = objectMapper.writeValueAsString(ShortenUrlRequest(validUrl))

            val result = client.post()
                .uri("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchangeSuccessfully()
                .returnResult()

            val response = objectMapper.readValue(String(result.responseBodyContent), ShortenUrlResponse::class.java)

            assertThat(response.slug).isNotBlank
            assertThat(response.url).isEqualTo("http://short.ly/${response.slug}")
        }
    }

    @Nested
    inner class GetUrlInfoTests {
        private lateinit var slug: String

        @BeforeEach
        fun setUp() {
            val request = objectMapper.writeValueAsString(ShortenUrlRequest(validUrl))

            val postResult = client.post()
                .uri("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchangeSuccessfully()
                .returnResult()

            val postResponse = objectMapper.readValue(String(postResult.responseBodyContent), ShortenUrlResponse::class.java)

            slug = postResponse.slug
        }

        @Test
        fun `should return url info when slug resource valid`() {
            val getResult = client.get()
                .uri("/api/v1/urls/{slug}", slug)
                .exchange()
                .expectStatus().isOk
                .returnResult()

            val getResponse = objectMapper.readValue(String(getResult.responseBodyContent), UrlInfoResponse::class.java)

            assertThat(getResponse.shortenedUrl).isEqualTo("http://short.ly/$slug")
            assertThat(getResponse.originalUrl).isEqualTo(validUrl)
            assertThat(getResponse.createdAt).isBefore(Instant.now())
        }
    }

    @Nested
    inner class RedirectShortUrlTests {
        private lateinit var slug: String

        @BeforeEach
        fun setUp() {
            val request = objectMapper.writeValueAsString(ShortenUrlRequest(validUrl))

            val postResult = client.post()
                .uri("/api/v1/urls")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchangeSuccessfully()
                .returnResult()

            val postResponse = objectMapper.readValue(String(postResult.responseBodyContent), ShortenUrlResponse::class.java)

            slug = postResponse.slug
        }

        @Test
        fun `should redirect to original url when slug resource valid`() {
            client.get()
                .uri("/{slug}", slug)
                .exchange()
                .expectStatus().is3xxRedirection
                .expectHeader().location(validUrl)
                .expectHeader().contentLength(0)
        }
    }

}
