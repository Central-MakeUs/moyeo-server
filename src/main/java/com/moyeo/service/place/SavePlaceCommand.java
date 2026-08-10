package com.moyeo.service.place;

import com.moyeo.departure.DeparturePlaceType;
import com.moyeo.domain.place.SavedPlaceCategory;

import java.math.BigDecimal;

public record SavePlaceCommand(
        String alias,
        SavedPlaceCategory category,
        DeparturePlaceType type,
        String displayName,
        String address,
        String roadAddress,
        String jibunAddress,
        BigDecimal latitude,
        BigDecimal longitude
) {
}
