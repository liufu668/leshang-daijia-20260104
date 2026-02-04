package com.daijia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.daijia.model.entity.customer.CustomerInfo;
import com.daijia.model.vo.customer.CustomerLoginVo;
import com.daijia.model.vo.customer.UpdateWxPhoneVo;

public interface CustomerInfoService extends IService<CustomerInfo> {

    // 微信小程序登录接口
    String login(String code);

    // 获取客户登录信息
    CustomerLoginVo getCustomerInfo(String wxOpenId);

    // 更新客户微信手机号码
    Boolean updateWxPhoneNumber(UpdateWxPhoneVo updateWxPhoneVo);

}
