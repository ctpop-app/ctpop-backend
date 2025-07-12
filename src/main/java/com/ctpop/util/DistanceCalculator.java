package com.ctpop.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DistanceCalculator {
    
    private static final double EARTH_RADIUS = 6371; // 지구 반지름 (km)
    
    /**
     * Haversine 공식을 사용하여 두 지점 간의 거리를 계산합니다.
     * @param lat1 첫 번째 지점의 위도
     * @param lon1 첫 번째 지점의 경도
     * @param lat2 두 번째 지점의 위도
     * @param lon2 두 번째 지점의 경도
     * @return 두 지점 간의 거리 (km)
     */
    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS * c;
        
        log.debug("Distance calculation: ({}, {}) to ({}, {}) = {} km", 
                lat1, lon1, lat2, lon2, distance);
        
        return distance;
    }
    
    /**
     * 거리를 포맷팅하여 반환합니다.
     * @param distanceKm 거리 (km)
     * @return 포맷팅된 거리 문자열
     */
    public String formatDistance(double distanceKm) {
        if (distanceKm < 1) {
            return String.format("%.0fm", distanceKm * 1000);
        } else if (distanceKm < 10) {
            return String.format("%.1fkm", distanceKm);
        } else {
            return String.format("%.0fkm", distanceKm);
        }
    }
} 