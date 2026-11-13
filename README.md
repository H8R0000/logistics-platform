# Logistics Platform

Тестовое задание: система управления заявками на доставку.
Java 21, Spring Boot, PostgreSQL, Kafka, Keycloak; интерфейс-- Vue 3 и Vite.

Т.к. UI не был основной целью задания -- решил его создать частично с помощью ИИ :)

## Запуск backend

Перед запуском заполните `.env` по образцу `.env.example`.

```bash
docker compose up -d --build
```

Management: `localhost:8081`, processing: `localhost:8082`, courier-provider: `localhost:8083`.
Keycloak: `localhost:8080`.

## Запуск UI

```bash
cd frontend
npm ci
npm run dev
```

UI: `http://localhost:5173`.
Примеры запросов для IntelliJ HTTP Client: [examples/requests.http](examples/requests.http).
