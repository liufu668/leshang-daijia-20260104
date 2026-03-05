package com.daijia.rules.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.daijia.model.entity.rule.ProfitsharingRule;
import com.daijia.model.form.rules.ProfitsharingRuleRequest;
import com.daijia.model.form.rules.ProfitsharingRuleRequestForm;
import com.daijia.model.vo.rules.ProfitsharingRuleResponse;
import com.daijia.model.vo.rules.ProfitsharingRuleResponseVo;
import com.daijia.rules.mapper.ProfitsharingRuleMapper;
import com.daijia.rules.service.ProfitsharingRuleService;
import com.daijia.rules.utils.DroolsHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 分账规则:
 *
 * 支付微信平台费用
 *     平台费率：0.6%
 *
 * 订单金额小于等于100
 *     当天完成订单小于等于10单 平台抽成 20%
 *     当天完成订单大于10单 平台抽成 18%
 *
 * 订单金额大于100
 *     当天完成订单小于等于10单 平台抽成 18%
 *     当天完成订单大于10单 平台抽成 16%
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfitsharingRuleServiceImpl implements ProfitsharingRuleService {

    private final ProfitsharingRuleMapper rewardRuleMapper;

    //private static final String RULES_CUSTOMER_RULES_DRL = "rules/ProfitsharingRule.drl";

    @Override
    public ProfitsharingRuleResponseVo calculateOrderProfitsharingFee(ProfitsharingRuleRequestForm profitsharingRuleRequestForm) {
        //封装传入对象
        ProfitsharingRuleRequest profitsharingRuleRequest = new ProfitsharingRuleRequest();
        profitsharingRuleRequest.setOrderAmount(profitsharingRuleRequestForm.getOrderAmount());
        profitsharingRuleRequest.setOrderNum(profitsharingRuleRequestForm.getOrderNum());
        log.info("传入参数：{}", JSON.toJSONString(profitsharingRuleRequest));

        //获取最新订单费用规则
        ProfitsharingRule profitsharingRule = rewardRuleMapper.selectOne(new LambdaQueryWrapper<ProfitsharingRule>().orderByDesc(ProfitsharingRule::getId).last("limit 1"));
        KieSession kieSession = DroolsHelper.loadForRule(profitsharingRule.getRule());

        //封装返回对象
        ProfitsharingRuleResponse profitsharingRuleResponse = new ProfitsharingRuleResponse();
        kieSession.setGlobal("profitsharingRuleResponse", profitsharingRuleResponse);
        // 设置订单对象
        kieSession.insert(profitsharingRuleRequest);
        // 触发规则
        kieSession.fireAllRules();
        // 中止会话
        kieSession.dispose();
        log.info("计算结果：{}", JSON.toJSONString(profitsharingRuleResponse));

        //封装返回对象
        ProfitsharingRuleResponseVo profitsharingRuleResponseVo = new ProfitsharingRuleResponseVo();
        profitsharingRuleResponseVo.setProfitsharingRuleId(profitsharingRule.getId());
        BeanUtils.copyProperties(profitsharingRuleResponse, profitsharingRuleResponseVo);
        return profitsharingRuleResponseVo;
    }
}
