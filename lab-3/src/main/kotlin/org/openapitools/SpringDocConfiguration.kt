package org.openapitools

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.security.SecurityScheme

@Configuration
class SpringDocConfiguration {

    @Bean
    fun apiInfo(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Pomodoro Timer Service")
                    .description("REST API для управления таймерами по технике помидоро. Сервис позволяет создавать таймеры с настраиваемой длительностью, запускать и приостанавливать их, а также отслеживать оставшееся время в реальном времени с учётом всех пауз. Состояния таймера: CREATED → RUNNING → PAUSED → COMPLETED. Допускаются переходы: start (из CREATED/PAUSED), stop (из RUNNING), complete (из любого, кроме COMPLETED). Таймер автоматически завершается при истечении времени. ")
                    .contact(
                        Contact()
                            .name("Нечаев Игорь Сергеевич")
                    )
                    .license(
                        License()
                            .name("MIT")
                                                )
                    .version("1.0.0")
            )
    }
}
