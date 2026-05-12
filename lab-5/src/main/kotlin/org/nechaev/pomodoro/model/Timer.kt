package org.nechaev.pomodoro.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import jakarta.validation.Valid
import io.swagger.v3.oas.annotations.media.Schema

/**
 * Таймер помидоро с отслеживанием оставшегося времени
 * @param id Уникальный идентификатор таймера
 * @param name Название таймера
 * @param status Текущее состояние таймера
 * @param remainingSeconds Оставшееся время в секундах. Вычисляется динамически с учётом всех запусков и пауз. Для запущенного таймера пересчитывается при каждом запросе. 
 * @param durationMinutes Длительность таймера в минутах
 */
data class Timer(

    @Schema(example = "1", required = true, description = "Уникальный идентификатор таймера")
    @get:JsonProperty("id", required = true) val id: kotlin.Long,

    @Schema(example = "Работа", required = true, description = "Название таймера")
    @get:JsonProperty("name", required = true) val name: kotlin.String,

    @Schema(example = "RUNNING", required = true, description = "Текущее состояние таймера")
    @get:JsonProperty("status", required = true) val status: Timer.Status,

    @get:Min(0L)
    @Schema(example = "1230", required = true, description = "Оставшееся время в секундах. Вычисляется динамически с учётом всех запусков и пауз. Для запущенного таймера пересчитывается при каждом запросе. ")
    @get:JsonProperty("remainingSeconds", required = true) val remainingSeconds: kotlin.Long,

    @get:Min(1)
    @get:Max(120)
    @Schema(example = "25", description = "Длительность таймера в минутах")
    @get:JsonProperty("durationMinutes") val durationMinutes: kotlin.Int? = 25
    ) {

    /**
    * Текущее состояние таймера
    * Values: CREATED,RUNNING,PAUSED,COMPLETED
    */
    enum class Status(@get:JsonValue val value: kotlin.String) {

        CREATED("CREATED"),
        RUNNING("RUNNING"),
        PAUSED("PAUSED"),
        COMPLETED("COMPLETED");

        companion object {
            @JvmStatic
            @JsonCreator
            fun forValue(value: kotlin.String): Status {
                return values().first{it -> it.value == value}
            }
        }
    }

}

