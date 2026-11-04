package com.cloudbackup.controller;

import com.cloudbackup.dto.analytics.ActivityPointDto;
import com.cloudbackup.dto.analytics.DashboardStatsResponse;
import com.cloudbackup.dto.common.ApiResponse;
import com.cloudbackup.security.UserPrincipal;
import com.cloudbackup.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getDashboardStats(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        DashboardStatsResponse stats = analyticsService.getDashboardStats(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.success("Dashboard statistics retrieved", stats));
    }

    @GetMapping("/activity")
    public ResponseEntity<ApiResponse<List<ActivityPointDto>>> getActivityTrends(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(value = "days", defaultValue = "7") int days) {

        List<ActivityPointDto> trends = analyticsService.getActivityTrends(currentUser.getId(), days);
        return ResponseEntity.ok(ApiResponse.success("Activity trends retrieved", trends));
    }
}

