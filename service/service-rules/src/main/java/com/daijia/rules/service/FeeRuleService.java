package com.daijia.rules.service;


import com.daijia.model.form.rules.FeeRuleRequestForm;
import com.daijia.model.vo.rules.FeeRuleResponseVo;

public interface FeeRuleService {

    //计算订单费用
    FeeRuleResponseVo calculateOrderFee(FeeRuleRequestForm calculateOrderFeeForm);
}
