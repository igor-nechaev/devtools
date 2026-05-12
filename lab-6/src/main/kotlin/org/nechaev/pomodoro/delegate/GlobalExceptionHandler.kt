package org.nechaev.pomodoro.delegate

import org.nechaev.pomodoro.model.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message =
            ex.bindingResult.fieldErrors
                .joinToString("; ") { "${it.field}: ${it.defaultMessage}" }
        log.warn("WARN reason=validation_failed details=\"{}\"", message)
        return ResponseEntity.badRequest().body(ErrorResponse(message))
    }

    @ExceptionHandler(TimerNotFoundException::class)
    fun handleNotFound(ex: TimerNotFoundException): ResponseEntity<ErrorResponse> = ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(ex.message))

    @ExceptionHandler(TimerStateConflictException::class)
    fun handleConflict(ex: TimerStateConflictException): ResponseEntity<ErrorResponse> = ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse(ex.message))

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error(
            "ERROR reason=unhandled_exception type={} message=\"{}\"",
            ex.javaClass.simpleName,
            ex.message,
            ex
        )
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ErrorResponse("Внутренняя ошибка сервера"))
    }
}

class TimerNotFoundException(
    message: String
) : RuntimeException(message)

class TimerStateConflictException(
    message: String
) : RuntimeException(message)
