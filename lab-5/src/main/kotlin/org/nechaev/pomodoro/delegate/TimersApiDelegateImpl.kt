package org.nechaev.pomodoro.delegate

import io.micrometer.core.instrument.DistributionSummary
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.observation.Observation
import io.micrometer.observation.ObservationRegistry
import org.nechaev.pomodoro.api.TimersApiDelegate
import org.nechaev.pomodoro.entity.TimerEntity
import org.nechaev.pomodoro.entity.TimerStatus
import org.nechaev.pomodoro.model.CreateTimerRequest
import org.nechaev.pomodoro.model.Timer
import org.nechaev.pomodoro.repository.TimerRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class TimersApiDelegateImpl(
    private val timerRepository: TimerRepository,
    private val meterRegistry: MeterRegistry,
    private val observationRegistry: ObservationRegistry
) : TimersApiDelegate {

    private val log = LoggerFactory.getLogger(javaClass)

    private val createdCounter = meterRegistry.counter("pomodoro.op.create")
    private val startedCounter = meterRegistry.counter("pomodoro.op.start")
    private val stoppedCounter = meterRegistry.counter("pomodoro.op.stop")
    private val completedCounter = meterRegistry.counter("pomodoro.op.complete", "reason", "manual")
    private val autoCompletedCounter = meterRegistry.counter("pomodoro.op.complete", "reason", "expired")
    private val notFoundErrors = meterRegistry.counter("pomodoro.op.error", "type", "not_found")
    private val conflictErrors = meterRegistry.counter("pomodoro.op.error", "type", "conflict")

    private val durationSummary: DistributionSummary = DistributionSummary.builder("pomodoro.timer.duration")
        .description("Длительность создаваемых таймеров в минутах")
        .baseUnit("minutes")
        .publishPercentileHistogram()
        .register(meterRegistry)

    init {
        meterRegistry.gauge("pomodoro.gauge.running", timerRepository) { repo ->
            repo.countByStatus(TimerStatus.RUNNING).toDouble()
        }
        meterRegistry.gauge("pomodoro.gauge.all", timerRepository) { repo ->
            repo.count().toDouble()
        }
        meterRegistry.gauge("pomodoro.gauge.pending", timerRepository) { repo ->
            repo.countByStatus(TimerStatus.CREATED).toDouble()
        }
        meterRegistry.gauge("pomodoro.gauge.completion.rate", timerRepository) { repo ->
            val total = repo.count().toDouble()
            if (total == 0.0) 0.0
            else repo.countByStatus(TimerStatus.COMPLETED).toDouble() / total
        }
    }

    private fun <T> span(name: String, block: (Observation) -> T): T {
        val observation = Observation.start(name, observationRegistry)
        val scope = observation.openScope()
        return try {
            block(observation)
        } catch (e: Exception) {
            observation.error(e)
            throw e
        } finally {
            scope.close()
            observation.stop()
        }
    }

    override fun getAllTimers(): ResponseEntity<List<Timer>> = span("pomodoro.list-timers") { obs ->
        val entities = span("pomodoro.repository.find-all") { timerRepository.findAll() }
        val timers = span("pomodoro.toDto.batch") {
            entities.map { it.toDto() }
        }
        obs.lowCardinalityKeyValue("timers.count", timers.size.toString())
        ResponseEntity.ok(timers)
    }

    override fun createTimer(createTimerRequest: CreateTimerRequest): ResponseEntity<Timer> =
        span("pomodoro.create-timer") { obs ->
            val durationMinutes = createTimerRequest.durationMinutes ?: 25
            obs.lowCardinalityKeyValue("timer.duration_minutes", durationMinutes.toString())

            val entity = span("pomodoro.entity.build") {
                TimerEntity(name = createTimerRequest.name, durationMinutes = durationMinutes)
            }
            val saved = span("pomodoro.repository.save") { childObs ->
                val result = timerRepository.save(entity)
                childObs.lowCardinalityKeyValue("timer.id", result.id.toString())
                result
            }
            span("pomodoro.metrics.record") {
                createdCounter.increment()
                durationSummary.record(entity.durationMinutes.toDouble())
            }
            log.info("BUSINESS action=create id={} duration_minutes={}", saved.id, saved.durationMinutes)
            ResponseEntity.status(HttpStatus.CREATED).body(saved.toDto())
        }

    override fun getTimerById(id: Long): ResponseEntity<Timer> = span("pomodoro.get-timer") { obs ->
        obs.lowCardinalityKeyValue("timer.id", id.toString())
        val entity = findTimerOrThrow(id)
        ResponseEntity.ok(entity.toDto())
    }

    override fun startTimer(id: Long): ResponseEntity<Timer> = span("pomodoro.start-timer") { obs ->
        obs.lowCardinalityKeyValue("timer.id", id.toString())
        val entity = findTimerOrThrow(id)

        span("pomodoro.state.validate") { childObs ->
            childObs.lowCardinalityKeyValue("timer.current_status", entity.status.name)
            if (entity.status != TimerStatus.CREATED && entity.status != TimerStatus.PAUSED) {
                conflictErrors.increment()
                log.warn("WARN reason=invalid_state action=start id={} status={}", id, entity.status)
                throw TimerStateConflictException("Таймер не может быть запущен в состоянии ${entity.status}")
            }
        }

        val saved = span("pomodoro.state.transition.running") {
            entity.status = TimerStatus.RUNNING
            entity.startedAt = Instant.now()
            timerRepository.save(entity)
        }
        startedCounter.increment()
        log.info("BUSINESS action=start id={}", saved.id)
        ResponseEntity.ok(saved.toDto())
    }

    override fun stopTimer(id: Long): ResponseEntity<Timer> = span("pomodoro.stop-timer") { obs ->
        obs.lowCardinalityKeyValue("timer.id", id.toString())
        val entity = findTimerOrThrow(id)

        span("pomodoro.state.validate") { childObs ->
            childObs.lowCardinalityKeyValue("timer.current_status", entity.status.name)
            if (entity.status != TimerStatus.RUNNING) {
                conflictErrors.increment()
                log.warn("WARN reason=invalid_state action=stop id={} status={}", id, entity.status)
                throw TimerStateConflictException("Таймер не может быть остановлен в состоянии ${entity.status}")
            }
        }

        val saved = span("pomodoro.state.transition.paused") { childObs ->
            entity.accumulateElapsed()
            entity.status = TimerStatus.PAUSED
            val result = timerRepository.save(entity)
            childObs.lowCardinalityKeyValue("timer.elapsed_seconds", result.elapsedSeconds.toString())
            result
        }
        stoppedCounter.increment()
        log.info("BUSINESS action=stop id={} elapsed_seconds={}", saved.id, saved.elapsedSeconds)
        ResponseEntity.ok(saved.toDto())
    }

    override fun completeTimer(id: Long): ResponseEntity<Timer> = span("pomodoro.complete-timer") { obs ->
        obs.lowCardinalityKeyValue("timer.id", id.toString())
        val entity = findTimerOrThrow(id)

        span("pomodoro.state.validate") { childObs ->
            childObs.lowCardinalityKeyValue("timer.current_status", entity.status.name)
            if (entity.status == TimerStatus.COMPLETED) {
                conflictErrors.increment()
                log.warn("WARN reason=already_completed action=complete id={}", id)
                throw TimerStateConflictException("Таймер уже завершён")
            }
        }

        val saved = span("pomodoro.state.transition.completed") { childObs ->
            entity.accumulateElapsed()
            entity.status = TimerStatus.COMPLETED
            val result = timerRepository.save(entity)
            childObs.lowCardinalityKeyValue("timer.elapsed_seconds", result.elapsedSeconds.toString())
            result
        }
        completedCounter.increment()
        log.info("BUSINESS action=complete id={} elapsed_seconds={}", saved.id, saved.elapsedSeconds)
        ResponseEntity.ok(saved.toDto())
    }

    private fun findTimerOrThrow(id: Long): TimerEntity = span("pomodoro.repository.find-by-id") { obs ->
        obs.lowCardinalityKeyValue("timer.id", id.toString())
        timerRepository.findById(id).orElseThrow {
            notFoundErrors.increment()
            log.warn("WARN reason=not_found id={}", id)
            TimerNotFoundException("Таймер с id=$id не найден")
        }
    }

    private fun TimerEntity.autoCompleteIfExpired(): TimerEntity {
        if (status == TimerStatus.RUNNING && remainingSeconds() <= 0) {
            span("pomodoro.auto-complete") { obs ->
                obs.lowCardinalityKeyValue("timer.id", id.toString())
                accumulateElapsed()
                status = TimerStatus.COMPLETED
                timerRepository.save(this)
                autoCompletedCounter.increment()
                log.info("BUSINESS action=auto_complete id={} elapsed_seconds={}", id, elapsedSeconds)
            }
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
