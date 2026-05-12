# Лабораторная работа 5 - Распределённая трассировка (OpenTelemetry + Tempo + Grafana)

Расширение сервиса Pomodoro Timer из лаб 2-4: к стеку метрик (Prometheus + Grafana) и логов (Loki + Alloy) добавлена
**распределённая трассировка** на базе **OpenTelemetry → OTLP → Grafana Tempo → Grafana (TraceQL)**.

Также по фидбеку преподавателя сборщик логов `Promtail` заменён на его наследника **Grafana Alloy** (новый агент
Grafana Labs на синтаксисе River, единый для логов / метрик / трейсов).

Подробности про архитектуру трассировки, генерируемые spanы и язык запросов TraceQL - в [TRACES.md](TRACES.md).
Логи (LogQL, дашборды) - в [LOGS.md](LOGS.md).

**Автор:** Нечаев Игорь Сергеевич, 334772

## Что нового по сравнению с lab-4

| Компонент                | lab-4                       | lab-5                                                       |
|--------------------------|-----------------------------|-------------------------------------------------------------|
| Сборщик логов            | `grafana/promtail:3.1.0`    | `grafana/alloy:v1.3.1` (River-конфиг)                       |
| Tracing backend          | -                           | `grafana/tempo:2.5.0` (OTLP/HTTP, локальное хранилище)      |
| Tracing SDK              | -                           | `micrometer-tracing-bridge-otel` + `opentelemetry-exporter` |
| Grafana datasources      | Prometheus, Loki            | Prometheus, Loki, **Tempo** + traces↔logs корреляция        |
| Бизнес-spanы             | -                           | вручную, через `Observation` API в `TimersApiDelegateImpl`  |

## Стек технологий

- Kotlin 2.1.10 + Spring Boot 3.5.13
- Spring Boot Actuator + Micrometer
- **Micrometer Tracing → OpenTelemetry SDK → OTLP/HTTP**
- **Grafana Tempo 2.5** (TSDB трейсов, TraceQL)
- Grafana Loki 3.1 (логи, LogQL)
- **Grafana Alloy 1.3** (наследник Promtail; сбор docker-логов)
- Prometheus (метрики, PromQL)
- Grafana 11 (визуализация, provisioning)
- Spring Data JPA + PostgreSQL
- OpenAPI Generator (kotlin-spring)
- Docker Compose, Maven

## Архитектура

```
┌──────────────────────────┐
│      pomodoro-app        │
│  (Spring Boot + OTel)    │
│                          │
│  /actuator/prometheus ───┼──▶ Prometheus ──┐
│                          │                 │
│  stdout (logs) ──────────┼──▶ Alloy ──▶ Loki ──┤
│                          │                 │
│  OTLP/HTTP :4318 ────────┼──▶ Tempo ───────┤
│                          │                 │
└──────────────────────────┘                 ▼
                                         Grafana
                                  (PromQL / LogQL / TraceQL)
```

## Распределённая трассировка

Каждый HTTP-запрос порождает **многоуровневое дерево spanов** благодаря тому, что в делегате бизнес-операции
обёрнуты в именованные `Observation`. Пример для `POST /timers`:

```
http post /timers                          ← root span (Spring MVC)
└─ pomodoro.create-timer                   ← бизнес-операция
   ├─ pomodoro.entity.build
   ├─ pomodoro.repository.save
   │  └─ connection                        ← Hikari acquire (auto, datasource-micrometer)
   │     ├─ query  (INSERT INTO timers …)  ← JDBC span (auto)
   │     └─ generated-keys                 ← JDBC span (auto)
   └─ pomodoro.metrics.record
```

8 spanов на один HTTP-запрос, дерево глубиной 4. Это и есть «трейс с ≥ 2 spanами», требуемый по заданию -
с большим запасом.

Полный список бизнес-spanов:

| Эндпоинт                   | Дочерние spanы                                                          |
|----------------------------|-------------------------------------------------------------------------|
| `GET    /timers`           | `pomodoro.list-timers` → `…repository.find-all`, `…toDto.batch`         |
| `POST   /timers`           | `pomodoro.create-timer` → `entity.build`, `repository.save`, `metrics.record` |
| `GET    /timers/{id}`      | `pomodoro.get-timer` → `repository.find-by-id`                          |
| `POST   /timers/{id}/start`| `pomodoro.start-timer` → `state.validate`, `state.transition.running`   |
| `POST   /timers/{id}/stop` | `pomodoro.stop-timer`  → `state.validate`, `state.transition.paused`    |
| `POST   /timers/{id}/complete` | `pomodoro.complete-timer` → `state.validate`, `state.transition.completed` |

К некоторым спанам приклеены атрибуты (`timer.id`, `timer.duration_minutes`, `timer.current_status`,
`timer.elapsed_seconds`) - по ним можно фильтровать в TraceQL.

## Запуск

### Требования

