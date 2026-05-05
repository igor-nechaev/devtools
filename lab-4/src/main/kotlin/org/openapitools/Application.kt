package org.openapitools

import org.springframework.boot.runApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.ComponentScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@ComponentScan(basePackages = ["org.openapitools", "org.nechaev.pomodoro"])
@EntityScan(basePackages = ["org.nechaev.pomodoro.entity"])
@EnableJpaRepositories(basePackages = ["org.nechaev.pomodoro.repository"])
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
