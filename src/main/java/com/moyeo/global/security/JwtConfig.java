package com.moyeo.global.security;

import com.moyeo.global.CorsProperties;
import com.moyeo.departure.DeparturePlaceSearchProperties;
import com.moyeo.service.meeting.MeetingCoverProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class, DeparturePlaceSearchProperties.class, MeetingCoverProperties.class})
public class JwtConfig {
}
