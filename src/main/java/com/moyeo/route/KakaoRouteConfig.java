package com.moyeo.route;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@EnableConfigurationProperties(KakaoRouteProperties.class)
public class KakaoRouteConfig {

    @Bean(destroyMethod = "shutdown")
    @Qualifier("actualRouteExecutor")
    ExecutorService actualRouteExecutor(KakaoRouteProperties properties) {
        AtomicInteger sequence = new AtomicInteger();
        return Executors.newFixedThreadPool(properties.maxConcurrentRequests(), runnable -> {
            Thread thread = new Thread(runnable, "actual-route-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    @Bean
    @Qualifier("kakaoRouteRestClient")
    RestClient kakaoRouteRestClient(RestClient.Builder builder, KakaoRouteProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        return builder.requestFactory(requestFactory).build();
    }
}
