package com.sharepay.web;

import com.sharepay.dto.AnalyticsDtos.DashboardResponse;
import com.sharepay.dto.AnalyticsDtos.SummaryResponse;
import com.sharepay.dto.MemberDtos.MemberSummaryResponse;
import com.sharepay.dto.TransactionDtos.CategoryRef;
import com.sharepay.security.AppUserPrincipal;
import com.sharepay.service.AnalyticsService;
import com.sharepay.service.CategoryService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/events/{eventId}")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final CategoryService categoryService;

    public AnalyticsController(AnalyticsService analyticsService, CategoryService categoryService) {
        this.analyticsService = analyticsService;
        this.categoryService = categoryService;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@AuthenticationPrincipal AppUserPrincipal principal,
                                       @PathVariable Long eventId) {
        return analyticsService.dashboard(principal.getId(), eventId);
    }

    @GetMapping("/summary")
    public SummaryResponse summary(@AuthenticationPrincipal AppUserPrincipal principal,
                                   @PathVariable Long eventId) {
        return analyticsService.summary(principal.getId(), eventId);
    }

    @GetMapping("/members-summary")
    public List<MemberSummaryResponse> memberSummaries(@AuthenticationPrincipal AppUserPrincipal principal,
                                                       @PathVariable Long eventId) {
        return analyticsService.memberSummaries(principal.getId(), eventId);
    }

    @GetMapping("/categories")
    public List<CategoryRef> categories(@AuthenticationPrincipal AppUserPrincipal principal,
                                        @PathVariable Long eventId) {
        return categoryService.listForEvent(principal.getId(), eventId);
    }
}
