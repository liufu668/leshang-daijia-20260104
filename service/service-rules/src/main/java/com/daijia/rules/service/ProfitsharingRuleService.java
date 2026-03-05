package com.daijia.rules.service;


import com.daijia.model.form.rules.ProfitsharingRuleRequestForm;
import com.daijia.model.vo.rules.ProfitsharingRuleResponseVo;

public interface ProfitsharingRuleService {

    ProfitsharingRuleResponseVo calculateOrderProfitsharingFee(ProfitsharingRuleRequestForm profitsharingRuleRequestForm);
}
