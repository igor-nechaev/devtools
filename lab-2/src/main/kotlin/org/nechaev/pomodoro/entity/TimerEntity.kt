package org.nechaev.pomodoro.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "timers")
class TimerEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    var name: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: TimerStatus = TimerStatus.CREATED,

    @Column(name = "duration_minutes", nullable = false)
    var durationMinutes: Int = 25,

    @Column(name = "elapsed_seconds", nullable = false)
    var elapsedSeconds: Long = 0,

    @Column(name = "started_at")
    var startedAt: Instant? = null
) {
    fun remainingSeconds(): Long {
        val totalSeconds = durationMinutes.toLong() * 60
        val currentElapsed = when (status) {
            TimerStatus.RUNNING -> {
                val running = java.time.Duration.between(startedAt, Instant.now()).seconds
                elapsedSeconds + running
            }
            else -> elapsedSeconds
        }
        return maxOf(0, totalSeconds - currentElapsed)
    }

    fun accumulateElapsed() {
        if (status == TimerStatus.RUNNING && startedAt != null) {
            elapsedSeconds += java.time.Duration.between(startedAt, Instant.now()).seconds
            startedAt = null
        }
    }
}

enum class TimerStatus {
    CREATED, RUNNING, PAUSED, COMPLETED
}
