package com.example.logistics.processing.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.example.logistics.processing.config.CourierClientProperties;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Получает сервисный JWT через client_credentials и кеширует его в памяти до времени обновления.
 * Используется для авторизации B в C независимо от пользовательского JWT.
 */
@Component
public class ServiceTokenProvider {

    private final RestClient client;
    private final CourierClientProperties properties;
    private @Nullable Token token;

    public ServiceTokenProvider(RestClient.Builder builder, CourierClientProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        this.client = builder.clone().requestFactory(factory).build();
    }

    /** Сериализует проверку и обновление кеша, чтобы worker-потоки безопасно использовали общий токен. */
    public synchronized String accessToken() {
        if (token != null && Instant.now().isBefore(token.expiresAt())) {
            return token.value();
        }
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        Instant requestedAt = Instant.now();
        TokenResponse response = client.post().uri(properties.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form)
                .retrieve().body(TokenResponse.class);
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()
                || response.expiresIn() < 1 || !"Bearer".equalsIgnoreCase(response.tokenType())) {
            throw new IllegalStateException("Keycloak returned an invalid service token response");
        }
        long lifetime = Math.max(1, response.expiresIn() - 30);
        token = new Token(response.accessToken(), requestedAt.plusSeconds(lifetime));
        return token.value();
    }

    public synchronized void invalidate() {
        token = null;
    }

    private record Token(String value, Instant expiresAt) {
    }

    private record TokenResponse(@JsonProperty("access_token") String accessToken,
                                 @JsonProperty("expires_in") long expiresIn,
                                 @JsonProperty("token_type") String tokenType) {
    }
}
