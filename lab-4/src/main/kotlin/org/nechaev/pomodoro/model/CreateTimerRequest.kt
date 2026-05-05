package org.nechaev.pomodoro.model

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

/**
 * Запрос на создание нового таймера
 * @param name Название таймера
 * @param durationMinutes Длительность таймера в минутах (от 1 до 120)
 */
data class CreateTimerRequest(

    @get:Size(min = 1, max = 255)
    @Schema(example = "Работа", required = true, description = "Название таймера")
    @get:JsonProperty("name", required = true) val name: kotlin.String,

    @get:Min(1)
    @get:Max(120)
    @Schema(example = "25", description = "Длительность таймера в минутах (от 1 до 120)")
    @get:JsonProperty("durationMinutes") val durationMinutes: kotlin.Int? = 25
) {

}

