package com.mall.system.controller;

import com.mall.common.R;
import com.mall.common.security.RequirePermission;
import com.mall.system.service.AdminStatsService;
import com.mall.system.vo.AdminStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "平台端 - 统计")
@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatsController {

    private final AdminStatsService adminStatsService;

    @Operation(summary = "平台统计概览", description = "会员/店铺/订单/销售额与各待办数量，供仪表盘使用")
    @GetMapping("/overview")
    @RequirePermission("admin:stats:view")
    public R<AdminStatsVO> overview() {
        return R.ok(adminStatsService.overview());
    }
}
