package com.daijia.rules.service;


import com.daijia.model.form.rules.RewardRuleRequestForm;
import com.daijia.model.vo.rules.RewardRuleResponseVo;

public interface RewardRuleService {

    RewardRuleResponseVo calculateOrderRewardFee(RewardRuleRequestForm rewardRuleRequestForm);
}
