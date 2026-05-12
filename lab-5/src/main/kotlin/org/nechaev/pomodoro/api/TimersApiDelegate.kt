package org.nechaev.pomodoro.api

import org.nechaev.pomodoro.model.CreateTimerRequest
import org.nechaev.pomodoro.model.ErrorResponse
import org.nechaev.pomodoro.model.Timer
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.context.request.NativeWebRequest

import java.util.Optional

/**
 * A delegate to be called by the {@link TimersApiController}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.12.0")
interface TimersApiDelegate {

    fun getRequest(): Optional<NativeWebRequest> = Optional.empty()

    /**
     * @see TimersApi#completeTimer
     */
    fun completeTimer(id: kotlin.Long): ResponseEntity<Timer> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"durationMinutes\" : 25,  \"name\" : \"Работа\",  \"remainingSeconds\" : 1230,  \"id\" : 1,  \"status\" : \"RUNNING\"}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"message\" : \"Таймер с id=99 не найден\"}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"message\" : \"Таймер с id=99 не найден\"}")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }


    /**
     * @see TimersApi#createTimer
     */
    fun createTimer(createTimerRequest: CreateTimerRequest): ResponseEntity<Timer> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"durationMinutes\" : 25,  \"name\" : \"Работа\",  \"remainingSeconds\" : 1230,  \"id\" : 1,  \"status\" : \"RUNNING\"}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"message\" : \"Таймер с id=99 не найден\"}")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }


    /**
     * @see TimersApi#getAllTimers
     */
    fun getAllTimers(): ResponseEntity<List<Timer>> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "[ {  \"durationMinutes\" : 25,  \"name\" : \"Работа\",  \"remainingSeconds\" : 1230,  \"id\" : 1,  \"status\" : \"RUNNING\"}, {  \"durationMinutes\" : 25,  \"name\" : \"Работа\",  \"remainingSeconds\" : 1230,  \"id\" : 1,  \"status\" : \"RUNNING\"} ]")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }


    /**
     * @see TimersApi#getTimerById
     */
    fun getTimerById(id: kotlin.Long): ResponseEntity<Timer> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"durationMinutes\" : 25,  \"name\" : \"Работа\",  \"remainingSeconds\" : 1230,  \"id\" : 1,  \"status\" : \"RUNNING\"}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"message\" : \"Таймер с id=99 не найден\"}")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }


    /**
     * @see TimersApi#startTimer
     */
    fun startTimer(id: kotlin.Long): ResponseEntity<Timer> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"durationMinutes\" : 25,  \"name\" : \"Работа\",  \"remainingSeconds\" : 1230,  \"id\" : 1,  \"status\" : \"RUNNING\"}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"message\" : \"Таймер с id=99 не найден\"}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"message\" : \"Таймер с id=99 не найден\"}")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }


    /**
     * @see TimersApi#stopTimer
     */
    fun stopTimer(id: kotlin.Long): ResponseEntity<Timer> {
        getRequest().ifPresent { request ->
            for (mediaType in MediaType.parseMediaTypes(request.getHeader("Accept"))) {
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"durationMinutes\" : 25,  \"name\" : \"Работа\",  \"remainingSeconds\" : 1230,  \"id\" : 1,  \"status\" : \"RUNNING\"}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"message\" : \"Таймер с id=99 не найден\"}")
                    break
                }
                if (mediaType.isCompatibleWith(MediaType.valueOf("application/json"))) {
                    ApiUtil.setExampleResponse(request, "application/json", "{  \"message\" : \"Таймер с id=99 не найден\"}")
                    break
                }
            }
        }
        return ResponseEntity(HttpStatus.NOT_IMPLEMENTED)

    }

}
