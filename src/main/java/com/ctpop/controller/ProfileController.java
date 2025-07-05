package com.ctpop.controller;

import com.ctpop.dto.request.LocationUpdateRequest;
import com.ctpop.dto.response.DistanceInfo;
import com.ctpop.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {
    
    private final ProfileService profileService;
    
    /**
     * 사용자 위치 정보를 업데이트합니다.
     */
    @PostMapping("/{uuid}/location")
    public ResponseEntity<Map<String, String>> updateLocation(
            @PathVariable String uuid,
            @RequestBody LocationUpdateRequest request) {
        
        profileService.updateUserLocation(uuid, request);
        
        return ResponseEntity.ok(Map.of(
            "message", "Location updated successfully",
            "uuid", uuid
        ));
    }
    
    /**
     * 사용자 위치 정보를 조회합니다.
     */
    @GetMapping("/{uuid}/location")
    public ResponseEntity<Map<String, Object>> getLocation(@PathVariable String uuid) {
        return profileService.getUserLocation(uuid)
                .map(location -> ResponseEntity.ok(location))
                .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 현재 사용자와 다른 사용자들 간의 거리를 계산합니다.
     */
    @PostMapping("/{uuid}/distances")
    public ResponseEntity<List<DistanceInfo>> calculateDistances(
            @PathVariable String uuid,
            @RequestBody List<String> targetUuids) {
        
        List<DistanceInfo> distances = profileService.calculateDistances(uuid, targetUuids);
        return ResponseEntity.ok(distances);
    }

    /**
     * 테스트용 위치 설정 API
     */
    @PostMapping("/test/setup-locations")
    public ResponseEntity<Map<String, String>> setupTestLocations() {
        // 서울의 주요 지역들
        double[][] locations = {
            {37.5665, 126.9780}, // 서울시청
            {37.5519, 126.9882}, // 명동
            {37.5796, 126.9770}, // 경복궁
            {37.5139, 127.0606}, // 강남역
            {37.4968, 127.0278}, // 잠실
            {37.5447, 126.9513}, // 홍대
            {37.5642, 126.9779}, // 광화문
            {37.5668, 126.9787}  // 종로
        };
        
        String[] testUsers = {"user1", "user2", "user3", "user4", "user5", "user6", "user7", "user8"};
        
        for (int i = 0; i < Math.min(testUsers.length, locations.length); i++) {
            LocationUpdateRequest request = new LocationUpdateRequest();
            request.setLatitude(locations[i][0]);
            request.setLongitude(locations[i][1]);
            profileService.updateUserLocation(testUsers[i], request);
        }
        
        return ResponseEntity.ok(Map.of(
            "message", "Test locations set up successfully",
            "users", String.join(", ", testUsers)
        ));
    }
} 