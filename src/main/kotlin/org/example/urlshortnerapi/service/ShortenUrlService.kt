package org.example.urlshortnerapi.service

import org.example.urlshortnerapi.service.Base62Encoder.MAX_LONG_SUPPORTED
import org.example.urlshortnerapi.service.domain.ShortUrlDetails
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap

@Component
class ShortenUrlService {
    private val secureRandom = SecureRandom.getInstanceStrong()

    private val shortenedUrls = ConcurrentHashMap<String, ShortUrlDetails>()    // slug -> shortenedUrl
    private val originalUrlIndex = ConcurrentHashMap<String, String>()          // original url to slug

    fun shorten(originalUrl: String): String {
        originalUrlIndex[originalUrl]?.let {
            logger.info("Existing shortened url: $originalUrl, slug: $it")
            return it
        }

        // Collision is astronomically unlikely given there's about 3.5 trillion possible slugs,
        // so this loop will virtually always return on the first iteration.
        while (true) {
            val randomLong = secureRandom.nextLong(MAX_LONG_SUPPORTED) // Max Long that would exceed 7-char base62 limit
            val slug = Base62Encoder.encode(randomLong)
            if (shortenedUrls.putIfAbsent(slug, ShortUrlDetails(originalUrl, slug)) == null) {
                originalUrlIndex[originalUrl] = slug
                return slug
            }
        }
    }

    fun resolve(slug: String): ShortUrlDetails? = shortenedUrls[slug]

    companion object {
        private val logger = LoggerFactory.getLogger(ShortenUrlService::class.java)
    }
}