package com.ctpop.websocket.config;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket을 통한 실시간 위치 기반 서비스를 제공하는 컨트롤러
 * 
 * 주요 기능:
 * - 사용자 연결/해제 관리
 * - 실시간 위치 업데이트 수신
 * - 사용자 간 거리 계산 및 브로드캐스트
 * - 하트비트 모니터링
 * - 마지막 위치 정보 Redis 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SocketController {

    private final SocketIOServer socketIOServer;
    private final RedisTemplate<String, String> redisTemplate;
    
    // 사용자 세션 관리: UUID -> SocketIOClient 매핑
    private final Map<String, SocketIOClient> userSessions = new ConcurrentHashMap<>();
    
    // 사용자 위치 정보 관리: UUID -> Location 매핑
    private final Map<String, Location> userLocations = new ConcurrentHashMap<>();

    // Redis 키 접두사
    private static final String LOCATION_PREFIX = "location:";
    private static final int LOCATION_EXPIRATION_DAYS = 7; // 위치 정보 7일간 보관

    /**
     * WebSocket 이벤트 리스너들을 초기화합니다.
     * 애플리케이션 시작 시 자동으로 호출됩니다.
     */
    @PostConstruct
    public void init() {
        // 사용자 연결 이벤트 처리
        socketIOServer.addConnectListener(client -> {
            String uuid = client.getHandshakeData().getSingleUrlParam("uuid");
            if (uuid != null) {
                // 사용자 세션 저장
                userSessions.put(uuid, client);
                
                // Redis에서 마지막 위치 정보 복원 (있는 경우)
                restoreLastLocation(uuid);
                
                // 다른 사용자들에게 연결 상태 브로드캐스트
                broadcastUserStatus(uuid, true);
                log.info("Client connected: {}", uuid);
            }
        });

        // 사용자 연결 해제 이벤트 처리
        socketIOServer.addDisconnectListener(client -> {
            String uuid = client.getHandshakeData().getSingleUrlParam("uuid");
            if (uuid != null) {
                // 마지막 위치 정보를 Redis에 저장
                saveLastLocation(uuid);
                
                // 사용자 세션 및 위치 정보 제거
                userSessions.remove(uuid);
                userLocations.remove(uuid);
                
                // 다른 사용자들에게 연결 해제 상태 브로드캐스트
                broadcastUserStatus(uuid, false);
                log.info("Client disconnected: {}", uuid);
            }
        });

        // 하트비트 이벤트 처리 (연결 상태 확인용)
        socketIOServer.addEventListener("heartbeat", String.class, (client, data, ackSender) -> {
            String uuid = client.getHandshakeData().getSingleUrlParam("uuid");
            if (uuid != null) {
                log.debug("Heartbeat from user: {}", uuid);
            }
        });

        // 위치 업데이트 이벤트 처리
        socketIOServer.addEventListener("updateLocation", Location.class, (client, location, ackSender) -> {
            String uuid = client.getHandshakeData().getSingleUrlParam("uuid");
            if (uuid != null) {
                // 현재 시간을 타임스탬프로 설정
                location.setTimestamp(Instant.now().toEpochMilli());
                // 사용자 위치 정보 업데이트
                userLocations.put(uuid, location);
                log.info("Location updated: {} -> ({}, {})", uuid, location.getLatitude(), location.getLongitude());

                // 위치 갱신 시 실시간으로 다른 사용자들과의 거리 계산하여 전송
                broadcastNearbyDistances(uuid);
            }
        });
    }

    /**
     * 사용자의 마지막 위치 정보를 Redis에 저장합니다.
     * 
     * @param uuid 사용자 UUID
     */
    private void saveLastLocation(String uuid) {
        Location location = userLocations.get(uuid);
        if (location != null) {
            try {
                String key = LOCATION_PREFIX + uuid;
                String locationData = String.format("%f,%f,%d", 
                    location.getLatitude(), 
                    location.getLongitude(), 
                    location.getTimestamp());
                
                redisTemplate.opsForValue().set(key, locationData, LOCATION_EXPIRATION_DAYS, TimeUnit.DAYS);
                log.info("Last location saved to Redis for user {}: ({}, {})", 
                    uuid, location.getLatitude(), location.getLongitude());
            } catch (Exception e) {
                log.error("Failed to save last location to Redis for user {}: {}", uuid, e.getMessage());
            }
        }
    }

    /**
     * Redis에서 사용자의 마지막 위치 정보를 복원합니다.
     * 
     * @param uuid 사용자 UUID
     */
    private void restoreLastLocation(String uuid) {
        try {
            String key = LOCATION_PREFIX + uuid;
            String locationData = redisTemplate.opsForValue().get(key);
            
            if (locationData != null) {
                String[] parts = locationData.split(",");
                if (parts.length == 3) {
                    Location location = new Location();
                    location.setLatitude(Double.parseDouble(parts[0]));
                    location.setLongitude(Double.parseDouble(parts[1]));
                    location.setTimestamp(Long.parseLong(parts[2]));
                    
                    userLocations.put(uuid, location);
                    log.info("Last location restored from Redis for user {}: ({}, {})", 
                        uuid, location.getLatitude(), location.getLongitude());
                }
            }
        } catch (Exception e) {
            log.error("Failed to restore last location from Redis for user {}: {}", uuid, e.getMessage());
        }
    }

    /**
     * 특정 사용자와 다른 모든 사용자 간의 거리를 계산하고 전송합니다.
     * 
     * @param uuid 거리 계산을 요청한 사용자의 UUID
     */
    private void broadcastNearbyDistances(String uuid) {
        // 요청한 사용자의 위치 정보 조회
        Location myLocation = userLocations.get(uuid);
        if (myLocation == null) {
            log.warn("No location found for user: {}", uuid);
            return;
        }

        log.info("Starting distance calculation for user: {} at ({}, {})", 
            uuid, myLocation.getLatitude(), myLocation.getLongitude());

        // 다른 사용자들과의 거리 정보를 저장할 맵
        Map<String, DistanceInfo> distances = new ConcurrentHashMap<>();
        
        // 모든 사용자의 위치 정보를 순회하며 거리 계산
        for (Map.Entry<String, Location> entry : userLocations.entrySet()) {
            String otherUuid = entry.getKey();
            // 자기 자신은 제외
            if (!otherUuid.equals(uuid)) {
                Location otherLocation = entry.getValue();
                // Haversine 공식을 사용하여 두 지점 간의 거리 계산 (미터 단위)
                double distanceInMeters = calculateDistance(myLocation, otherLocation);
                DistanceInfo distanceInfo = new DistanceInfo(distanceInMeters);
                distances.put(otherUuid, distanceInfo);
                
                log.info("Distance calculated: {} -> {} = {}m ({})", 
                    uuid, otherUuid, distanceInfo.getDistance(), distanceInfo.getFormattedDistance());
            }
        }

        // 해당 사용자에게 거리 정보 전송
        SocketIOClient client = userSessions.get(uuid);
        if (client != null) {
            client.sendEvent("nearbyDistances", distances);
            log.info("Sent nearbyDistances to: {} - Data: {}", uuid, distances);
        } else {
            log.warn("Client not found for user: {}", uuid);
        }
    }

    /**
     * Haversine 공식을 사용하여 두 지점 간의 거리를 계산합니다.
     * 지구의 곡률을 고려한 정확한 거리 계산이 가능합니다.
     * 
     * @param loc1 첫 번째 위치 (위도, 경도)
     * @param loc2 두 번째 위치 (위도, 경도)
     * @return 두 지점 간의 거리 (미터 단위)
     */
    private double calculateDistance(Location loc1, Location loc2) {
        final int R = 6371000; // 지구 반지름 (미터)
        
        // 위도와 경도 차이를 라디안으로 변환
        double latDistance = Math.toRadians(loc2.getLatitude() - loc1.getLatitude());
        double lonDistance = Math.toRadians(loc2.getLongitude() - loc1.getLongitude());
        
        // Haversine 공식 계산
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(loc1.getLatitude())) * Math.cos(Math.toRadians(loc2.getLatitude()))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        // 최종 거리 계산 (미터 단위)
        return R * c;
    }

    /**
     * 사용자의 연결 상태를 모든 클라이언트에게 브로드캐스트합니다.
     * 
     * @param uuid 사용자 UUID
     * @param isOnline 연결 상태 (true: 연결됨, false: 연결 해제됨)
     */
    private void broadcastUserStatus(String uuid, boolean isOnline) {
        socketIOServer.getBroadcastOperations().sendEvent("userStatus", Map.of(
                "uuid", uuid,
                "isOnline", isOnline
        ));
    }

    /**
     * 사용자의 위치 정보를 담는 내부 클래스
     * 위도, 경도, 타임스탬프 정보를 포함합니다.
     */
    public static class Location {
        private double latitude;    // 위도
        private double longitude;   // 경도
        private long timestamp;     // 위치 업데이트 시간 (밀리초)

        // Getter와 Setter 메서드들
        public double getLatitude() {
            return latitude;
        }
        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }
        public double getLongitude() {
            return longitude;
        }
        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }
        public long getTimestamp() {
            return timestamp;
        }
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }

    /**
     * 거리 정보를 담는 내부 클래스
     * 실제 거리값과 포맷된 문자열을 모두 제공합니다.
     */
    public static class DistanceInfo {
        private final double distance;           // 실제 거리 (미터 단위)
        private final String formattedDistance;  // 포맷된 거리 문자열

        /**
         * 거리 정보 객체를 생성합니다.
         * 
         * @param distanceInMeters 미터 단위의 거리
         */
        public DistanceInfo(double distanceInMeters) {
            this.distance = distanceInMeters;
            this.formattedDistance = formatDistance(distanceInMeters);
        }

        /**
         * 거리를 사용자 친화적인 형태로 포맷팅합니다.
         * 
         * @param meters 미터 단위 거리
         * @return 포맷된 거리 문자열 (예: "123m", "1.2km")
         */
        private String formatDistance(double meters) {
            if (meters < 1000) {
                // 1000m 미만: 미터 단위로 표시 (반올림)
                return String.format("%dm", Math.round(meters));
            }
            // 1000m 이상: 킬로미터 단위로 표시 (소수점 첫째 자리까지)
            return String.format("%.1fkm", meters / 1000);
        }

        public double getDistance() {
            return distance;
        }

        public String getFormattedDistance() {
            return formattedDistance;
        }

        @Override
        public String toString() {
            return String.format("{\"distance\": %.0f, \"formattedDistance\": \"%s\"}", 
                distance, formattedDistance);
        }
    }
}