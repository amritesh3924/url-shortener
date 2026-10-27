package com.example.shortener.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UrlValidatorTest {
    private final UrlValidator validator = new UrlValidator();

    @Test
    void allowsOnlyValidHttpUrls() {
        assertTrue(validator.isValid("https://example.com/path"));
        assertTrue(validator.isValid("http://example.com"));
        assertFalse(validator.isValid("javascript:alert(1)"));
        assertFalse(validator.isValid("https:///missing-host"));
        assertFalse(validator.isValid("https://example.com/has space"));
    }
}
