package com.example.logistics.processing.client;

import com.example.logistics.processing.config.CourierClientProperties;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;

import java.util.List;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/**
 * Запрашивает доступных курьеров у C с сервисным JWT и сетевыми таймаутами.
 * Проверяет контракт ответа; ошибки запроса передаются worker для планирования повтора.
 */
@Component
public class CourierClient {

    private final RestClient client;
    private final ServiceTokenProvider tokens;
    private final Validator validator;

    public CourierClient(RestClient.Builder builder, CourierClientProperties properties,
                         ServiceTokenProvider tokens, Validator validator) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        this.client = builder.clone().baseUrl(properties.baseUrl().toString()).requestFactory(factory).build();
        this.tokens = tokens;
        this.validator = validator;
    }

    /** Возвращает доступных курьеров из C; корректный ответ без курьеров даёт пустой список. */
    public List<Courier> getAvailableCouriers() {
        CouriersResponse response;
        try {
            response = client.get().uri("/api/v1/couriers/available")
                .headers(headers -> headers.setBearerAuth(tokens.accessToken()))
                .retrieve().body(CouriersResponse.class);
        } catch (HttpClientErrorException.Unauthorized exception) {
            tokens.invalidate();
            throw new IllegalStateException("Courier provider rejected service authentication");
        }
        if (response == null || !validator.validate(response).isEmpty()) {
            throw new IllegalStateException("Courier provider returned invalid courier data");
        }
        return response.couriers();
    }

    private record CouriersResponse(@NotNull List<@NotNull @Valid Courier> couriers) {
    }
}
