package com.moyeo.service.place;

import com.moyeo.departure.DeparturePlaceType;

import java.math.BigDecimal;

public record SavePlaceCommand(
        String alias,
        DeparturePlaceType type,
        String displayName,
        String address,
        String roadAddress,
        String jibunAddress,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
