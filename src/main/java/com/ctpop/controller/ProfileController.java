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
} 