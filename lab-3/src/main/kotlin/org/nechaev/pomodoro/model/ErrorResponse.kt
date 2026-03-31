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
 * Ответ с описанием ошибки
 * @param message Человекочитаемое описание ошибки
 */
data class ErrorResponse(

    @Schema(example = "Таймер с id=99 не найден", description = "Человекочитаемое описание ошибки")
    @get:JsonProperty("message") val message: kotlin.String? = null
    ) {

}

