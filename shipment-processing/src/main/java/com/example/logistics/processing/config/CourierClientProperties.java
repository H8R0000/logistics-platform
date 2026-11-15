package com.example.logistics.processing.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("courier-client")
public record CourierClientProperties(@NotNull URI baseUrl, @NotNull URI tokenUri,
                                      @NotBlank String clientId, @NotBlank String clientSecret,
                                      @NotNull Duration connectTimeout, @NotNull Duration readTimeout) {

    @AssertTrue(message = "courier-client requires HTTP(S) URLs and timeouts from 1ms to 30s")
    public boolean isConfigurationValid() {
        return http(baseUrl) && http(tokenUri) && timeout(connectTimeout) && timeout(readTimeout);
    }

    private static boolean http(URI uri) {
        return uri != null && uri.getHost() != null
                && ("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()));
    }

    private static boolean timeout(Duration duration) {
        return duration != null && !duration.isNegative()
                && duration.toMillis() >= 1 && duration.compareTo(Duration.ofSeconds(30)) <= 0;
    }

    @Override
    public String toString() {
        return "CourierClientProperties[baseUrl=" + baseUrl + ", clientId=" + clientId + ", clientSecret=REDACTED]";
    }
}
