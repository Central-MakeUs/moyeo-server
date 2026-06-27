package com.moyeo.global;

import com.moyeo.global.security.CurrentMemberArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final CurrentMemberArgumentResolver currentMemberArgumentResolver;
    private final CorsProperties corsProperties;

    public WebMvcConfig(
            CurrentMemberArgumentResolver currentMemberArgumentResolver,
            CorsProperties corsProperties
    ) {
        this.currentMemberArgumentResolver = currentMemberArgumentResolver;
        this.corsProperties = corsProperties;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentMemberArgumentResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(corsProperties.allowedOrigins().toArray(String[]::new))
                .allowedMethods(corsProperties.allowedMethods().toArray(String[]::new))
                .allowedHeaders(corsProperties.allowedHeaders().toArray(String[]::new))
                .maxAge(corsProperties.maxAgeSeconds());
    }
}
