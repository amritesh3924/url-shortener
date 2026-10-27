package com.example.shortener.service;

import org.junit.jupiter.api.Test;
import java.util.HashSet;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class Base62EncoderTest {
    private final Base62Encoder encoder = new Base62Encoder();

    @Test
    void encodesZeroAndPositiveIds() {
        assertEquals("0", encoder.encode(0));
        assertEquals("1", encoder.encode(1));
        assertEquals("1C", encoder.encode(100));
        assertEquals("10", encoder.encode(62));
        assertEquals("Z", encoder.encode(61));
    }

    @Test
    void rejectsNegativeIds() {
        assertThrows(IllegalArgumentException.class, () -> encoder.encode(-1));
    }

    @Test
    void encodesUniqueCodesForFirstFiveThousandIds() {
        Set<String> codes = new HashSet<>();
        for (long id = 1; id <= 5000; id++) {
            assertTrue(codes.add(encoder.encode(id)), "Duplicate code for ID " + id);
        }
    }
}
