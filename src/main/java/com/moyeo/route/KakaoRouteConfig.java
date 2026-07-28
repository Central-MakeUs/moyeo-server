package com.moyeo.route;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(KakaoRouteProperties.class)
public class KakaoRouteConfig {

    @Bean
    @Qualifier("kakaoRouteRestClient")
    RestClient kakaoRouteRestClient(RestClient.Builder builder, KakaoRouteProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        return builder.requestFactory(requestFactory).build();
    }
}
