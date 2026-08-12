package com.moyeo.domain.departure;

public final class DepartureRegionPolicy {

    private static final String SEOUL = "서울";
    private static final String SEOUL_METROPOLITAN = "서울특별시";
    private static final String GYEONGGI = "경기";
    private static final String GYEONGGI_DO = "경기도";

    private DepartureRegionPolicy() {
    }

    public static boolean isSupportedAddress(String address) {
        if (address == null) {
            return false;
        }
        String normalizedAddress = address.strip();
        return matchesRegionPrefix(normalizedAddress, SEOUL)
                || matchesRegionPrefix(normalizedAddress, SEOUL_METROPOLITAN)
                || matchesRegionPrefix(normalizedAddress, GYEONGGI)
                || matchesRegionPrefix(normalizedAddress, GYEONGGI_DO);
    }

    private static boolean matchesRegionPrefix(String address, String region) {
        return address.equals(region) || address.startsWith(region + " ");
    }
}
