package com.daijia.customer.service;

import com.daijia.model.vo.customer.CustomerLoginVo;
import com.daijia.model.vo.customer.UpdateWxPhoneVo;

public interface CustomerService {

    // 微信登录
    String login(String code);

    // 获取用户信息
    CustomerLoginVo getCustomerLoginInfo(String token);

    CustomerLoginVo getCustomerInfo(Long customerId);

    Boolean updateWxPhoneNumber(UpdateWxPhoneVo updateWxPhoneVo);
}
