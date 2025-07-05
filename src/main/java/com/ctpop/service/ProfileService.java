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
    private static final String USER_LAST_LOCATION_KEY_PREFIX = "last_location:";
    private static final String LOCATION_PERMISSION_KEY_PREFIX = "location_permission:";
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
     * 사용자의 마지막 위치를 업데이트합니다.
     */
    public void updateUserLastLocation(String uuid, LocationUpdateRequest request) {
        String lastLocationKey = USER_LAST_LOCATION_KEY_PREFIX + uuid;
        Map<String, Object> locationData = new HashMap<>();
        locationData.put("latitude", request.getLatitude());
        locationData.put("longitude", request.getLongitude());
        locationData.put("timestamp", System.currentTimeMillis());
        
        redisTemplate.opsForHash().putAll(lastLocationKey, locationData);
        redisTemplate.expire(lastLocationKey, LOCATION_EXPIRY_HOURS * 7, TimeUnit.HOURS); // 7일간 보관
        
        log.info("Updated last location for user {}: ({}, {})", uuid, request.getLatitude(), request.getLongitude());
    }
    
    /**
     * 사용자의 마지막 위치를 조회합니다.
     */
    public Optional<Map<String, Object>> getUserLastLocation(String uuid) {
        String lastLocationKey = USER_LAST_LOCATION_KEY_PREFIX + uuid;
        Map<Object, Object> locationData = redisTemplate.opsForHash().entries(lastLocationKey);
        
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
        
        // 현재 사용자 위치 조회 (현재 위치가 없으면 마지막 위치 사용)
        Optional<Map<String, Object>> currentUserLocation = getUserLocation(currentUserUuid);
        if (currentUserLocation.isEmpty()) {
            currentUserLocation = getUserLastLocation(currentUserUuid);
        }
        
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
            
            // 대상 사용자 위치 조회 (현재 위치가 없으면 마지막 위치 사용)
            Optional<Map<String, Object>> targetLocation = getUserLocation(targetUuid);
            if (targetLocation.isEmpty()) {
                targetLocation = getUserLastLocation(targetUuid);
            }
            
            if (targetLocation.isPresent()) {
                Double targetLat = (Double) targetLocation.get().get("latitude");
                Double targetLon = (Double) targetLocation.get().get("longitude");
                
                double distanceKm = distanceCalculator.calculateDistance(currentLat, currentLon, targetLat, targetLon);
                String formattedDistance = distanceCalculator.formatDistance(distanceKm);
                
                // 위치 타입 확인 (현재 위치인지 마지막 위치인지)
                String locationType = getUserLocation(targetUuid).isPresent() ? "current" : "last";
                
                DistanceInfo distanceInfo = DistanceInfo.builder()
                        .targetUuid(targetUuid)
                        .distanceKm(distanceKm)
                        .formattedDistance(formattedDistance)
                        .calculationMethod("backend")
                        .locationType(locationType)
                        .build();
                
                distances.add(distanceInfo);
                log.debug("Calculated distance from {} to {}: {} km (location type: {})", 
                    currentUserUuid, targetUuid, distanceKm, locationType);
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
    
    /**
     * 사용자의 위치 허용 상태를 업데이트합니다.
     */
    public void updateLocationPermission(String uuid, Boolean isAllowed) {
        String permissionKey = LOCATION_PERMISSION_KEY_PREFIX + uuid;
        redisTemplate.opsForValue().set(permissionKey, isAllowed, LOCATION_EXPIRY_HOURS, TimeUnit.HOURS);
        
        log.info("Updated location permission for user {}: {}", uuid, isAllowed);
    }
    
    /**
     * 사용자의 위치 허용 상태를 조회합니다.
     */
    public Boolean getLocationPermission(String uuid) {
        String permissionKey = LOCATION_PERMISSION_KEY_PREFIX + uuid;
        Boolean isAllowed = (Boolean) redisTemplate.opsForValue().get(permissionKey);
        
        return isAllowed != null ? isAllowed : false; // 기본값은 false
    }
} 