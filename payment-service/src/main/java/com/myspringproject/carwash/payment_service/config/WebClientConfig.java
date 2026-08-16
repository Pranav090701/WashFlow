package com.myspringproject.carwash.payment_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient bookingWebClient(WebClient.Builder builder,
                                      @Value("${booking.service.base-url}") String bookingServiceBaseUrl) {
        return builder.baseUrl(bookingServiceBaseUrl).build();
    }
}
