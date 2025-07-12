package com.rohit.HotelBooking.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer webMvcConfigurer(){
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {

                        // Apply CORS to all backend endpoints
                registry.addMapping("/**")
                        .allowedMethods("GET","POST","PUT","DELETE")
                        .allowedOrigins("*");
                        // it allows request from all domain

                        //  Safer Alternative for Production:
                        //  If your frontend is hosted at https://myfrontend.com , if i use this url
                        // it accept request from only this url, https://myfrontend.com
            }
        };
    }

}
