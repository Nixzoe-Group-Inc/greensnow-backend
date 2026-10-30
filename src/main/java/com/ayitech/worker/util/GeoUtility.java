package com.ayitech.worker.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class GeoUtility {

    private static final double EARTH_RADIUS_KM = 6371.0088;
    private static final int DISTANCE_SCALE = 2;

    private static final BigDecimal MIN_LATITUDE = BigDecimal.valueOf(-90);
    private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90);
    private static final BigDecimal MIN_LONGITUDE = BigDecimal.valueOf(-180);
    private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180);

    private GeoUtility() {
    }

    // haversine, in kilometres. good to a few metres at the ranges a tradesperson travels,
    // which is all this is ever asked for - it decides whether a job is inside a worker's
    // service radius, never where anyone actually is
    public static BigDecimal distanceKm(BigDecimal fromLatitude, BigDecimal fromLongitude,
                                        BigDecimal toLatitude, BigDecimal toLongitude) {

        // null means a side has no location on it at all, which is not a distance of zero -
        // zero would read as "on the doorstep" and score the job top marks for being nearby
        if (Objects.isNull(fromLatitude) || Objects.isNull(fromLongitude)
                || Objects.isNull(toLatitude) || Objects.isNull(toLongitude)) {
            return null;
        }

        double fromLatitudeRadians = Math.toRadians(fromLatitude.doubleValue());
        double toLatitudeRadians = Math.toRadians(toLatitude.doubleValue());
        double latitudeDelta = Math.toRadians(toLatitude.doubleValue() - fromLatitude.doubleValue());
        double longitudeDelta = Math.toRadians(toLongitude.doubleValue() - fromLongitude.doubleValue());

        double a = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(fromLatitudeRadians) * Math.cos(toLatitudeRadians)
                * Math.pow(Math.sin(longitudeDelta / 2), 2);

        double kilometres = 2 * EARTH_RADIUS_KM * Math.asin(Math.min(1.0, Math.sqrt(a)));

        return BigDecimal.valueOf(kilometres).setScale(DISTANCE_SCALE, RoundingMode.HALF_UP);
    }

    public static boolean isValidLatitude(BigDecimal latitude) {
        return Objects.nonNull(latitude)
                && latitude.compareTo(MIN_LATITUDE) >= 0
                && latitude.compareTo(MAX_LATITUDE) <= 0;
    }

    public static boolean isValidLongitude(BigDecimal longitude) {
        return Objects.nonNull(longitude)
                && longitude.compareTo(MIN_LONGITUDE) >= 0
                && longitude.compareTo(MAX_LONGITUDE) <= 0;
    }
}
