package com.phegon.phegonbank.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        registry.addResourceHandler("/profile-picture/**")
                .addResourceLocations("file:/Users/miaoyiyuan/Desktop/试验田/phegon-bank-react/profile-picture/");
    }
}
