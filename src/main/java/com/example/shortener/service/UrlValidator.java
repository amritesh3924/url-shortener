package com.example.shortener.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public class UrlValidator {
    public boolean isValid(String value) {
        if (value == null || value.isBlank() || value.length() > 2048) return false;
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            return scheme != null
                    && (scheme.toLowerCase(Locale.ROOT).equals("http") || scheme.toLowerCase(Locale.ROOT).equals("https"))
                    && uri.getHost() != null;
        } catch (URISyntaxException exception) {
            return false;
        }
    }
}
