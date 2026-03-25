package org.example.urlshortnerapi.service.domain

import java.time.Instant

data class ShortUrlDetails(val originalUrl: String, val slug: String, val createdAt: Instant = Instant.now())
