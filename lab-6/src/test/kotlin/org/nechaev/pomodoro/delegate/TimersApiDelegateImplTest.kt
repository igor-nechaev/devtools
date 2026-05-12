package org.nechaev.pomodoro.delegate

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.micrometer.observation.ObservationRegistry
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.nechaev.pomodoro.entity.TimerEntity
import org.nechaev.pomodoro.entity.TimerStatus
import org.nechaev.pomodoro.model.CreateTimerRequest
import org.nechaev.pomodoro.model.Timer
import org.nechaev.pomodoro.repository.TimerRepository
import org.springframework.http.HttpStatus
import java.time.Instant
import java.util.Optional

class TimersApiDelegateImplTest {
    private lateinit var timerRepository: TimerRepository
    private lateinit var delegate: TimersApiDelegateImpl

    @BeforeEach
    fun setUp() {
        timerRepository = mock()
        delegate = TimersApiDelegateImpl(timerRepository, SimpleMeterRegistry(), ObservationRegistry.NOOP)
    }

    @Test
    fun `createTimer returns 201 with timer body`() {
        val request = CreateTimerRequest(name = "Study", durationMinutes = 25)
        val saved = TimerEntity(id = 1L, name = "Study", durationMinutes = 25, status = TimerStatus.CREATED)
        whenever(timerRepository.save(any())).thenReturn(saved)

        val response = delegate.createTimer(request)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals("Study", response.body?.name)
        assertEquals(Timer.Status.CREATED, response.body?.status)
        verify(timerRepository).save(any())
    }

    @Test
    fun `createTimer uses default duration of 25 minutes when not specified`() {
        val request = CreateTimerRequest(name = "Work")
        val saved = TimerEntity(id = 2L, name = "Work", durationMinutes = 25, status = TimerStatus.CREATED)
        whenever(timerRepository.save(any())).thenReturn(saved)

        val response = delegate.createTimer(request)

        assertEquals(25, response.body?.durationMinutes)
    }

    @Test
    fun `getAllTimers returns empty list when no timers exist`() {
        whenever(timerRepository.findAll()).thenReturn(emptyList())

        val response = delegate.getAllTimers()

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(emptyList<Timer>(), response.body)
    }

    @Test
    fun `getAllTimers returns all existing timers`() {
        val entities =
            listOf(
                TimerEntity(id = 1L, name = "A", status = TimerStatus.CREATED),
                TimerEntity(id = 2L, name = "B", status = TimerStatus.COMPLETED, elapsedSeconds = 1500L),
            )
        whenever(timerRepository.findAll()).thenReturn(entities)

        val response = delegate.getAllTimers()

        assertEquals(2, response.body?.size)
    }

    @Test
    fun `getTimerById returns timer when found`() {
        val entity = TimerEntity(id = 1L, name = "Focus", status = TimerStatus.CREATED)
        whenever(timerRepository.findById(1L)).thenReturn(Optional.of(entity))

        val response = delegate.getTimerById(1L)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(1L, response.body?.id)
    }

    @Test
    fun `getTimerById throws TimerNotFoundException when not found`() {
        whenever(timerRepository.findById(99L)).thenReturn(Optional.empty())

        assertThrows<TimerNotFoundException> { delegate.getTimerById(99L) }
    }

    @Test
    fun `startTimer transitions CREATED timer to RUNNING`() {
        val entity = TimerEntity(id = 1L, name = "Sprint", status = TimerStatus.CREATED)
        whenever(timerRepository.findById(1L)).thenReturn(Optional.of(entity))
        whenever(timerRepository.save(any())).thenAnswer { it.arguments[0] as TimerEntity }

        val response = delegate.startTimer(1L)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(Timer.Status.RUNNING, response.body?.status)
    }

    @Test
    fun `startTimer transitions PAUSED timer to RUNNING`() {
        val entity = TimerEntity(id = 1L, name = "Sprint", status = TimerStatus.PAUSED, elapsedSeconds = 300L)
        whenever(timerRepository.findById(1L)).thenReturn(Optional.of(entity))
        whenever(timerRepository.save(any())).thenAnswer { it.arguments[0] as TimerEntity }

        val response = delegate.startTimer(1L)

        assertEquals(Timer.Status.RUNNING, response.body?.status)
    }

    @Test
    fun `startTimer throws conflict when timer is already RUNNING`() {
        val entity = TimerEntity(id = 1L, name = "Sprint", status = TimerStatus.RUNNING, startedAt = Instant.now())
        whenever(timerRepository.findById(1L)).thenReturn(Optional.of(entity))

        assertThrows<TimerStateConflictException> { delegate.startTimer(1L) }
    }

    @Test
    fun `startTimer throws conflict when timer is COMPLETED`() {
        val entity = TimerEntity(id = 1L, name = "Sprint", status = TimerStatus.COMPLETED, elapsedSeconds = 1500L)
        whenever(timerRepository.findById(1L)).thenReturn(Optional.of(entity))

        assertThrows<TimerStateConflictException> { delegate.startTimer(1L) }
    }

    @Test
    fun `stopTimer transitions RUNNING timer to PAUSED`() {
        val entity =
            TimerEntity(
                id = 1L,
                name = "Sprint",
                status = TimerStatus.RUNNING,
                startedAt = Instant.now().minusSeconds(60),
            )
        whenever(timerRepository.findById(1L)).thenReturn(Optional.of(entity))
        whenever(timerRepository.save(any())).thenAnswer { it.arguments[0] as TimerEntity }

        val response = delegate.stopTimer(1L)

        assertEquals(Timer.Status.PAUSED, response.body?.status)
    }

    @Test
    fun `stopTimer throws conflict when timer is not RUNNING`() {
        val entity = TimerEntity(id = 1L, name = "Sprint", status = TimerStatus.CREATED)
        whenever(timerRepository.findById(1L)).thenReturn(Optional.of(entity))

        assertThrows<TimerStateConflictException> { delegate.stopTimer(1L) }
    }

    @Test
    fun `completeTimer marks RUNNING timer as COMPLETED`() {
        val entity =
            TimerEntity(
                id = 1L,
                name = "Sprint",
                status = TimerStatus.RUNNING,
                startedAt = Instant.now().minusSeconds(30),
            )
        whenever(timerRepository.findById(1L)).thenReturn(Optional.of(entity))
        whenever(timerRepository.save(any())).thenAnswer { it.arguments[0] as TimerEntity }

        val response = delegate.completeTimer(1L)

        assertEquals(Timer.Status.COMPLETED, response.body?.status)
    }

    @Test
    fun `completeTimer throws conflict when timer is already COMPLETED`() {
        val entity = TimerEntity(id = 1L, name = "Sprint", status = TimerStatus.COMPLETED, elapsedSeconds = 1500L)
        whenever(timerRepository.findById(1L)).thenReturn(Optional.of(entity))

        assertThrows<TimerStateConflictException> { delegate.completeTimer(1L) }
    }
}