- Java 17+
- Docker
- Maven (или встроенный `./mvnw`)

### 1. Сборка и запуск стека

```bash
cd lab-5
./mvnw clean package
docker compose up -d --build
```

Поднимутся: PostgreSQL, само приложение, Prometheus, Grafana, Loki, Alloy, Tempo.

### 2. Что доступно

| Что                | URL                                                  |
|--------------------|------------------------------------------------------|
| Приложение         | http://localhost:8080                                |
| Swagger UI         | http://localhost:8080/swagger-ui/index.html          |
| Метрики (raw)      | http://localhost:8080/actuator/prometheus            |
| Prometheus         | http://localhost:9090                                |
| Loki API           | http://localhost:3100                                |
| Tempo HTTP API     | http://localhost:3200                                |
| Tempo OTLP/HTTP    | http://localhost:4318/v1/traces                      |
| Alloy UI           | http://localhost:12345                               |
| Grafana            | http://localhost:3000 (admin/admin)                  |

### 3. Сгенерировать трейсы

```bash
TIMER_ID=$(curl -s -X POST http://localhost:8080/timers \
    -H 'Content-Type: application/json' \
    -d '{"name":"deep work","durationMinutes":25}' | jq .id)

curl -X POST http://localhost:8080/timers/$TIMER_ID/start
curl -X POST http://localhost:8080/timers/$TIMER_ID/stop
curl -X POST http://localhost:8080/timers/$TIMER_ID/complete

# трейсы с ошибками (status=error в TraceQL):
curl http://localhost:8080/timers/999999
curl -X POST http://localhost:8080/timers/$TIMER_ID/complete   # 409
```

### 4. Открыть трейсы в Grafana

Grafana → **Explore** → datasource **Tempo** → **TraceQL** →

```traceql
{ resource.service.name = "pomodoro-service" }
```

Кликнуть на любую строку результата - раскроется дерево spanов с временной шкалой.

Полный список TraceQL-запросов (фильтрация по эндпоинту, по атрибуту, по ошибкам, по длительности и т. д.) - в
[TRACES.md](TRACES.md).

## Что осталось от прошлых лаб

- **Метрики (lab-3):** `/actuator/prometheus`, дашборд `Pomodoro Timer Service` - без изменений. Подробнее в разделе
  ниже.
- **Логи (lab-4):** структурированные `INFO BUSINESS` / `WARN reason=…` / `ERROR`, Loki, LogQL. Поменялся только
  агент: `Promtail → Alloy`. Все LogQL-запросы и дашборд логов работают как раньше - см. [LOGS.md](LOGS.md).

## Продуктовые метрики (из lab-3, без изменений)

| Метрика в Prometheus                           | Тип       | Что показывает                                  |
|------------------------------------------------|-----------|-------------------------------------------------|
| `pomodoro_op_create_total`                     | Counter   | Сколько таймеров создано                        |
| `pomodoro_op_start_total`                      | Counter   | Сколько раз запускали таймеры                   |
| `pomodoro_op_stop_total`                       | Counter   | Сколько раз ставили на паузу                    |
| `pomodoro_op_complete_total{reason="manual"}`  | Counter   | Завершены вручную                               |
| `pomodoro_op_complete_total{reason="expired"}` | Counter   | Завершены автоматически                         |
| `pomodoro_op_error_total{type="not_found"}`    | Counter   | Обращение к несуществующему таймеру             |
| `pomodoro_op_error_total{type="conflict"}`     | Counter   | Попытка недопустимого перехода состояния        |
| `pomodoro_gauge_running`                       | Gauge     | Сколько таймеров сейчас запущено                |
| `pomodoro_gauge_pending`                       | Gauge     | Сколько таймеров ожидают запуска                |
| `pomodoro_gauge_all`                           | Gauge     | Общее число таймеров в системе                  |
| `pomodoro_gauge_completion_rate`               | Gauge     | Доля завершённых от общего числа (0.0-1.0)      |
| `pomodoro_timer_duration_minutes`              | Histogram | Распределение длительности создаваемых таймеров |

## Файлы инфраструктуры

```
docker-compose.yml                 PG + app + Prometheus + Grafana + Loki + Alloy + Tempo
prometheus.yml                     scrape /actuator/prometheus
loki-config.yml                    single-instance Loki (filesystem)
alloy-config.alloy                 docker discovery → Loki  (заменил promtail-config.yml)
tempo-config.yml                   Tempo: OTLP/HTTP + local storage
grafana/provisioning/
├── datasources/prometheus.yml     Prometheus + Loki + Tempo (с traces logs корреляцией)
└── dashboards/
    ├── dashboards.yml             провайдер
    └── pomodoro.json              дашборд метрик
```

## Связанные документы

- [TRACES.md](TRACES.md) - теория трассировки, описание spanов, TraceQL-запросы
- [LOGS.md](LOGS.md) - логи (LogQL, формат сообщений, дашборд)
