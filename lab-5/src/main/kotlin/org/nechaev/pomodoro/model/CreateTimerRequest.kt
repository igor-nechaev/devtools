package org.nechaev.pomodoro.model

import java.util.Objects
import com.fasterxml.jackson.annotation.JsonProperty
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
 * Запрос на создание нового таймера
 * @param name Название таймера
 * @param durationMinutes Длительность таймера в минутах (от 1 до 120)
 */
data class CreateTimerRequest(

    @get:Size(min=1,max=255)
    @Schema(example = "Работа", required = true, description = "Название таймера")
    @get:JsonProperty("name", required = true) val name: kotlin.String,

    @get:Min(1)
    @get:Max(120)
    @Schema(example = "25", description = "Длительность таймера в минутах (от 1 до 120)")
    @get:JsonProperty("durationMinutes") val durationMinutes: kotlin.Int? = 25
    ) {

}

