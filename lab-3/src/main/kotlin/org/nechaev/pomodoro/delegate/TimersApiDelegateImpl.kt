package org.nechaev.pomodoro.delegate

import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.MeterRegistry
import org.nechaev.pomodoro.api.TimersApiDelegate
import org.nechaev.pomodoro.entity.TimerEntity
import org.nechaev.pomodoro.entity.TimerStatus
import org.nechaev.pomodoro.model.CreateTimerRequest
import org.nechaev.pomodoro.model.Timer
import org.nechaev.pomodoro.repository.TimerRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class TimersApiDelegateImpl(
    private val timerRepository: TimerRepository,
    private val meterRegistry: MeterRegistry
) : TimersApiDelegate {

    // Продуктовые счётчики операций
    private val createdCounter = meterRegistry.counter("pomodoro.op.create")
    private val startedCounter = meterRegistry.counter("pomodoro.op.start")
    private val stoppedCounter = meterRegistry.counter("pomodoro.op.stop")
    private val completedCounter = meterRegistry.counter("pomodoro.op.complete", "reason", "manual")
    private val autoCompletedCounter = meterRegistry.counter("pomodoro.op.complete", "reason", "expired")
    private val notFoundErrors = meterRegistry.counter("pomodoro.op.error", "type", "not_found")
    private val conflictErrors = meterRegistry.counter("pomodoro.op.error", "type", "conflict")

    // Распределение длительности создаваемых таймеров (в минутах)
    private val durationSummary: DistributionSummary = DistributionSummary.builder("pomodoro.timer.duration")
        .description("Длительность создаваемых таймеров в минутах")
        .baseUnit("minutes")
        .publishPercentileHistogram()
        .register(meterRegistry)

    init {
        // Gauge: текущее число активных (RUNNING) таймеров
        meterRegistry.gauge("pomodoro.gauge.running", timerRepository) { repo ->
            repo.countByStatus(TimerStatus.RUNNING).toDouble()
        }
        // Gauge: общее число таймеров в системе
        meterRegistry.gauge("pomodoro.gauge.all", timerRepository) { repo ->
            repo.count().toDouble()
        }
        // Gauge: число таймеров, ожидающих запуска (CREATED)
        meterRegistry.gauge("pomodoro.gauge.pending", timerRepository) { repo ->
            repo.countByStatus(TimerStatus.CREATED).toDouble()
        }
        // Gauge: доля завершённых таймеров (completion rate)
        meterRegistry.gauge("pomodoro.gauge.completion.rate", timerRepository) { repo ->
            val total = repo.count().toDouble()
            if (total == 0.0) 0.0
            else repo.countByStatus(TimerStatus.COMPLETED).toDouble() / total
        }
    }

    override fun getAllTimers(): ResponseEntity<List<Timer>> {
        val timers = timerRepository.findAll().map { it.toDto() }
        return ResponseEntity.ok(timers)
    }

    override fun createTimer(createTimerRequest: CreateTimerRequest): ResponseEntity<Timer> {
        val entity = TimerEntity(
            name = createTimerRequest.name,
            durationMinutes = createTimerRequest.durationMinutes ?: 25
        )
        val saved = timerRepository.save(entity)
        createdCounter.increment()
        durationSummary.record(entity.durationMinutes.toDouble())
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toDto())
    }

    override fun getTimerById(id: Long): ResponseEntity<Timer> {
        val entity = findTimerOrThrow(id)
        return ResponseEntity.ok(entity.toDto())
    }

    override fun startTimer(id: Long): ResponseEntity<Timer> {
        val entity = findTimerOrThrow(id)

        if (entity.status != TimerStatus.CREATED && entity.status != TimerStatus.PAUSED) {
            conflictErrors.increment()
            throw TimerStateConflictException("Таймер не может быть запущен в состоянии ${entity.status}")
        }

        entity.status = TimerStatus.RUNNING
        entity.startedAt = Instant.now()
        val saved = timerRepository.save(entity)
        startedCounter.increment()
        return ResponseEntity.ok(saved.toDto())
    }

    override fun stopTimer(id: Long): ResponseEntity<Timer> {
        val entity = findTimerOrThrow(id)

        if (entity.status != TimerStatus.RUNNING) {
            conflictErrors.increment()
            throw TimerStateConflictException("Таймер не может быть остановлен в состоянии ${entity.status}")
        }

        entity.accumulateElapsed()
        entity.status = TimerStatus.PAUSED
        val saved = timerRepository.save(entity)
        stoppedCounter.increment()
        return ResponseEntity.ok(saved.toDto())
    }

    override fun completeTimer(id: Long): ResponseEntity<Timer> {
        val entity = findTimerOrThrow(id)

        if (entity.status == TimerStatus.COMPLETED) {
            conflictErrors.increment()
            throw TimerStateConflictException("Таймер уже завершён")
        }

        entity.accumulateElapsed()
        entity.status = TimerStatus.COMPLETED
        val saved = timerRepository.save(entity)
        completedCounter.increment()
        return ResponseEntity.ok(saved.toDto())
    }

    private fun findTimerOrThrow(id: Long): TimerEntity {
        return timerRepository.findById(id).orElseThrow {
            notFoundErrors.increment()
            TimerNotFoundException("Таймер с id=$id не найден")
        }
    }

    private fun TimerEntity.autoCompleteIfExpired(): TimerEntity {
        if (status == TimerStatus.RUNNING && remainingSeconds() <= 0) {
            accumulateElapsed()
            status = TimerStatus.COMPLETED
            timerRepository.save(this)
            autoCompletedCounter.increment()
        }
        return this
    }

    private fun TimerEntity.toDto() = autoCompleteIfExpired().let {
        Timer(
            id = it.id,
            name = it.name,
            status = Timer.Status.forValue(it.status.name),
            durationMinutes = it.durationMinutes,
            remainingSeconds = it.remainingSeconds()
        )
    }
}
