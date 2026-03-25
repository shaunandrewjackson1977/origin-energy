package org.example.urlshortnerapi.model

import java.time.Instant

data class UrlInfoResponse(
    val shortenedUrl: String,
    val originalUrl: String,
    val createdAt: Instant
)
