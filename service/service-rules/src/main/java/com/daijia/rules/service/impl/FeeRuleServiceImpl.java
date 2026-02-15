package com.daijia.rules.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.daijia.model.form.rules.FeeRuleRequest;
import com.daijia.model.form.rules.FeeRuleRequestForm;
import com.daijia.model.entity.rule.FeeRule;
import com.daijia.model.vo.rules.FeeRuleResponse;
import com.daijia.model.vo.rules.FeeRuleResponseVo;
import com.daijia.rules.mapper.FeeRuleMapper;
import com.daijia.rules.service.FeeRuleService;
import com.daijia.rules.utils.DroolsHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.runtime.KieSession;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import org.joda.time.DateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class FeeRuleServiceImpl implements FeeRuleService {

    //private final KieContainer kieContainer;
    private final FeeRuleMapper feeRuleMapper;

    @Override
    public FeeRuleResponseVo calculateOrderFee(FeeRuleRequestForm feeRuleRequestForm) {

        //封装传入对象
        FeeRuleRequest feeRuleRequest = new FeeRuleRequest();
        feeRuleRequest.setDistance(feeRuleRequestForm.getDistance());
        feeRuleRequest.setStartTime(new DateTime(feeRuleRequestForm.getStartTime()).toString("HH:mm:ss"));
        feeRuleRequest.setWaitMinute(feeRuleRequestForm.getWaitMinute());
        log.info("传入参数：{}", JSON.toJSONString(feeRuleRequest));

        //获取最新订单费用规则
        FeeRule feeRule = feeRuleMapper.selectOne(new LambdaQueryWrapper<FeeRule>().orderByDesc(FeeRule::getId).last("limit 1"));
        KieSession kieSession = DroolsHelper.loadForRule(feeRule.getRule());

        //封装返回对象
        FeeRuleResponse feeRuleResponse = new FeeRuleResponse();
        kieSession.setGlobal("feeRuleResponse", feeRuleResponse);
        // 设置订单对象
        kieSession.insert(feeRuleRequest);
        // 触发规则
        kieSession.fireAllRules();
        // 中止会话
        kieSession.dispose();
        log.info("计算结果：{}", JSON.toJSONString(feeRuleResponse));

        //封装返回对象
        FeeRuleResponseVo feeRuleResponseVo = new FeeRuleResponseVo();
        feeRuleResponseVo.setFeeRuleId(feeRule.getId());
        BeanUtils.copyProperties(feeRuleResponse, feeRuleResponseVo);
        return feeRuleResponseVo;
    }
}
