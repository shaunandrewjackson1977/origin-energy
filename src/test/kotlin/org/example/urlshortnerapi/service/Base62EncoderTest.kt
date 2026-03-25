package org.example.urlshortnerapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class Base62EncoderTest {

    @Test
    fun `should encode long that does not exceed 7-char base62 limit`() {
        val encoded = Base62Encoder.encode(3521614606207L)
        assertThat(encoded).hasSize(7)
    }

    @Test
    fun `should throw when trying to encode long at base62 limit`() {
        assertThatThrownBy { Base62Encoder.encode(3521614606208L) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Long exceeded 7-char base62 limit")
    }

}