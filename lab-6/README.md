# Лабораторная работа 6 — CI/CD (GitHub Actions)

Нечаев Игорь Сергеевич, 334772

Расширение сервиса Pomodoro Timer (lab-5) с настроенным CI/CD через GitHub Actions.

## Что добавлено

### CI/CD pipeline (`.github/workflows/lab-6-ci.yml`)

Три job'а, выполняющихся последовательно (docker ждёт lint + test):

| Job | Что делает |
|-----|-----------|
| **lint** | Проверяет Kotlin-код линтером ktlint (ручные файлы: `delegate/`, `entity/`, `repository/`) |
| **test** | Запускает 21 unit-тест без базы данных |
| **docker** | Собирает образ, при пуше в `master` публикует в GHCR |

Триггеры: `push`/`pull_request` в `lab-6` и `master` при изменениях в `lab-6/**`.

### Тесты (`src/test/kotlin/`)

**TimerEntityTest** (7 тестов) — логика `remainingSeconds()` и `accumulateElapsed()`:
- корректный расчёт оставшегося времени для разных статусов
- накопление elapsed при паузе
- защита от отрицательного остатка

**TimersApiDelegateImplTest** (14 тестов) — переходы состояний и HTTP-статусы через Mockito-мок репозитория. Не требуют Spring-контекста и базы данных.

```bash
# Запуск тестов
./mvnw test

# Линтинг
./mvnw com.github.gantsign.maven:ktlint-maven-plugin:check
```

### Multi-stage Dockerfile

Dockerfile переработан на двухэтапную сборку:
- **build**: `eclipse-temurin:17-jdk-jammy` — компиляция и `mvnw package`
- **runtime**: `eclipse-temurin:17-jre-jammy` — только JRE, меньший образ

```bash
# Сборка образа (не требует предварительного ./mvnw package)
docker build -t pomodoro-service .

# Полный стек (PostgreSQL + Prometheus + Grafana + Loki + Tempo)
docker compose up -d
```

## Развёртывание

При пуше в `master` образ автоматически публикуется в GitHub Container Registry:

```
ghcr.io/igor-nechaev/pomodoro-service:latest
ghcr.io/igor-nechaev/pomodoro-service:<commit-sha>
```

Права на публикацию выдаются через `GITHUB_TOKEN` (permissions: `packages: write`).

## Технологии

Всё из lab-5 (Spring Boot 3, PostgreSQL, Prometheus, Grafana, Loki, Alloy, Tempo) + добавлено:

| Зависимость | Версия | Назначение |
|-------------|--------|-----------|
| `mockito-kotlin` | 5.4.0 | Kotlin-friendly API для Mockito в unit-тестах |
| `ktlint-maven-plugin` | 3.4.0 | Линтер Kotlin-кода (стиль кода) |

## Endpoints

| Метод | Путь | Описание |
|-------|------|----------|
| GET | `/timers` | Список всех таймеров |
| POST | `/timers` | Создать таймер |
| GET | `/timers/{id}` | Получить таймер |
| POST | `/timers/{id}/start` | Запустить таймер |
| POST | `/timers/{id}/stop` | Поставить на паузу |
| POST | `/timers/{id}/complete` | Завершить таймер |
