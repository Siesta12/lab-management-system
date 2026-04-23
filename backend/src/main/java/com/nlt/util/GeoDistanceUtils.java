package com.nlt.util;

/**
 * 地理距离计算工具类。
 */
public final class GeoDistanceUtils {

    private static final double EARTH_RADIUS_METERS = 6_371_000D;

    private GeoDistanceUtils() {
    }

    /**
     * 使用 Haversine 公式计算两个经纬度点之间的球面距离，单位为米。
     *
     * @param latitude1 第一个点的纬度
     * @param longitude1 第一个点的经度
     * @param latitude2 第二个点的纬度
     * @param longitude2 第二个点的经度
     * @return 两点之间的距离（米）
     */
    public static double distanceMeters(double latitude1, double longitude1, double latitude2, double longitude2) {
        double lat1 = Math.toRadians(latitude1);
        double lat2 = Math.toRadians(latitude2);
        double deltaLat = Math.toRadians(latitude2 - latitude1);
        double deltaLng = Math.toRadians(longitude2 - longitude1);

        double haversine = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
            + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double angle = 2 * Math.atan2(Math.sqrt(haversine), Math.sqrt(1 - haversine));
        return EARTH_RADIUS_METERS * angle;
    }
}
