package com.tfg.ticketflow.config;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class ReservaClockConfig {

    // Inyectable para probar exactamente el vencimiento sin esperar 10 minutos.
    @Bean
    public Clock reservasClock(@Value("${ticketflow.eventos.zona-horaria:Europe/Madrid}") String zonaEventos) {
        return Clock.tick(Clock.system(ZoneId.of(zonaEventos)), Duration.ofNanos(1_000));
    }
}
