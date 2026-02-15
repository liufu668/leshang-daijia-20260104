package com.daijia.rules.controller;


import com.daijia.common.result.Result;
import com.daijia.model.form.rules.FeeRuleRequestForm;
import com.daijia.model.vo.rules.FeeRuleResponseVo;
import com.daijia.rules.service.FeeRuleService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rules/fee")
@RequiredArgsConstructor
public class FeeRuleController {

    private final FeeRuleService feeRuleService;

    @Operation(summary = "计算订单费用")
    @PostMapping("/calculateOrderFee")
    public Result<FeeRuleResponseVo> calculateOrderFee(@RequestBody FeeRuleRequestForm calculateOrderFeeForm) {
        FeeRuleResponseVo feeRuleResponseVo = feeRuleService.calculateOrderFee(calculateOrderFeeForm);
        return Result.ok(feeRuleResponseVo);
    }
}
