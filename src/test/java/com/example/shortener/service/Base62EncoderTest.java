package com.example.shortener.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class Base62EncoderTest {
    private final Base62Encoder encoder = new Base62Encoder();

    @Test
    void encodesZeroAndPositiveIds() {
        assertEquals("0", encoder.encode(0));
        assertEquals("10", encoder.encode(62));
        assertEquals("Z", encoder.encode(61));
    }
}
