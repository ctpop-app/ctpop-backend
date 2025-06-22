package com.ctpop.websocket.controller;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocketController {
    private final SocketIOServer socketIOServer;
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
                client.sendEvent("onlineUsersList", userSessions.keySet());
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