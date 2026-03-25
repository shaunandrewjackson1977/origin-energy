package org.example.urlshortnerapi.model

import org.hibernate.validator.constraints.URL

data class ShortenUrlRequest(@URL val url: String)
