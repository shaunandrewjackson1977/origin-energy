package org.example.urlshortnerapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.example.urlshortnerapi.model.ShortenUrlRequest
import org.example.urlshortnerapi.model.ShortenUrlResponse
import org.example.urlshortnerapi.model.UrlInfoResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import tools.jackson.databind.ObjectMapper
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
class ShortenUrlControllerTest(
    @Autowired private val mvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper
) {
    private val validUrl = "https://www.originenergy.com.au/electricity-gas/plans.html"

    @Nested
    inner class UrlShorteningTests {
        @Test
        fun `should return bad request when original url invalid`() {
            val url = "http://example.com/path with spaces"
            val request = objectMapper.writeValueAsString(ShortenUrlRequest(url))

            mvc.post("/api/v1/shorten") {
                contentType = MediaType.APPLICATION_JSON
                content = request
            }.andExpect {
                status { isBadRequest() }
            }
        }

        @Test
        fun `should return new shortened url when original url valid`() {
            val request = objectMapper.writeValueAsString(ShortenUrlRequest(validUrl))

            val result = mvc.post("/api/v1/shorten") {
                contentType = MediaType.APPLICATION_JSON
                content = request
            }.andExpect {
                status { isOk() }
            }.andReturn()

            val response = objectMapper.readValue(result.response.contentAsString, ShortenUrlResponse::class.java)

            assertThat(response.slug).isNotBlank.hasSize(7)
            assertThat(response.url).isEqualTo("http://short.ly/${response.slug}")
        }

        @Test
        fun `should return same slug for same original url`() {
            val request = objectMapper.writeValueAsString(ShortenUrlRequest("https://www.originenergy.com.au/internet/"))

            val result = mvc.post("/api/v1/shorten") {
                contentType = MediaType.APPLICATION_JSON
                content = request
            }.andExpect {
                status { isOk() }
            }.andReturn()

            val response = objectMapper.readValue(result.response.contentAsString, ShortenUrlResponse::class.java)

            assertThat(response.slug).isNotBlank.hasSize(7)
            assertThat(response.url).isEqualTo("http://short.ly/${response.slug}")

            val idempotent = mvc.post("/api/v1/shorten") {
                contentType = MediaType.APPLICATION_JSON
                content = request
            }.andExpect {
                status { isOk() }
            }.andReturn()

            assertThat(objectMapper.readValue(idempotent.response.contentAsString, ShortenUrlResponse::class.java))
                .isEqualTo(response)
        }
    }

    @Nested
    inner class GetUrlInfoTests {
        private lateinit var slug: String

        @BeforeEach
        fun setUp() {
            val request = objectMapper.writeValueAsString(ShortenUrlRequest(validUrl))

            val result = mvc.post("/api/v1/shorten") {
                contentType = MediaType.APPLICATION_JSON
                content = request
            }.andExpect {
                status { isOk() }
            }.andReturn()

            val response = objectMapper.readValue(result.response.contentAsString, ShortenUrlResponse::class.java)

            slug = response.slug
        }

        @Test
        fun `should return bad request when shortened url slug empty`() {
            mvc.get("/api/v1/shorten") {
                accept(MediaType.APPLICATION_JSON)
                param("slug", "")
            }.andExpect {
                status { isBadRequest() }
            }.andReturn()
        }

        @Test
        fun `should return bad request when shortened url slug blank`() {
            mvc.get("/api/v1/shorten/{slug}", "      ") {
                accept(MediaType.APPLICATION_JSON)
            }.andExpect {
                status { isBadRequest() }
            }.andReturn()
        }

        @Test
        fun `should return not found when slug resource invalid`() {
            mvc.get("/api/v1/shorten/{slug}", "abc1234") {
                accept(MediaType.APPLICATION_JSON)
            }.andExpect {
                status { isNotFound() }
            }.andReturn()
        }

        @Test
        fun `should return url info when slug resource valid`() {
            val result = mvc.get("/api/v1/shorten/{slug}", slug) {
                accept(MediaType.APPLICATION_JSON)
            }.andExpect {
                status { isOk() }
            }.andReturn()

            val response = objectMapper.readValue(result.response.contentAsString, UrlInfoResponse::class.java)

            assertThat(response.shortenedUrl).isEqualTo("http://short.ly/$slug")
            assertThat(response.originalUrl).isEqualTo(validUrl)
            assertThat(response.createdAt).isBefore(Instant.now())
        }
    }

    @Nested
    inner class RedirectShortUrlTests {
        @Test
        fun `should return bad request when shortened url slug empty`() {
            mvc.get("/api/v1/redirect") {
                accept(MediaType.APPLICATION_JSON)
                param("slug", "")
            }.andExpect {
                status { isBadRequest() }
            }
        }

        @Test
        fun `should return bad request when shortened url slug blank`() {
            mvc.get("/api/v1/redirect/{slug}", "       ") {
                accept(MediaType.APPLICATION_JSON)
            }.andExpect {
                status { isBadRequest() }
            }
        }

        @Test
        fun `should return not found when shortened url slug resource invalid`() {
            mvc.get("/api/v1/redirect/{slug}", "abc1234") {
                accept(MediaType.APPLICATION_JSON)
            }.andExpect {
                status { isNotFound() }
            }
        }

        @Test
        fun `should redirect to original url for valid slug resource`() {
            val url = "https://www.originenergy.com.au/internet/"
            val request = objectMapper.writeValueAsString(ShortenUrlRequest(url))

            val result = mvc.post("/api/v1/shorten") {
                contentType = MediaType.APPLICATION_JSON
                content = request
            }.andExpect {
                status { isOk() }
            }.andReturn()

            val response = objectMapper.readValue(result.response.contentAsString, ShortenUrlResponse::class.java)

            mvc.get("/api/v1/redirect/{slug}", response.slug)
                .andExpect {
                    status { isFound() }
                }
        }
    }

}