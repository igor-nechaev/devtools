package org.nechaev.pomodoro.entity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class TimerEntityTest {
    @Test
    fun `remainingSeconds returns full duration when just created`() {
        val entity = TimerEntity(durationMinutes = 25)
        assertEquals(25 * 60L, entity.remainingSeconds())
    }

    @Test
    fun `remainingSeconds returns zero when fully elapsed`() {
        val entity = TimerEntity(durationMinutes = 25, elapsedSeconds = 25 * 60L)
        assertEquals(0L, entity.remainingSeconds())
    }

    @Test
    fun `remainingSeconds never goes below zero`() {
        val entity = TimerEntity(durationMinutes = 1, elapsedSeconds = 9999L)
        assertEquals(0L, entity.remainingSeconds())
    }

    @Test
    fun `remainingSeconds accounts for running time`() {
        val startedAt = Instant.now().minusSeconds(30)
        val entity =
            TimerEntity(
                durationMinutes = 1,
                status = TimerStatus.RUNNING,
                startedAt = startedAt,
            )
        val remaining = entity.remainingSeconds()
        assertTrue(remaining in 25L..35L, "Expected ~30s remaining, got $remaining")
    }

    @Test
    fun `accumulateElapsed does nothing when status is CREATED`() {
        val entity = TimerEntity(durationMinutes = 25, status = TimerStatus.CREATED)
        entity.accumulateElapsed()
        assertEquals(0L, entity.elapsedSeconds)
        assertNull(entity.startedAt)
    }

    @Test
    fun `accumulateElapsed accumulates running seconds and clears startedAt`() {
        val startedAt = Instant.now().minusSeconds(60)
        val entity =
            TimerEntity(
                durationMinutes = 25,
                status = TimerStatus.RUNNING,
                startedAt = startedAt,
            )
        entity.accumulateElapsed()
        assertTrue(entity.elapsedSeconds >= 60L)
        assertNull(entity.startedAt)
    }

    @Test
    fun `accumulateElapsed adds to previously accumulated elapsed`() {
        val startedAt = Instant.now().minusSeconds(30)
        val entity =
            TimerEntity(
                durationMinutes = 25,
                status = TimerStatus.RUNNING,
                elapsedSeconds = 60L,
                startedAt = startedAt,
            )
        entity.accumulateElapsed()
        assertTrue(entity.elapsedSeconds >= 90L)
    }
}
