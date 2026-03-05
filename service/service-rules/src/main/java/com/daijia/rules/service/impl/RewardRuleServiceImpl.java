package com.daijia.rules.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.daijia.model.entity.rule.RewardRule;
import com.daijia.model.form.rules.RewardRuleRequest;
import com.daijia.model.form.rules.RewardRuleRequestForm;
import com.daijia.model.vo.rules.RewardRuleResponse;
import com.daijia.model.vo.rules.RewardRuleResponseVo;
import com.daijia.rules.mapper.RewardRuleMapper;
import com.daijia.rules.service.RewardRuleService;
import com.daijia.rules.utils.DroolsHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieSession;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RewardRuleServiceImpl implements RewardRuleService {

    //private static final String RULES_CUSTOMER_RULES_DRL = "rules/RewardRule.drl";
    private final RewardRuleMapper rewardRuleMapper;

    /**
     * 系统奖励
     *     每天完成5单后 每单奖励2元
     *     每天完成10单后 每单奖励5元
     *     每天完成20单后 每单奖励10元
     * @param rewardRuleRequestForm
     * @return
     */
    @Override
    public RewardRuleResponseVo calculateOrderRewardFee(RewardRuleRequestForm rewardRuleRequestForm) {
        //封装传入对象
        RewardRuleRequest rewardRuleRequest = new RewardRuleRequest();
        rewardRuleRequest.setOrderNum(rewardRuleRequestForm.getOrderNum());
        log.info("传入参数：{}", JSON.toJSONString(rewardRuleRequest));

        //获取最新订单费用规则
        RewardRule rewardRule = rewardRuleMapper.selectOne(new LambdaQueryWrapper<RewardRule>().orderByDesc(RewardRule::getId).last("limit 1"));
        KieSession kieSession = DroolsHelper.loadForRule(rewardRule.getRule());

        //封装返回对象
        RewardRuleResponse rewardRuleResponse = new RewardRuleResponse();
        kieSession.setGlobal("rewardRuleResponse", rewardRuleResponse);
        // 设置订单对象
        kieSession.insert(rewardRuleRequest);
        // 触发规则
        kieSession.fireAllRules();
        // 中止会话
        kieSession.dispose();
        log.info("计算结果：{}", JSON.toJSONString(rewardRuleResponse));

        //封装返回对象
        RewardRuleResponseVo rewardRuleResponseVo = new RewardRuleResponseVo();
        rewardRuleResponseVo.setRewardRuleId(rewardRule.getId());
        rewardRuleResponseVo.setRewardAmount(rewardRuleResponse.getRewardAmount());
        return rewardRuleResponseVo;
    }
}
