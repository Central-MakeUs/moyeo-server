package com.moyeo.service.place;

import com.moyeo.departure.DeparturePlaceType;
import com.moyeo.domain.place.SavedPlace;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SavedPlaceResult(
        Long id,
        String alias,
        DeparturePlaceType type,
        String displayName,
        String address,
        String roadAddress,
        String jibunAddress,
        BigDecimal latitude,
        BigDecimal longitude,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static SavedPlaceResult from(SavedPlace place) {
        return new SavedPlaceResult(
                place.getId(),
                place.getAlias(),
                place.getType(),
                place.getDisplayName(),
                place.getAddress(),
                place.getRoadAddress(),
                place.getJibunAddress(),
                place.getLatitude(),
                place.getLongitude(),
                place.getCreatedAt(),
                place.getUpdatedAt()
        );
    }
}
