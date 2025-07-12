package com.ctpop.websocket.controller;

import com.ctpop.dto.request.LocationUpdateRequest;
import com.ctpop.service.ProfileService;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocketController {
    private final SocketIOServer socketIOServer;
    private final ProfileService profileService;
    private final Map<String, SocketIOClient> userSessions = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        socketIOServer.start();
        log.info("Socket.IO server started");

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
                broadcastUserStatus(uuid, false);
                log.info("User disconnected: {}", uuid);
            }
        });

        socketIOServer.addEventListener("getOnlineUsers", String.class, (client, data, ackSender) -> {
            String uuid = client.getHandshakeData().getSingleUrlParam("uuid");
            if (uuid != null) {
                log.debug("Online users request from: {}", uuid);
                List<String> onlineUsers = List.copyOf(userSessions.keySet());
                client.sendEvent("onlineUsersList", onlineUsers);
                
                // 온라인 사용자들의 거리 정보도 함께 전송
                if (!onlineUsers.isEmpty()) {
                    profileService.broadcastNearbyDistances(uuid, onlineUsers);
                    client.sendEvent("nearbyDistances", profileService.calculateDistances(uuid, onlineUsers));
                }
            }
        });

        // 위치 업데이트 이벤트
        socketIOServer.addEventListener("updateLocation", LocationUpdateRequest.class, (client, locationData, ackSender) -> {
            String uuid = client.getHandshakeData().getSingleUrlParam("uuid");
            if (uuid != null) {
                profileService.updateUserLocation(uuid, locationData);
                log.info("Location updated for user {}: ({}, {})", uuid, locationData.getLatitude(), locationData.getLongitude());
                
                // 위치 업데이트 후 온라인 사용자들과의 거리 재계산
                List<String> onlineUsers = List.copyOf(userSessions.keySet());
                if (!onlineUsers.isEmpty()) {
                    client.sendEvent("nearbyDistances", profileService.calculateDistances(uuid, onlineUsers));
                }
            }
        });

        // 거리 계산 요청 이벤트
        socketIOServer.addEventListener("calculateDistances", String.class, (client, data, ackSender) -> {
            String uuid = client.getHandshakeData().getSingleUrlParam("uuid");
            if (uuid != null) {
                List<String> onlineUsers = List.copyOf(userSessions.keySet());
                if (!onlineUsers.isEmpty()) {
                    client.sendEvent("nearbyDistances", profileService.calculateDistances(uuid, onlineUsers));
                    log.debug("Distance calculation requested for user {} with {} online users", uuid, onlineUsers.size());
                }
            }
        });

        // requestNearbyDistances 이벤트 핸들러 추가
        socketIOServer.addEventListener("requestNearbyDistances", String.class, (client, data, ackSender) -> {
            String uuid = client.getHandshakeData().getSingleUrlParam("uuid");
            if (uuid != null) {
                List<String> onlineUsers = List.copyOf(userSessions.keySet());
                if (!onlineUsers.isEmpty()) {
                    client.sendEvent("nearbyDistances", profileService.calculateDistances(uuid, onlineUsers));
                    log.info("Nearby distances requested for user {} with {} online users", uuid, onlineUsers.size());
                } else {
                    log.debug("No online users found for distance calculation request from user: {}", uuid);
                }
            }
        });

        // 앱 종료 시 마지막 위치 전송 이벤트
        socketIOServer.addEventListener("updateLastLocation", LocationUpdateRequest.class, (client, locationData, ackSender) -> {
            String uuid = client.getHandshakeData().getSingleUrlParam("uuid");
            if (uuid != null) {
                profileService.updateUserLastLocation(uuid, locationData);
                log.info("Last location updated for user {}: ({}, {})", uuid, locationData.getLatitude(), locationData.getLongitude());
                
                // 다른 온라인 사용자들에게 마지막 위치 알림
                List<String> onlineUsers = List.copyOf(userSessions.keySet());
                if (!onlineUsers.isEmpty()) {
                    socketIOServer.getBroadcastOperations().sendEvent("userLastLocation", Map.of(
                        "uuid", uuid,
                        "latitude", locationData.getLatitude(),
                        "longitude", locationData.getLongitude(),
                        "timestamp", System.currentTimeMillis()
                    ));
                }
            }
        });
    }

    private void broadcastUserStatus(String uuid, boolean isOnline) {
        socketIOServer.getBroadcastOperations().sendEvent("userStatus", Map.of(
            "uuid", uuid,
            "isOnline", isOnline
        ));
    }
} 