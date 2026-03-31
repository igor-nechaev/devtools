package org.nechaev.pomodoro.delegate

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
    private val timerRepository: TimerRepository
) : TimersApiDelegate {

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
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toDto())
    }

    override fun getTimerById(id: Long): ResponseEntity<Timer> {
        val entity = findTimerOrThrow(id)
        return ResponseEntity.ok(entity.toDto())
    }

    override fun startTimer(id: Long): ResponseEntity<Timer> {
        val entity = findTimerOrThrow(id)

        if (entity.status != TimerStatus.CREATED && entity.status != TimerStatus.PAUSED) {
            throw TimerStateConflictException("Таймер не может быть запущен в состоянии ${entity.status}")
        }

        entity.status = TimerStatus.RUNNING
        entity.startedAt = Instant.now()
        return ResponseEntity.ok(timerRepository.save(entity).toDto())
    }

    override fun stopTimer(id: Long): ResponseEntity<Timer> {
        val entity = findTimerOrThrow(id)

        if (entity.status != TimerStatus.RUNNING) {
            throw TimerStateConflictException("Таймер не может быть остановлен в состоянии ${entity.status}")
        }

        entity.accumulateElapsed()
        entity.status = TimerStatus.PAUSED
        return ResponseEntity.ok(timerRepository.save(entity).toDto())
    }

    override fun completeTimer(id: Long): ResponseEntity<Timer> {
        val entity = findTimerOrThrow(id)

        if (entity.status == TimerStatus.COMPLETED) {
            throw TimerStateConflictException("Таймер уже завершён")
        }

        entity.accumulateElapsed()
        entity.status = TimerStatus.COMPLETED
        return ResponseEntity.ok(timerRepository.save(entity).toDto())
    }

    private fun findTimerOrThrow(id: Long): TimerEntity {
        return timerRepository.findById(id).orElseThrow {
            TimerNotFoundException("Таймер с id=$id не найден")
        }
    }

    private fun TimerEntity.autoCompleteIfExpired(): TimerEntity {
        if (status == TimerStatus.RUNNING && remainingSeconds() <= 0) {
            accumulateElapsed()
            status = TimerStatus.COMPLETED
            timerRepository.save(this)
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
