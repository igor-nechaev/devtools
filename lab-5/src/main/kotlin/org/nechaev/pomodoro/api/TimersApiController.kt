package org.nechaev.pomodoro.api

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import java.util.Optional

@jakarta.annotation.Generated(value = ["org.openapitools.codegen.languages.KotlinSpringServerCodegen"], comments = "Generator version: 7.12.0")
@Controller
@RequestMapping("\${openapi.pomodoroTimerService.base-path:}")
class TimersApiController(
        delegate: TimersApiDelegate?
) : TimersApi {
    private lateinit var delegate: TimersApiDelegate

    init {
        this.delegate = Optional.ofNullable(delegate).orElse(object : TimersApiDelegate {})
    }

    override fun getDelegate(): TimersApiDelegate = delegate
}
