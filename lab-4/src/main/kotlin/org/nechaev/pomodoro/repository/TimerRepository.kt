package org.nechaev.pomodoro.repository

import org.nechaev.pomodoro.entity.TimerEntity
import org.nechaev.pomodoro.entity.TimerStatus
import org.springframework.data.jpa.repository.JpaRepository

interface TimerRepository : JpaRepository<TimerEntity, Long> {
    fun countByStatus(status: TimerStatus): Long
}
