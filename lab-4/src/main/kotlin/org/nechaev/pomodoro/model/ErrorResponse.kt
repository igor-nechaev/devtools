package org.nechaev.pomodoro.model

import com.fasterxml.jackson.annotation.JsonProperty
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

