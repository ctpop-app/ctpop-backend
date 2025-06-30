package com.ctpop.service;

import com.ctpop.dto.request.LocationUpdateRequest;
import com.ctpop.dto.response.DistanceInfo;
import com.ctpop.model.Profile;
import com.ctpop.util.DistanceCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileService {
    
    @Qualifier("objectRedisTemplate")
    private final RedisTemplate<String, Object> redisTemplate;
    private final DistanceCalculator distanceCalculator;
    
    private static final String PROFILE_KEY_PREFIX = "profile:";
    private static final String USER_LOCATION_KEY_PREFIX = "location:";
    private static final int LOCATION_EXPIRY_HOURS = 24;
    
    /**
     * 사용자 위치 정보를 업데이트합니다.
     */
    public void updateUserLocation(String uuid, LocationUpdateRequest request) {
        String locationKey = USER_LOCATION_KEY_PREFIX + uuid;
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", request.getLatitude());
        locationData.put("longitude", request.getLongitude());
        locationData.put("timestamp", System.currentTimeMillis());
        
        redisTemplate.opsForHash().putAll(locationKey, locationData);
        redisTemplate.expire(locationKey, LOCATION_EXPIRY_HOURS, TimeUnit.HOURS);
        
        log.info("Updated location for user {}: ({}, {})", uuid, request.getLatitude(), request.getLongitude());
    }
    
    /**
     * 사용자 위치 정보를 조회합니다.
     */
    public Optional<Map<String, Object>> getUserLocation(String uuid) {
        String locationKey = USER_LOCATION_KEY_PREFIX + uuid;
        Map<Object, Object> locationData = redisTemplate.opsForHash().entries(locationKey);
        
        if (locationData.isEmpty()) {
            return Optional.empty();
        }
        
        Map<String, Object> result = new HashMap<>();
        locationData.forEach((key, value) -> result.put(key.toString(), value));
        
        return Optional.of(result);
    }
    
    /**
     * 현재 사용자와 다른 사용자들 간의 거리를 계산합니다.
     */
    public List<DistanceInfo> calculateDistances(String currentUserUuid, List<String> targetUuids) {
        List<DistanceInfo> distances = new ArrayList<>();
        
        // 현재 사용자 위치 조회
        Optional<Map<String, Object>> currentUserLocation = getUserLocation(currentUserUuid);
        if (currentUserLocation.isEmpty()) {
            log.warn("Current user {} location not found", currentUserUuid);
            return distances;
        }
        
        Double currentLat = (Double) currentUserLocation.get().get("latitude");
        Double currentLon = (Double) currentUserLocation.get().get("longitude");
        
        for (String targetUuid : targetUuids) {
            if (targetUuid.equals(currentUserUuid)) {
                continue; // 자기 자신은 제외
            }
            
            Optional<Map<String, Object>> targetLocation = getUserLocation(targetUuid);
            if (targetLocation.isPresent()) {
                Double targetLat = (Double) targetLocation.get().get("latitude");
                Double targetLon = (Double) targetLocation.get().get("longitude");
                
                double distanceKm = distanceCalculator.calculateDistance(currentLat, currentLon, targetLat, targetLon);
                String formattedDistance = distanceCalculator.formatDistance(distanceKm);
                
                DistanceInfo distanceInfo = DistanceInfo.builder()
                        .targetUuid(targetUuid)
                        .distanceKm(distanceKm)
                        .formattedDistance(formattedDistance)
                        .calculationMethod("backend")
                        .build();
                
                distances.add(distanceInfo);
                log.debug("Calculated distance from {} to {}: {} km", currentUserUuid, targetUuid, distanceKm);
            } else {
                log.debug("Target user {} location not found", targetUuid);
            }
        }
        
        return distances;
    }
    
    /**
     * 온라인 사용자들의 거리 정보를 브로드캐스트합니다.
     */
    public void broadcastNearbyDistances(String currentUserUuid, List<String> onlineUsers) {
        List<DistanceInfo> distances = calculateDistances(currentUserUuid, onlineUsers);
        
        Map<String, Object> distanceData = new HashMap<>();
        distanceData.put("distances", distances);
        distanceData.put("timestamp", System.currentTimeMillis());
        
        // Redis에 거리 정보 저장 (선택사항)
        String distanceKey = "distances:" + currentUserUuid;
        redisTemplate.opsForValue().set(distanceKey, distanceData, 5, TimeUnit.MINUTES);
        
        log.info("Broadcasted {} distance calculations for user {}", distances.size(), currentUserUuid);
    }
} 