package org.example.urlshortnerapi.controller

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.example.urlshortnerapi.model.ShortenUrlRequest
import org.example.urlshortnerapi.model.ShortenUrlResponse
import org.example.urlshortnerapi.model.UrlInfoResponse
import org.example.urlshortnerapi.service.ShortenUrlService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.net.URI

@RestController
@RequestMapping("/api/v1")
class ShortenUrlController(@Autowired private val shortenUrlService: ShortenUrlService) {
    @PostMapping("/shorten")
    fun shortenOriginalUrl(@RequestBody @Valid request: ShortenUrlRequest): ResponseEntity<ShortenUrlResponse> {
        logger.info("Shorten url service request: $request")
        val slug = shortenUrlService.shorten(request.url)
        val shortenedUrl = buildShortenedUrl(slug)
        return ResponseEntity.ok().body(ShortenUrlResponse(slug, shortenedUrl))
            .also { logger.info("Shorten url service response: $it") }
    }

    @GetMapping("/redirect")
    fun redirectToOriginalUrlViaRequestParam(@RequestParam @NotBlank slug: String): ResponseEntity<Unit> {
        return doRedirectToOriginalUrl(slug)
    }

    @GetMapping("/redirect/{slug}")
    fun redirectToOriginalUrlViaPathVariable(@PathVariable @NotBlank slug: String): ResponseEntity<Unit> {
        return doRedirectToOriginalUrl(slug)
    }

    @GetMapping("/shorten")
    fun getOriginalUrlViaRequestParam(@RequestParam @NotBlank slug: String): ResponseEntity<UrlInfoResponse> {
        return doGetOriginalUrl(slug)
    }

    @GetMapping("/shorten/{slug}")
    fun getOriginalUrlViaPathVariable(@PathVariable @NotBlank slug: String): ResponseEntity<UrlInfoResponse> {
        return doGetOriginalUrl(slug)
    }

    private fun doRedirectToOriginalUrl(slug: String): ResponseEntity<Unit> {
        logger.info("Redirect url service request: $slug")
        val urlDetails = shortenUrlService.resolve(slug) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        return ResponseEntity
            .status(HttpStatus.FOUND)
            .location(URI.create(urlDetails.originalUrl))
            .build()
    }

    private fun doGetOriginalUrl(slug: String): ResponseEntity<UrlInfoResponse> {
        logger.info("Get url info request: $slug")
        val urlDetails = shortenUrlService.resolve(slug) ?: throw ResponseStatusException(HttpStatus.NOT_FOUND)
        return ResponseEntity
            .ok()
            .body(
                UrlInfoResponse(
                    shortenedUrl = buildShortenedUrl(urlDetails.slug),
                    originalUrl = urlDetails.originalUrl,
                    createdAt = urlDetails.createdAt
                )
            ).also { logger.info("Get url info response: $it") }
    }

    private fun buildShortenedUrl(slug: String): String = """http://short.ly/${slug}"""

    companion object {
        private val logger = LoggerFactory.getLogger(ShortenUrlController::class.java)
    }
}