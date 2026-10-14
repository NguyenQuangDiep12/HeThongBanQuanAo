package com.project_shopping.shopee.controller;

import com.project_shopping.shopee.dto.ApiDtos.AdminStatisticsResponse;
import com.project_shopping.shopee.service.AdminStatisticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/statistics")
public class AdminStatisticsController {

    private final AdminStatisticsService statistics;

    public AdminStatisticsController(AdminStatisticsService statistics) {
        this.statistics = statistics;
    }

    @GetMapping
    public AdminStatisticsResponse dashboard() {
        return statistics.dashboard();
    }
}
