package com.daijia.rules.client;

import com.daijia.common.result.Result;
import com.daijia.model.form.rules.FeeRuleRequestForm;
import com.daijia.model.vo.rules.FeeRuleResponseVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FeeRuleFeignFallback implements FeeRuleFeignClient {

    @Override
    public Result<FeeRuleResponseVo> calculateOrderFee(FeeRuleRequestForm calculateOrderFeeForm) {
        log.error("【降级】规则服务不可用，calculateOrderFee 调用失败");
        return Result.fail(null);
    }
}
