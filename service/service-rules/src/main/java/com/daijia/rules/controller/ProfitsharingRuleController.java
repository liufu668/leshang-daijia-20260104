package com.daijia.rules.controller;

import com.daijia.common.result.Result;
import com.daijia.model.form.rules.ProfitsharingRuleRequestForm;
import com.daijia.model.vo.rules.ProfitsharingRuleResponseVo;
import com.daijia.rules.service.ProfitsharingRuleService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/rules/profitsharing")
@RequiredArgsConstructor
public class ProfitsharingRuleController {

    private final ProfitsharingRuleService profitsharingRuleService;

    @Operation(summary = "计算系统分账费用")
    @PostMapping("/calculateOrderProfitsharingFee")
    public Result<ProfitsharingRuleResponseVo> calculateOrderProfitsharingFee(@RequestBody ProfitsharingRuleRequestForm profitsharingRuleRequestForm) {
        return Result.ok(profitsharingRuleService.calculateOrderProfitsharingFee(profitsharingRuleRequestForm));
    }
}

