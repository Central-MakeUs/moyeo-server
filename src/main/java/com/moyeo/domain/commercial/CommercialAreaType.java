package com.moyeo.domain.commercial;

public enum CommercialAreaType {
    DEVELOPMENT("발달상권"),
    TOURIST_SPECIAL("관광특구");

    private final String displayName;

    CommercialAreaType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
