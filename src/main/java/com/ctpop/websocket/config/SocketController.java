package com.ctpop.websocket.controller;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocketController {

    private final SocketIOServer socketIOServer;
    private final Map<String, SocketIOClient> userSessions = new ConcurrentHashMap<>();
    private final Map<String, Location> userLocations = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        socketIOServer.addConnectListener(client -> {
            String uuid = client.getHandshakeData().getSingleUrlParam("uuid");
            if (uuid != null) {
                userSessions.put(uuid, client);
                broadcastUserStatus(uuid, true);
                log.info("User connected: {}", uuid);
            }
        });

        socketIOServer.addDisconnectListener(client -> {
            String uuid = client.getHandshakeData().getSingleUrlParam("uuid");
            if (uuid != null) {
                userSessions.remove(uuid);
                userLocations.remove(uuid);
                broadcastUserStatus(uuid, false);
                log.info("User disconnected: {}", uuid);
            }
        });

        socketIOServer.addEventListener("heartbeat", String.class, (client, data, ackSender) -> {
            String uuid = client.getHandshakeData().getSingleUrlParam("uuid");
            if (uuid != null) {
                log.debug("Heartbeat from user: {}", uuid);
            }
        });

        // 위치 업데이트 이벤트 추가
        socketIOServer.addEventListener("updateLocation", Location.class, (client, location, ackSender) -> {
            String uuid = client.getHandshakeData().getSingleUrlParam("uuid");
            if (uuid != null) {
                location.setTimestamp(Instant.now().toEpochMilli());
                userLocations.put(uuid, location);
                log.info("Location updated: {} -> ({}, {})", uuid, location.getLatitude(), location.getLongitude());

                // 위치 갱신시 실시간 거리 계산해서 전송
                broadcastNearbyDistances(uuid);
            }
        });
    }

    // 실시간 거리 계산 및 브로드캐스트
    private void broadcastNearbyDistances(String uuid) {
        Location myLocation = userLocations.get(uuid);
        if (myLocation == null) return;

        Map<String, DistanceInfo> distances = new ConcurrentHashMap<>();
        for (Map.Entry<String, Location> entry : userLocations.entrySet()) {
            String otherUuid = entry.getKey();
            if (!otherUuid.equals(uuid)) {
                Location otherLocation = entry.getValue();
                double distanceInMeters = calculateDistance(myLocation, otherLocation);
                distances.put(otherUuid, new DistanceInfo(distanceInMeters));
            }
        }

        SocketIOClient client = userSessions.get(uuid);
        if (client != null) {
            client.sendEvent("nearbyDistances", distances);
            log.debug("Sent distances to user {}: {}", uuid, distances);
        }
    }

    // Haversine 거리 계산 (미터 단위)
    private double calculateDistance(Location loc1, Location loc2) {
        final int R = 6371000; // 지구 반지름 (미터)
        double latDistance = Math.toRadians(loc2.getLatitude() - loc1.getLatitude());
        double lonDistance = Math.toRadians(loc2.getLongitude() - loc1.getLongitude());
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(loc1.getLatitude())) * Math.cos(Math.toRadians(loc2.getLatitude()))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private void broadcastUserStatus(String uuid, boolean isOnline) {
        socketIOServer.getBroadcastOperations().sendEvent("userStatus", Map.of(
                "uuid", uuid,
                "isOnline", isOnline
        ));
    }

    // 내부 위치 모델 클래스
    public static class Location {
        private double latitude;
        private double longitude;
        private long timestamp;

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

    // 거리 정보 클래스
    public static class DistanceInfo {
        private final double distance;
        private final String formattedDistance;

        public DistanceInfo(double distanceInMeters) {
            this.distance = distanceInMeters;
            this.formattedDistance = formatDistance(distanceInMeters);
        }

        private String formatDistance(double meters) {
            if (meters < 1000) {
                return String.format("%dm", Math.round(meters));
            }
            return String.format("%.1fkm", meters / 1000);
        }

        public double getDistance() {
            return distance;
        }

        public String getFormattedDistance() {
            return formattedDistance;
        }
    }
}