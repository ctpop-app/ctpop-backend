package com.ctpop.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistanceInfo {
    private String targetUuid;
    private Double distanceKm;
    private String formattedDistance;
    private String calculationMethod; // "backend" 또는 "frontend"
    private String locationType; // "current" 또는 "last"
} 